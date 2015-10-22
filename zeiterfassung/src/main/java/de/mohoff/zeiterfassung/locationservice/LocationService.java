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

import java.util.ArrayList;
import java.util.List;

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
    private final LocalBinder binder = new LocalBinder();

    private GoogleApiClient googleApiClient;
    private LocationRequest locReq;

    private static boolean IS_SERVICE_RUNNING = false; // not used right now
    private static float boundaryTreshold = 0.8f;
    public static int ACTIVE_CACHE_SIZE = 5;
    public static int PASSIVE_CACHE_SIZE = 50;
    public static int REGULAR_UPDATE_INTERVAL = 60 * 1000; // ms, update interval, 60 * 1000, 150 * 1000, 120 * 1000
    public static int FASTEST_UPDATE_INTERVAL = REGULAR_UPDATE_INTERVAL / 2;
    public static float INTERPOLATION_VARIANCE = 1.0f;
    private static String locationProviderType = LocationManager.NETWORK_PROVIDER;  // LocationManager.NETWORK_PROVIDER or LocationManager.GPS_PROVIDER

    private LocationManager locationmanager;
    public static Location mostRecentLocation = null;

    private DatabaseHelper dbHelper = null;
    private boolean inBound = false;
    private int numberOfUpdates = 0;
    private TargetLocationArea nearestTLA = null;
    private List<TargetLocationArea> TLAs = new ArrayList<TargetLocationArea>();
    private List<TargetLocationArea> inBoundTLAs = new ArrayList<TargetLocationArea>();

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationService.mostRecentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locReq, this);

        Toast.makeText(this, "Connected to GoogleApiClient.", Toast.LENGTH_SHORT).show();
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

    // ServiceBinder: so activities and other classes can bind to this service
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
        //throw new UnsupportedOperationException("Not yet implemented");
    }


    public void onCreate() {
        super.onCreate();
        getHelper(this);
        //LocationCache.getInstance().setParameters(ACTIVE_CACHE_SIZE, PASSIVE_CACHE_SIZE, REGULAR_UPDATE_INTERVAL, INTERPOLATION_VARIANCE);
        locationmanager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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

        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locReq = new LocationRequest();
        locReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // make static
        locReq.setInterval(LocationService.REGULAR_UPDATE_INTERVAL); // make static
        locReq.setFastestInterval(LocationService.FASTEST_UPDATE_INTERVAL); // make static // or better same as setInterval() to have consistent locAlgorithm?

        startForeground(1337, notification);

        //Toast.makeText(this, "Service created", Toast.LENGTH_LONG).show();

        // Retrieve stored locations from DB. Parameter is maxAge, a timestamp in milliseconds.
        // All locations younger than maxAge are retrieved.
        //List<Loc> locs = dbHelper.getLocs(System.currentTimeMillis() - REGULAR_UPDATE_INTERVAL * PASSIVE_CACHE_SIZE);
        //CircularFifoQueue<Loc> test = LocationCache.getInstance().getPassiveCache();
    }

    @Override
    public void onDestroy() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        googleApiClient.disconnect(); // does nothing if googleApiClient is already disconnected. So no need to check if already connected or not.
        stopForeground(true);
        IS_SERVICE_RUNNING = false;

        //Toast.makeText(this, "Service terminated", Toast.LENGTH_LONG).show();

        // Store locs in DB in order to retrieve them when service is recreated soon and stored locs
        // aren't too old already.
        dbHelper.dumpLocs(LocationCache.getInstance().getPassiveCache());

        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        googleApiClient.connect();

        updateTLAs();
        IS_SERVICE_RUNNING = true;
        return Service.START_STICKY;
    }

    // not used anymore
    public boolean isNetworkEnabled() {

        return this.locationmanager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean isGPSEnabled() {
        return this.locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void updateTLAs() {
        getHelper(this);
        this.TLAs = dbHelper.getAllTLAs();
    }

    public void updateInBoundTLAs() {
        inBoundTLAs.clear();

        for (TargetLocationArea tla : TLAs) {
            float prox = LocationCache.getInstance().getCurrentInBoundProxFor(tla);
            if (prox > boundaryTreshold) {
                inBoundTLAs.add(tla);
                // break; if only one TLA can match a location
            }
        }
    }

    public void updateTLAsAndTimeslots() {
        updateInBoundTLAs();

        // compare inBoundTLAs with "open" DB entries
        List<Timeslot> unsealedTimeslots = dbHelper.getAllUnsealedTimeslots();
        List<Timeslot> satisfiedTimeslots = new ArrayList<Timeslot>();

        // start timeslots + retrieve timeslots which should get sealed
        for (TargetLocationArea tla : inBoundTLAs) {
            for (Timeslot ts : unsealedTimeslots) {
                //if ((ts.getActivity().equals(tla.getActivityName())) && (ts.getLocation().equals(tla.getLocationName()))) {
                if ((ts.getTLA().getActivityName().equals(tla.getActivityName())) && (ts.getTLA().getLocationName().equals(tla.getLocationName()))) {
                    satisfiedTimeslots.add(ts);
                    break;
                    // match, no update required
                }
            }
            int status = dbHelper.startNewTimeslot(getNormalizedTimestamp(), tla);
            if (status == 1) {
                GeneralHelper.showToast(this, "New Timeslot started");
            } else {
                //GeneralHelper.showToast(this, "timeslot already exists");
            }
        }

        // seal timeslots
        unsealedTimeslots.removeAll(satisfiedTimeslots); // unsealedTimeslots ist jetzt List für unsatisfied Timeslots
        for (Timeslot shouldSeal : unsealedTimeslots) {
            int id = shouldSeal.get_id();
            int status = dbHelper.sealThisTimeslot(id, getNormalizedTimestamp());
            if (status == 1) {
                GeneralHelper.showToast(this, "Timeslot sealed");
            } else {
                GeneralHelper.showToast(this, "Error sealing timeslot in DB");
            }
        }
    }


    public void handleLocationUpdate(Location loc) {
        Log.v(TAG, loc.getLatitude() + ", " + loc.getLongitude() + ", " + loc.getAccuracy());

        Loc currentLoc = convertLocationToLoc(loc);
        // Put loc in activeCache and passiveCache
        LocationCache.getInstance().addLocationUpdate(currentLoc);
        // Order is important: First update cache, then send broadcast because Broadcast-Receivers
        // might access LocationCache.
        sendLocationUpdateViaBroadcast(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy());

        numberOfUpdates++;
        if ((numberOfUpdates % 2 == 0) && LocationCache.getInstance().isActiveCacheFull()) {
            updateTLAsAndTimeslots();
        }
    }

    public static long getNormalizedTimestamp() {
        return System.currentTimeMillis() - (long) (boundaryTreshold * ACTIVE_CACHE_SIZE * REGULAR_UPDATE_INTERVAL);
    }

    public Loc getInterpolatedPosition() {
        return LocationCache.getInstance().getInterpolatedPosition();
    }

    public LatLng getInterpolatedPositionInLatLng() {
        return new LatLng(
                LocationCache.getInstance().getInterpolatedPosition().getLatitude(),
                LocationCache.getInstance().getInterpolatedPosition().getLongitude()
        );
    }

    // broadcast for timeslot events
    private void sendTimeslotEventViaBroadcast(int _id, String message, long timestamp, String activityName, String locationName) {
        Intent intent = new Intent("locationServiceTimeslotEvents");
        intent.putExtra("id", _id);
        intent.putExtra("message", message);
        intent.putExtra("timestamp", String.valueOf(timestamp));
        intent.putExtra("activityName", activityName);
        intent.putExtra("locationName", locationName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // broadcast for locationUpdate events
    private void sendLocationUpdateViaBroadcast(double lat, double lng, double accuracy){
        Intent intent = new Intent("locationServiceLocUpdateEvents");
        // message = "newTimeslotStarted"
        intent.putExtra("lat", String.valueOf(lat));
        intent.putExtra("lng", String.valueOf(lng));
        intent.putExtra("accuracy", String.valueOf(accuracy));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static long getPastTimestampForBoundaryEvents(Loc loc){
        //return loc.getTimestampInMillis() - (int)((ACTIVE_CACHE_SIZE-ACTIVE_CACHE_SIZE*boundaryTreshold) * LocationUpdater.timeBetweenMeasures);
        return loc.getTimestampInMillis() - ACTIVE_CACHE_SIZE * REGULAR_UPDATE_INTERVAL;
    }

    public static Loc convertLocationToLoc(Location loc) {
        Loc result;
        long timeToPersist = System.currentTimeMillis();
        result = new Loc(loc.getLatitude(), loc.getLongitude(), timeToPersist);
        if (loc.getAccuracy() > 0.0) {
            result.setAccuracyMultiplier(LocationCache._getAccuracyMultiplier(loc.getAccuracy()));
        }
        if (loc.hasAltitude()) {
            result.setAltitude((int) loc.getAltitude());
        }
        if (loc.hasSpeed()) {
            result.setSpeed((int) loc.getSpeed());
        }
        return result;
    }

    public static Loc convertLatLngToLoc(LatLng latLng) {
        Loc result = null;
        if ((latLng.latitude != 0) && (latLng.longitude != 0)) {
            long currentTimeInMinutes = (System.currentTimeMillis());
            result = new Loc(latLng.latitude, latLng.longitude, currentTimeInMinutes);
        }
        return result;
    }

    // kann man DatabaseHelper hier weglassen, und DatabaseHelper-Klasse direkt ansprechen? getHelper eigtl nur außerhalb von DatabaseHelper-Klasse zu benutzen
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







