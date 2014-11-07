package de.mohoff.zeiterfassung;



import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;
import de.mohoff.zeiterfassung.datamodel.Timeslot;
import de.mohoff.zeiterfassung.legacy.LocationUpdater;

import java.util.*;

public class LocationService extends Service {
    private final LocalBinder binder = new LocalBinder();

    public static int timeBetweenMeasures = 1000 * 60; // in ms // 1000 * 60;
    private static float boundaryTreshold = 0.8f;
    private static int amountOfTemporarySavedLocations = 5;
    private static String locationProviderType = android.location.LocationManager.NETWORK_PROVIDER;  // LocationManager.NETWORK_PROVIDER or LocationManager.GPS_PROVIDER

    private static LocationUpdater lm = null;
    private static Context ctx;
    private android.location.LocationManager locationmanager;
    private LocationListener locationListener;
    public static Location mostRecentLocation = null;
    /////
    private LocationCache locCache;
    private DatabaseHelper databaseHelper = null;
    private boolean inBound = false;
    private int numberOfUpdates = 0;
    private TargetLocationArea nearestTLA = null;
    private List<TargetLocationArea> TLAs = new ArrayList<TargetLocationArea>();
    private List<TargetLocationArea> inBoundTLAs = new ArrayList<TargetLocationArea>();

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    // ServiceBinder: so activities and other classes can bind to this service
    public class LocalBinder extends Binder {
        public LocationService getService(){
            return LocationService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        getHelper();
        databaseHelper._createSampleTLAs();

        return binder;
        //throw new UnsupportedOperationException("Not yet implemented");
    }



    public void onCreate() {
        super.onCreate();
        locCache = new LocationCache(amountOfTemporarySavedLocations);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Location Tracking")
                .setContentText("Location Tracking active")
                .setSmallIcon(R.drawable.status_locationservice)
                .setOngoing(false)
                //.setLargeIcon(R.drawable.status_locationservice)
                .build();

        startForeground(1337, notification);
        
    }
    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationmanager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        this.mostRecentLocation = locationmanager.getLastKnownLocation(locationProviderType);
        updateTLAs();

        if (isNetworkEnabled()){
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    System.out.println("onLocationChanged");
                    // this method gets invoked every 20 sec (see attribute timeBetweenMeasures
                    // implement seperate CachedLocationHandler
                    Toast toast = Toast.makeText(LocationService.this, "onLocationChanged() called", Toast.LENGTH_LONG);
                    //toast.show();



                    handleLocationUpdate(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };
            locationmanager.requestLocationUpdates(locationProviderType, timeBetweenMeasures, 0, locationListener); // every 1000*20 msec
        }
        return Service.START_STICKY;
    }



    // my stuff


    // kann man DatabaseHelper hier weglassen, und DatabaseHelper-Klasse direkt ansprechen? getHelper eigtl nur außerhalb von DatabaseHelper-Klasse zu benutzen
    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(ctx, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    public static Location getLastKnownLocation(Context ctx){
        LocationManager lm = (android.location.LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        return lm.getLastKnownLocation(locationProviderType);
    }


    public boolean isNetworkEnabled(){
        return this.locationmanager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean isGPSEnabled(){
        return this.locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void updateTLAs(){
        getHelper();
        this.TLAs = databaseHelper.getTLAs();
    }

    public void updateInBoundTLAs(){
        inBoundTLAs.clear();

        for(TargetLocationArea tla : TLAs){
            float prox = locCache.getCurrentInBoundProxFor(tla);
            if(prox > boundaryTreshold){
                inBoundTLAs.add(tla);
            }
        }
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

    public void showToastWithMsg(String msg){
        Toast toast = Toast.makeText(LocationService.this, msg, Toast.LENGTH_LONG);
        toast.show();
    }

    public void updateTLAsAndTimeslots(){
        updateInBoundTLAs();

        // compare inBoundTLAs with "open" DB entries
        List<Timeslot> unsealedTimeslots = databaseHelper.getAllUnsealedTimeslots();
        List<Timeslot> satisfiedTimeslots = new ArrayList<Timeslot>();

        // start timeslots + retrieve timeslots which should get sealed
        for(TargetLocationArea tla : inBoundTLAs){
            for(Timeslot ts : unsealedTimeslots){
                if((ts.getActivity().equals(tla.getActivityName())) && (ts.getLocation().equals(tla.getLocationName()))){
                    satisfiedTimeslots.add(ts);
                    break;
                    // match, no update required
                }
            }
            int status = databaseHelper.startNewTimeslot(getNormalizedTimestamp(), tla.getActivityName(), tla.getLocationName());
            if(status == 1){
                showToastWithMsg("new timeslot in DB started");
            } else {
                showToastWithMsg("error starting new timelsot in DB");
            }
        }

        // seal timeslots
        unsealedTimeslots.removeAll(satisfiedTimeslots); // unsealedTimeslots ist jetzt List für unsatisfied Timeslots
        for(Timeslot shouldSeal : unsealedTimeslots){
            int id = shouldSeal.get_id();
            int status = databaseHelper.sealThisTimeslot(id, getNormalizedTimestamp());
            if(status == 1){
                showToastWithMsg("timeslot successfully sealed in DB");
            } else {
                showToastWithMsg("error sealing timelsot in DB");
            }
        }
    }


    public void handleLocationUpdate(Location loc){
        Loc currentLoc = convertLocationToLoc(loc);
        locCache.addLocationUpdate(currentLoc);
        boolean cacheIsFull = locCache.isFull();
        numberOfUpdates++;

        if((numberOfUpdates%1 == 0) && cacheIsFull){
            updateTLAsAndTimeslots();
        }



        // LEGACY
        /*
        float locationsInBound = 0;
        locationsInBound = locCache.validateInBoundsForTLA(nearestTLA);

        // check if user entered TLA
        boolean tmp = locCache.isFull();
        if((locationsInBound > boundaryTreshold) && (!inBound) && (locCache.isFull())){
            long timestampToPersist = getPastTimestampForBoundaryEvents(currentLoc);
            int status = databaseHelper.startNewTimeslot(timestampToPersist, nearestTLA.getActivityName(), nearestTLA.getLocationName());
            // if startNewTimeslot was successfull: inBound = true         (same for leaving location, see below)
            if(status == 1){
                Toast toast = Toast.makeText(LocationService.this, "new timeslot in DB started", Toast.LENGTH_LONG);
                //toast.show();
            } else {
                Toast toast = Toast.makeText(LocationService.this, "error starting new timelsot in DB", Toast.LENGTH_LONG);
                //toast.show();
            }
            inBound = true;
            sendMessageViaBroadcast("newTimeslotStarted", timestampToPersist, nearestTLA.getActivityName(), nearestTLA.getLocationName());
        }
        // check if user left TLA
        if((locationsInBound < (1-boundaryTreshold)) && (inBound) && (locCache.isFull())){
            long timestampToPersist = getPastTimestampForBoundaryEvents(currentLoc);
            databaseHelper.sealCurrentTimeslot(timestampToPersist);
            Toast toast = Toast.makeText(LocationService.this, "timeslot ended", Toast.LENGTH_LONG);
            //toast.show();
            inBound = false;
            sendMessageViaBroadcast("openTimeslotSealed", timestampToPersist, nearestTLA.getActivityName(), nearestTLA.getLocationName());
        }
        */
    }

    // broadcast custom events
    private void sendMessageViaBroadcast(String message, long timestamp, String activityName, String locationName) {
        Intent intent = new Intent("locationServiceEvents");
        // message = "newTimeslotStarted"
        intent.putExtra("message", message);
        intent.putExtra("timestamp", String.valueOf(timestamp));
        intent.putExtra("activityName", activityName);
        intent.putExtra("locationName", locationName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    public static long getPastTimestampForBoundaryEvents(Loc loc){
        //return loc.getTimestampInMillis() - (int)((amountOfTemporarySavedLocations-amountOfTemporarySavedLocations*boundaryTreshold) * LocationUpdater.timeBetweenMeasures);
        return loc.getTimestampInMillis() - amountOfTemporarySavedLocations * LocationUpdater.timeBetweenMeasures;
    }

    public static long getNormalizedTimestamp(){
        return System.currentTimeMillis() - (long)(boundaryTreshold * amountOfTemporarySavedLocations * LocationUpdater.timeBetweenMeasures);
    }

    public Loc getInterpolatedPosition() {
        return locCache.getInterpolatedPosition();
    }

    public LatLng getInterpolatedPositionInLatLng(){
        return new LatLng(locCache.getInterpolatedPosition().getLatitude(), locCache.getInterpolatedPosition().getLongitude());
    }

    public static Loc convertLocationToLoc(Location loc){
        Loc result = null;
        if((loc.getLatitude() != 0) && (loc.getLongitude() != 0)){
            result = new Loc(loc.getLatitude(), loc.getLongitude(), (loc.getTime()));
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
            long currentTimeInMinutes = (System.currentTimeMillis());
            result = new Loc(latLng.latitude, latLng.longitude, currentTimeInMinutes);
        }
        return result;
    }



}








