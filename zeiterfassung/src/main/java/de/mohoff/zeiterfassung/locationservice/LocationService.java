package de.mohoff.zeiterfassung.locationservice;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.mohoff.zeiterfassung.datamodel.Stat;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.datamodel.Zone;
import de.mohoff.zeiterfassung.datamodel.Timeslot;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = LocationService.class.getSimpleName();
    // Flag to store the service status
    public static boolean IS_SERVICE_RUNNING = false;
    // Indicates if location update interval exceeded (REGULAR_UPDATE_INTERVAL + 10s) since the last real update
    public static boolean NO_CONNECTION = false;
    // Holds the accumulated distance that the user travelled during this service session, so since he
    // last started this service. While user is inbound a Zone SESSION_DISTANCE remains constant.
    // Is 0 when service is stopped and last session value is already persisted
    public static int SESSION_DISTANCE = 0; // meters
    // Don't update SESSION_DISTANCE when the delta which is to add is smaller than SESSION_DISTANCE_IGNORE_TRESHOLD.
    public static int SESSION_DISTANCE_IGNORE_TRESHOLD = 20;
    // Holds the last start timestamp of this service. When service is stopped the difference of
    // System.currentTimeMillis() and SESSION_STARTTIME will be persistend and combined with the
    // overall service uptime.
    public static long SESSION_STARTTIME = 0; // milliseconds

    // TODO: What to do when service is started? We really need to wait ~ REGULAR_UPDATE_INTERVAL * ACTIVE_CACHE_SIZE until activeCache is full in order to perform first createNewTimeslot? Can we do better?
    // Time interval after which a new location update is retrieved periodically
    // In milliseconds. Used values so far:  60 * 1000, 150 * 1000, 120 * 1000
    public static int REGULAR_UPDATE_INTERVAL = 60 * 1000;
    // Time interval after which a new location update is accepted at the earliest from other applications' requests
    public static int FASTEST_UPDATE_INTERVAL = REGULAR_UPDATE_INTERVAL / 2;
    // Ignore location updates for activeCache which have accuracy > 300m
    public static double ACCURACY_TRESHOLD = 300.0;
    // Determines the ratio of inbound locations in activeCache to trigger an enter-event
    public static int NO_CONNECTION_INTERVAL = REGULAR_UPDATE_INTERVAL + 1000 * 30; // wait extra 30sec
    public static float INBOUND_TRESHOLD = 0.8f;
    public static float OUTBOUND_TRESHOLD = 1 - INBOUND_TRESHOLD;
    // Size of cache that is used to determine inbound and outbound events
    public static int ACTIVE_CACHE_SIZE = 5;
    // Size of cache that is used to display location markers on maps
    public static int PASSIVE_CACHE_SIZE = 50; // 50

    private int numberOfUpdates = 0;

    // General DB, service, googleAPI variables
    private DatabaseHelper dbHelper = null;
    private final LocalBinder binder = new LocalBinder();
    private GoogleApiClient googleApiClient;
    private LocationRequest locReq;

    // Zone information
    private List<Zone> allZones = new ArrayList<Zone>();
    private Zone inboundZone;

    // Timers and Countdowns
    private LocationUpdateTimer timerTask = new LocationUpdateTimer();
    private Timer timer = new Timer();
    private ServiceRunningTime serviceRunningTime = new ServiceRunningTime(REGULAR_UPDATE_INTERVAL * ACTIVE_CACHE_SIZE * 2);

    // Statistics information
    private List<Stat> stats;

    @Override
    public boolean stopService(Intent name) {
        getHelper(this);

        updateNumericStat("serviceUptime", getServiceSessionUptime());
        SESSION_STARTTIME = 0;

        sendServiceEventViaBroadcast("stop");

        resetLocRepeatTimer();

        return super.stopService(name);
    }

    // Returns service session uptime in seconds
    public static int getServiceSessionUptime(){
        if(SESSION_STARTTIME == 0) return 0;
        return (int)((System.currentTimeMillis() - SESSION_STARTTIME)/1000);
    }

    @Override
    public void onConnected(Bundle bundle) {
        //LocationService.mostRecentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locReq, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Loc loc = Loc.convertLocationToLoc(location);
        loc.setIsRealUpdate(true);

        handleLocationUpdate(loc);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        getHelper(this);
        dbHelper._createSampleZones();
        googleApiClient.connect();

        return binder;
    }

    public void onCreate() {
        super.onCreate();
        getHelper(this);

        //locationmanager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locReq = new LocationRequest();
        locReq.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY); // PRIORITY_HIGH_ACCURACY
        locReq.setInterval(LocationService.REGULAR_UPDATE_INTERVAL);
        locReq.setFastestInterval(LocationService.FASTEST_UPDATE_INTERVAL);

        // Setup statusbar notification to indicate running service
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Location Tracking")
                .setContentText("Location Tracking active")
                .setSmallIcon(R.drawable.status_locationservice)
                .setOngoing(true)
                        //.setLargeIcon(R.drawable.status_locationservice)
                .setContentIntent(intent)
                .setPriority(Notification.PRIORITY_MIN)
                .build();

        startForeground(1337, notification);

        stats = dbHelper.getAllStats();
    }

    private void updateNumericStat(String identifier, int deltaToAdd){
        // Update 'travelled distance' in DB.
        for(Stat stat : stats){
            if(stat.getIdentifier().equals(identifier)){
                int oldValue = stat.getIntValue();
                try {
                    dbHelper.updateStat(identifier, oldValue + deltaToAdd);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void onDestroy() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        googleApiClient.disconnect();

        // Stop statusbar notification
        stopForeground(true);

        // Store locs in DB in order to retrieve them when service is recreated soon and stored locs
        // aren't too old already.
        dbHelper.dumpLocs(LocationCache.getInstance().getPassiveCache());

        // Update relevant statistics
        updateNumericStat("distanceTravelled", SESSION_DISTANCE);
        if(SESSION_STARTTIME != 0){
            updateNumericStat("serviceUptime", getServiceSessionUptime());
        }

        IS_SERVICE_RUNNING = false;

        resetLocRepeatTimer();

        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        googleApiClient.connect();
        getHelper(this);
        allZones = dbHelper.getAllZones();

        IS_SERVICE_RUNNING = true;
        sendServiceEventViaBroadcast("start");

        // Start counting service tracking uptime
        serviceRunningTime.start();
        SESSION_STARTTIME = System.currentTimeMillis();

        startLocRepeatTimer();

        return Service.START_STICKY;
    }

    public boolean updateInboundZone() {
        Zone foundInboundZone = null;

        for (Zone zone : allZones) {
            float ratio = LocationCache.getInstance().getCurrentInBoundProxFor2(zone);
            // Only need to check for 'new inbound' and 'still inbound' here to assign foundInboundZone.
            // 'new outbound' and 'still outbound' are handled automatically when foundInboundZone is null.
            if(
                    // New enter event for the Zone we are iterating over: There are at
                    // least (INBOUND_TRESHOLD*activeCache.size()) locations inbound.
                    (ratio >= INBOUND_TRESHOLD) ||
                    // Still inbound of the Zone we are iterating over: There is already an inboundZone
                    // AND its ratio isn't that low too trigger a leave-event AND inboundZone is the
                    // same as the Zone we are just iterating over.
                    (inboundZone != null && ratio > OUTBOUND_TRESHOLD && inboundZone.get_id() == zone.get_id())){

                foundInboundZone = zone;
                // No need to iterate any further since we found an inbound Zone and only one is possible.
                break;
            }
        }

        // Determine if a change happened. To do so we check for all cases in which foundInboundZone
        // and inboundZone are different (only one of them is null OR both have different IDs in the
        // DB.)
        if((foundInboundZone != null && inboundZone == null) ||       // enter event (no inbound --> inbound)
                (foundInboundZone == null && inboundZone != null) ||  // leave event (inbound --> no inbound)
                (foundInboundZone != null && (foundInboundZone.get_id() != inboundZone.get_id()))) {
                                                                    // leave AND enter event (inbound1 --> inbound2)

            inboundZone = foundInboundZone;
            // 'True' indicates the calling function that a change was detected.
            // An enter- or a leave-event happened.
            return true;
        }
        // 'False' indicates the calling function that no change was detected.
        // Neither an enter- nor a leave-event happened.
        return false;
    }

    public void updateTimeslots() {
        Timeslot openTimeslot = dbHelper.getOpenTimeslot();

        // Close openTimeslot
        // TODO: Check if it makes sense to add in if-statement: && isInbound(interpolatedPosition)
        if(openTimeslot != null && inboundZone == null){
            // TODO: Check for serviceRunningTime.isServiceRunningLongterm() and apply 'endtimeIsVague = true' flag.
            if (dbHelper.closeTimeslotById(openTimeslot.get_id(), getEventTimestamp()) == 1) {
                sendTimeslotEventViaBroadcast("closed");
            }
        }

        // Start new Timeslot
        // TODO: Check if it makes sense to add in if-statement: && isOutbound(interpolatedPosition)
        if(openTimeslot == null && inboundZone != null){
            // TODO: Check for serviceRunningTime.isServiceRunningLongterm() and apply 'starttimeIsVague = true' flag.
            if (dbHelper.startNewTimeslot(getEventTimestamp(), inboundZone) == 1) {
                sendTimeslotEventViaBroadcast("opened");
            }
        }
    }

    public void handleLocationUpdate(Loc loc){
        NO_CONNECTION = !loc.isRealUpdate();
        numberOfUpdates++;

        updateTravelDistance(loc);

        // Put loc in activeCache and passiveCache
        LocationCache.getInstance().addLocationUpdate(loc);
        // Order is important: First update cache, then send broadcast because Broadcast-Receivers
        // might access LocationCache.
        sendLocationUpdateViaBroadcast(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy(), loc.isRealUpdate());

        // Update inboundZone at every LocationUpdate unless activeCache doesn't contain
        // 2 entries yet. If it was updated, also update Timeslots.
        if((LocationCache.getInstance().getActiveCache().size() >= 2) &&
                updateInboundZone()){
            updateTimeslots();
        }

        resetLocRepeatTimer();
        startLocRepeatTimer();
    }

    private void resetLocRepeatTimer(){
        if(timerTask != null){
            timerTask.cancel();
        }
        if(timer != null){
            timer.cancel();
            timer.purge();
        }
        timerTask = null;
        timer = null;
    }

    private void startLocRepeatTimer(){
        timerTask = new LocationUpdateTimer();
        timer = new Timer();
        // Execute timerTask after <2nd parameter> and repeat every <3rd parameter>
        //timer.schedule(timerTask, NO_CONNECTION_INTERVAL, REGULAR_UPDATE_INTERVAL);
        timer.schedule(timerTask, NO_CONNECTION_INTERVAL);
    }

    public void updateTravelDistance(Loc loc){
        // TODO: Maybe add condition '&& numberOfUpdates % 2 == 0' in order to reduce update frequency
        if(inboundZone == null && loc != null && loc.getAccuracy() <= 100){
            Loc mostRecentLoc = LocationCache.getInstance().getMostRecentActiveLoc();
            int distanceInMeters = loc.distanceTo(mostRecentLoc);
            // Only add distance to distanceTravelled when it's greater than some value in order to
            // prevent fluctuations which occur due to the inaccurate nature of used location service.
            if(distanceInMeters > SESSION_DISTANCE_IGNORE_TRESHOLD){
                // TODO: Maybe add correction factor of 0.9 or similar.
                SESSION_DISTANCE += loc.distanceTo(mostRecentLoc);
            }
        }
    }

    public static long getEventTimestamp() {
        // Old approach: Substract static duration from current time and return result.
        //return System.currentTimeMillis() - (long) (INBOUND_TRESHOLD * ACTIVE_CACHE_SIZE * REGULAR_UPDATE_INTERVAL);

        // New approach: Location updates also will be received outside of REGULAR_UPDATE_INTERVAL.
        // To take that into account, we lookup the stored Loc at indexOfInterest and return its timestamp.
        // That should lead to a more accurate timestamp result.
        CircularFifoQueue<Loc> activeCache = LocationCache.getInstance().getActiveCache();
        int indexOfInterest = (int)(INBOUND_TRESHOLD * ACTIVE_CACHE_SIZE);
        for(; indexOfInterest >= 0; indexOfInterest--){
            try {
                return activeCache.get(activeCache.size()-indexOfInterest).getTimestampInMillis();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return -1;
    }

    // Broadcast for Timeslot events
    private void sendTimeslotEventViaBroadcast(String eventType) {
        Intent intent = new Intent("locationServiceTimeslotEvents");
        intent.putExtra("type", eventType);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Broadcast for locationUpdate events
    private void sendLocationUpdateViaBroadcast(double lat, double lng, double accuracy, boolean isRealUpdate){
        Intent intent = new Intent("locationServiceLocUpdateEvents");
        intent.putExtra("lat", String.valueOf(lat));
        intent.putExtra("lng", String.valueOf(lng));
        intent.putExtra("accuracy", String.valueOf(accuracy));
        intent.putExtra("isRealUpdate", isRealUpdate);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Broadcast for locationUpdate events
    private void sendServiceEventViaBroadcast(String eventtype){
        Intent intent = new Intent("serviceEventUpdate");
        intent.putExtra("type", eventtype);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private class LocationUpdateTimer extends TimerTask {
        public void run(){
            Loc loc = LocationCache.getInstance().getMostRecentActiveLoc();
            if(loc != null){
                loc.setIsRealUpdate(false);
                handleLocationUpdate(loc);
            }
        }
    }

    private class ServiceRunningTime extends CountDownTimer {
        private boolean isServiceRunningLongterm = false;

        public ServiceRunningTime(long millisInFuture) {
            // 2nd parameter sets interval for onTick().
            super(millisInFuture, millisInFuture);
        }

        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {
            isServiceRunningLongterm = true;
        }

        public boolean isServiceRunningLongterm() {
            return isServiceRunningLongterm;
        }
    }

    private DatabaseHelper getHelper(Context context) {
        if (dbHelper == null) {
            dbHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }
}







