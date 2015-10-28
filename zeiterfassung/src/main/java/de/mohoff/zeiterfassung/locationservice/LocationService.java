package de.mohoff.zeiterfassung.locationservice;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;
import de.mohoff.zeiterfassung.datamodel.Timeslot;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = LocationService.class.getSimpleName();

    public static boolean IS_SERVICE_RUNNING = false; // not used right now
    public static float INBOUND_TRESHOLD = 0.8f;
    public static int ACTIVE_CACHE_SIZE = 5;
    public static int PASSIVE_CACHE_SIZE = 50;
    // TODO: What to do when service is started? We really need to wait ~ REGULAR_UPDATE_INTERVAL * ACTIVE_CACHE_SIZE until activeCache is full in order to perform first createNewTimeslot? Can we do better?
    public static int REGULAR_UPDATE_INTERVAL = 60 * 1000; // ms, update interval, 60 * 1000, 150 * 1000, 120 * 1000
    public static int FASTEST_UPDATE_INTERVAL = REGULAR_UPDATE_INTERVAL / 2;
    public static float INTERPOLATION_VARIANCE = 1.0f;
    private static String locationProviderType = LocationManager.NETWORK_PROVIDER;  // LocationManager.NETWORK_PROVIDER or LocationManager.GPS_PROVIDER

    private DatabaseHelper dbHelper = null;

    private final LocalBinder binder = new LocalBinder();
    private GoogleApiClient googleApiClient;
    private LocationRequest locReq;

    private List<TargetLocationArea> allTLAs = new ArrayList<TargetLocationArea>();
    private TargetLocationArea inboundTLA;
    public static Location mostRecentLocation = null;
    private int numberOfUpdates = 0;

    private LocationUpdateTimer locUpdateTimerTask = new LocationUpdateTimer();
    private Timer timer = new Timer();

    @Override
    public boolean stopService(Intent name) {
        sendServiceEventViaBroadcast("stop");
        return super.stopService(name);
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationService.mostRecentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locReq, this);

        //Toast.makeText(this, "Connected to GoogleApiClient.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection suspended.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        LocationService.mostRecentLocation = location;
        handleLocationUpdate(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    // ServiceBinder: Activities and other classes can bind to this service
    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        getHelper(this);
        dbHelper._createSampleTLAs();
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
    }

    @Override
    public void onDestroy() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        googleApiClient.disconnect();

        // Stop statusbar notification
        stopForeground(true);
        IS_SERVICE_RUNNING = false;

        // Store locs in DB in order to retrieve them when service is recreated soon and stored locs
        // aren't too old already.
        dbHelper.dumpLocs(LocationCache.getInstance().getPassiveCache());

        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        googleApiClient.connect();
        getHelper(this);
        allTLAs = dbHelper.getAllTLAs();

        IS_SERVICE_RUNNING = true;
        sendServiceEventViaBroadcast("start");
        return Service.START_STICKY;
    }

    public boolean updateInboundTLA() {
        TargetLocationArea foundInboundTLA = null;

        for (TargetLocationArea tla : allTLAs) {
            float ratio = LocationCache.getInstance().getCurrentInBoundProxFor2(tla);
            // Trigger inbound zone event if ratio of #(inbound locations) to #(outbound locations)
            // is greater than or equal to INBOUND_TRESHOLD.
            if (ratio >= INBOUND_TRESHOLD) {
                foundInboundTLA = tla;
            }
        }

        // Determine if a change happened. To do so we check for all cases in which foundInboundTLA
        // and inboundTLA are different (only one of them is null OR both have different IDs in the
        // DB.)
        if((foundInboundTLA != null && inboundTLA == null) ||       // enter event (no inbound --> inbound)
                (foundInboundTLA == null && inboundTLA != null) ||  // leave event (inbound --> no inbound)
                (foundInboundTLA != null && (foundInboundTLA.get_id() != inboundTLA.get_id()))) {
                                                                    // leave AND enter event (inbound1 --> inbound2)

            inboundTLA = foundInboundTLA;
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
        if(openTimeslot != null && inboundTLA == null){
            if (dbHelper.closeTimeslotById(openTimeslot.get_id(), getEventTimestamp()) == 1) {
                GeneralHelper.showToast(this, "Timeslot sealed");
            } else {
                GeneralHelper.showToast(this, "Error sealing timeslot in DB");
            }
        }

        // Start new Timeslot
        // TODO: Check if it makes sense to add in if-statement: && isOutbound(interpolatedPosition)
        if(openTimeslot == null && inboundTLA != null){
            if (dbHelper.startNewTimeslot(getEventTimestamp(), inboundTLA) == 1) {
                GeneralHelper.showToast(this, "New Timeslot started.");
            } else {
                GeneralHelper.showToast(this, "Timeslot already exists.");
            }
        }


    }


    public void handleLocationUpdate(Location loc) {
        Log.v(TAG, loc.getLatitude() + ", " + loc.getLongitude() + ", " + loc.getAccuracy());

        Loc currentLoc = Loc.convertLocationToLoc(loc);
        // Put loc in activeCache and passiveCache
        LocationCache.getInstance().addLocationUpdate(currentLoc);
        // Order is important: First update cache, then send broadcast because Broadcast-Receivers
        // might access LocationCache.
        sendLocationUpdateViaBroadcast(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy());

        numberOfUpdates++;

        // Old apprach: Update TLAs and Timeslots every 2nd LocationUpdate.
        /*if ((numberOfUpdates % 2 == 0) && LocationCache.getInstance().isActiveCacheFull()) {
            updateTLAsAndTimeslots();
        }*/

        // New approach: Update inboundTLA at every LocationUpdate unless activeCache doesn't contain
        // 2 entries yet. If it was updated, also update Timeslots.
        if((LocationCache.getInstance().getActiveCache().size() >= 2) &&
                updateInboundTLA()){
            updateTimeslots();
        }

        timer.cancel();
        timer.schedule(locUpdateTimerTask, REGULAR_UPDATE_INTERVAL * 3);
    }

    private class LocationUpdateTimer extends TimerTask {
        public void run(){
            // set marker on map that connection interruption because no more
            // location updates are received and timer exceeded.
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
                return activeCache.get(indexOfInterest).getTimestampInMillis();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return -1;
    }

    // Broadcast for Timeslot events
    private void sendTimeslotEventViaBroadcast(int _id, String message, long timestamp, String activityName, String locationName) {
        Intent intent = new Intent("locationServiceTimeslotEvents");
        intent.putExtra("id", _id);
        intent.putExtra("message", message);
        intent.putExtra("timestamp", String.valueOf(timestamp));
        intent.putExtra("activityName", activityName);
        intent.putExtra("locationName", locationName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Broadcast for locationUpdate events
    private void sendLocationUpdateViaBroadcast(double lat, double lng, double accuracy){
        Intent intent = new Intent("locationServiceLocUpdateEvents");
        // message = "newTimeslotStarted"
        intent.putExtra("lat", String.valueOf(lat));
        intent.putExtra("lng", String.valueOf(lng));
        intent.putExtra("accuracy", String.valueOf(accuracy));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Broadcast for locationUpdate events
    private void sendServiceEventViaBroadcast(String eventtype){
        Intent intent = new Intent("serviceEventUpdate");
        intent.putExtra("type", eventtype);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private DatabaseHelper getHelper(Context context) {
        if (dbHelper == null) {
            dbHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }

    private boolean googlePlayServiceConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else {
            Toast toast = Toast.makeText(LocationService.this, resultCode, Toast.LENGTH_LONG);
            toast.show();
            return false;

            // Get the error dialog from Google Play services
            /*Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getSupportFragmentManager(),
                        "Location Updates");
            }*/

        }
    }
}







