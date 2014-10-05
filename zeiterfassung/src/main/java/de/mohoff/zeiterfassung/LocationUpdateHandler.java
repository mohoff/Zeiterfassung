package de.mohoff.zeiterfassung;

import android.content.Context;
import android.location.Location;
import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;

import java.util.List;

/**
 * Created by Moritz on 03.10.2014.
 */
public class LocationUpdateHandler extends OpenHelperManager implements LocationChangeListener{

    private static LocationUpdater lu;
    private static LocationUpdateHandler luh;
    private static Context ctx;

    private LocationCache locCache;
    private DatabaseHelper databaseHelper = null;

    private int numberOfUpdates = 0;
    private TargetLocationArea nearestTLA = null;

    // Singleton
    public static LocationUpdateHandler getInstance(Context ctx){
        if (luh == null){
            LocationUpdateHandler.ctx = ctx;
            luh = new LocationUpdateHandler();
        }
        return luh;
    }

    private LocationUpdateHandler(){
        lu = LocationUpdater.getInstance(ctx);
        lu.addTheListener(this); // so the listener-method "handleLocationUpdate" gets invoked frequently

        locCache = new LocationCache(10);
    }

    // kann man DatabaseHelper hier weglassen, und DatabaseHelper-Klasse direkt ansprechen? getHelper eigtl nur außerhalb von DatabaseHelper-Klasse zu benutzen
    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(ctx, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    public static Loc convertLocationToMyLocation(Location loc){
        Loc result = null;
        if((loc.getLatitude() != 0) && (loc.getLongitude() != 0)){
            result = new Loc(loc.getLatitude(), loc.getLongitude(), getMinutesOfLocationTimestamp(loc));
        }
        if(loc.getAccuracy() > 0.0){
            result.setAccuracyPenalty(LocationCache.getPenaltyFromAccuracy(loc.getAccuracy()));
        }
        if(loc.hasAltitude()){
            result.setAccuracyPenalty(loc.getAltitude());
        }
        if(loc.hasSpeed()){
            result.setSpeed((int)loc.getSpeed());
        }

        return result;
    }

    public static Loc convertLatLngToMyLocation(LatLng latLng){
        Loc result = null;
        if((latLng.latitude != 0) && (latLng.longitude != 0)){
            int currentTimeInMinutes = (int)(System.currentTimeMillis()/(1000*60));
            result = new Loc(latLng.latitude, latLng.longitude, currentTimeInMinutes);
        }
        return result;
    }


    public void updateNearestTLA(LatLng currentLoc){
        getHelper();
        Loc loc = convertLatLngToMyLocation(currentLoc);
        int minDistance = 10000;        // 10km distance to remember iterated TLA as closes TLA

        List<TargetLocationArea> tlas = databaseHelper.getTargetLocationAreas();
        for(TargetLocationArea tla : tlas){
            Loc targetLoc = new Loc(tla.getLatitude(), tla.getLongitude());
            int distance = loc.distanceTo(targetLoc); // - tla.getRadius(); ???
            if(distance < minDistance){
                minDistance = distance;
                nearestTLA = tla;
            }
        }
    }



    public void handleLocationUpdate(Location loc){
        // markers get drawn in Map-Activity

        locCache.addLocationUpdate(convertLocationToMyLocation(loc));
        int locationsInCacheInBound = 0;

        if(numberOfUpdates%2 == 0){     // update nearest location every 2 minutes
            updateNearestTLA(locCache.getInterpolatedPosition());
            locationsInCacheInBound = locCache.validateInBoundsForTLA(nearestTLA);
        }


        numberOfUpdates++;
        //locationCache.add(loc);

        // draft 1
        /*if(inBoundEventFired){
            databaseHelper.startNewTimeslot(getMinutesOfLocationTimestamp(loc), "work", "ibm böblingen");
        } else if(outBoundEventFired){
            databaseHelper.sealCurrentTimeslot(getMinutesOfLocationTimestamp(loc));
        }
        */

        // calc/interpolate/...
        // %2 calcInterpolatedPosition
    }

    public static int getMinutesOfLocationTimestamp(Location loc){
        int minutes = (int)(loc.getTime()/(1000*60));
        return minutes;
    }


    public LatLng getInterpolatedPosition() {
        return locCache.getInterpolatedPosition();
    }
}
