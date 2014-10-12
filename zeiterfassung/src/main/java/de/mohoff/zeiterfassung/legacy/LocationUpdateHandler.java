package de.mohoff.zeiterfassung.legacy;

import android.content.Context;
import android.location.Location;
import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import de.mohoff.zeiterfassung.LocationChangeListener;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Moritz on 03.10.2014.
 */
public class LocationUpdateHandler extends OpenHelperManager implements LocationChangeListener {
    private ArrayList<TimeslotEventListener> timeslotEventListeners = new ArrayList<TimeslotEventListener>();

    private static LocationUpdater lu;
    private static LocationUpdateHandler luh;
    private static Context ctx;

    private LocationCache locCache;
    private DatabaseHelper databaseHelper = null;

    private static float boundaryTreshold = 0.8f;
    private static int amountOfTemporarySavedLocations = 5;
    private boolean inBound = false;

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

        locCache = new LocationCache(amountOfTemporarySavedLocations);
    }

    // Listener
    public void addTheListener(TimeslotEventListener listener) {
        timeslotEventListeners.add(listener);
    }

    public void fireTimeslotStartedEvent(TargetLocationArea tla, long timestampToPersist){
        if(!timeslotEventListeners.isEmpty()) {
            for(TimeslotEventListener tel : timeslotEventListeners){
                tel.timeslotStartedEvent(tla, timestampToPersist);
            }
        }
    }

    public void fireTimeslotFinishedEvent(TargetLocationArea tla, long timestampToPersist){
        if(!timeslotEventListeners.isEmpty()) {
            for(TimeslotEventListener tel : timeslotEventListeners){
                tel.timeslotFinishedEvent(tla, timestampToPersist);
            }
        }
    }

    // kann man DatabaseHelper hier weglassen, und DatabaseHelper-Klasse direkt ansprechen? getHelper eigtl nur außerhalb von DatabaseHelper-Klasse zu benutzen
    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(ctx, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    public static Loc convertLocationToLoc(Location loc){
        Loc result = null;
        if((loc.getLatitude() != 0) && (loc.getLongitude() != 0)){
            result = new Loc(loc.getLatitude(), loc.getLongitude(), (int)(loc.getTime()));
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

    public static Loc convertLatLngToLoc(LatLng latLng){
        Loc result = null;
        if((latLng.latitude != 0) && (latLng.longitude != 0)){
            long currentTimeInMillis = (System.currentTimeMillis());
            result = new Loc(latLng.latitude, latLng.longitude, currentTimeInMillis);
        }
        return result;
    }


    public void updateNearestTLA(Loc currentLoc){
        getHelper();
        Loc loc = currentLoc; //convertLatLngToLoc(currentLoc);
        int minDistance = 10000;        // 10km distance to remember iterated TLA as closes TLA

        List<TargetLocationArea> tlas = databaseHelper.getTLAs();
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
        Loc currentLoc = convertLocationToLoc(loc);
        locCache.addLocationUpdate(currentLoc);
        float locationsInBound = 0;

        if(numberOfUpdates%2 == 0) {     // update nearest location every 2 minutes
            updateNearestTLA(locCache.getInterpolatedPosition());
        }

        locationsInBound = locCache.validateInBoundsForTLA(nearestTLA);
        // check if user entered TLA
        boolean tmp = locCache.isFull();
        if((locationsInBound > boundaryTreshold) && (!inBound) && (locCache.isFull())){
            long timestampToPersist = getPastTimestampForBoundaryEvents(currentLoc);
            int status = databaseHelper.startNewTimeslot(timestampToPersist, nearestTLA.getActivityName(), nearestTLA.getLocationName());
            fireTimeslotStartedEvent(nearestTLA, timestampToPersist);
        }
        // check if user left TLA
        if((locationsInBound < (1-boundaryTreshold)) && (inBound)&& (locCache.isFull())){
            long timestampToPersist = getPastTimestampForBoundaryEvents(currentLoc);
            databaseHelper.sealCurrentTimeslot(timestampToPersist);
            fireTimeslotFinishedEvent(nearestTLA, timestampToPersist);
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



    public static long getPastTimestampForBoundaryEvents(Loc loc){
        //return loc.getTimestampInMillis() - (int)((amountOfTemporarySavedLocations-amountOfTemporarySavedLocations*boundaryTreshold) * LocationUpdater.timeBetweenMeasures);
        return (int)(loc.getTimestampInMillis()/(1000*60)) - amountOfTemporarySavedLocations * LocationUpdater.timeBetweenMeasures;
    }

    public Loc getInterpolatedPosition() {
        return locCache.getInterpolatedPosition();
    }

    public LatLng getInterpolatedPositionInLatLng(){
        return new LatLng(locCache.getInterpolatedPosition().getLatitude(), locCache.getInterpolatedPosition().getLongitude());
    }
}
