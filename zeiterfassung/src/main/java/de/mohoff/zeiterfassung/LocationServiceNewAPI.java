package de.mohoff.zeiterfassung;


import android.app.Dialog;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
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

import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;
import de.mohoff.zeiterfassung.datamodel.Timeslot;
import de.mohoff.zeiterfassung.legacy.LocationUpdater;

public class LocationServiceNewAPI extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private final LocalBinder binder = new LocalBinder();

    private GoogleApiClient googleApiClient;
    private Location currentLocation;
    private LocationRequest locReq;


    public static int timeBetweenMeasures = 1000 * 60; // in ms // 1000 * 60;
    private static float boundaryTreshold = 0.8f;
    private static int amountOfTemporarySavedLocations = 5;
    private static String locationProviderType = LocationManager.NETWORK_PROVIDER;  // LocationManager.NETWORK_PROVIDER or LocationManager.GPS_PROVIDER

    private static LocationUpdater lm = null;
    private static Context ctx;
    private LocationManager locationmanager;
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

    @Override
    public void onConnected(Bundle bundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        if (googlePlayServiceConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locReq, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Disconnected. Please reconnect.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        //System.out.println("onLocationChanged");
        //Toast toast = Toast.makeText(LocationServiceNewAPI.this, "onLocationChanged() called", Toast.LENGTH_LONG);
        //toast.show();

        handleLocationUpdate(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    // ServiceBinder: so activities and other classes can bind to this service
    public class LocalBinder extends Binder {
        public LocationServiceNewAPI getService() {
            return LocationServiceNewAPI.this;
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
        locationmanager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Location Tracking")
                .setContentText("Location Tracking active")
                .setSmallIcon(R.drawable.status_locationservice)
                .setOngoing(false)
                        //.setLargeIcon(R.drawable.status_locationservice)
                .build();

        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locReq = new LocationRequest();
        locReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // make static
        locReq.setInterval(60 * 1000); // make static
        locReq.setFastestInterval(30 * 1000); // make static


        startForeground(1337, notification);

    }

    @Override
    public void onDestroy() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        googleApiClient.disconnect();

        stopForeground(true);
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(isNetworkEnabled()){
            googleApiClient.connect();
        }


        this.mostRecentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        updateTLAs();

        return Service.START_STICKY;
    }

    public boolean isNetworkEnabled() {
        return this.locationmanager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean isGPSEnabled() {
        return this.locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void updateTLAs() {
        getHelper();
        this.TLAs = databaseHelper.getTLAs();
    }

    public void updateInBoundTLAs() {
        inBoundTLAs.clear();

        for (TargetLocationArea tla : TLAs) {
            float prox = locCache.getCurrentInBoundProxFor(tla);
            if (prox > boundaryTreshold) {
                inBoundTLAs.add(tla);
            }
        }
    }

    public void showToastWithMsg(String msg) {
        Toast toast = Toast.makeText(LocationServiceNewAPI.this, msg, Toast.LENGTH_LONG);
        toast.show();
    }

    public void updateTLAsAndTimeslots() {
        updateInBoundTLAs();

        // compare inBoundTLAs with "open" DB entries
        List<Timeslot> unsealedTimeslots = databaseHelper.getAllUnsealedTimeslots();
        List<Timeslot> satisfiedTimeslots = new ArrayList<Timeslot>();

        // start timeslots + retrieve timeslots which should get sealed
        for (TargetLocationArea tla : inBoundTLAs) {
            for (Timeslot ts : unsealedTimeslots) {
                if ((ts.getActivity().equals(tla.getActivityName())) && (ts.getLocation().equals(tla.getLocationName()))) {
                    satisfiedTimeslots.add(ts);
                    break;
                    // match, no update required
                }
            }
            int status = databaseHelper.startNewTimeslot(getNormalizedTimestamp(), tla.getActivityName(), tla.getLocationName());
            if (status == 1) {
                showToastWithMsg("new timeslot in DB started");
            } else {
                showToastWithMsg("timeslot already exists");
            }
        }

        // seal timeslots
        unsealedTimeslots.removeAll(satisfiedTimeslots); // unsealedTimeslots ist jetzt List für unsatisfied Timeslots
        for (Timeslot shouldSeal : unsealedTimeslots) {
            int id = shouldSeal.get_id();
            int status = databaseHelper.sealThisTimeslot(id, getNormalizedTimestamp());
            if (status == 1) {
                showToastWithMsg("timeslot successfully sealed in DB");
            } else {
                showToastWithMsg("error sealing timelsot in DB");
            }
        }
    }


    public void handleLocationUpdate(Location loc) {
        Loc currentLoc = convertLocationToLoc(loc);
        locCache.addLocationUpdate(currentLoc);
        boolean cacheIsFull = locCache.isFull();
        numberOfUpdates++;

        if ((numberOfUpdates % 2 == 0) && cacheIsFull) {
            updateTLAsAndTimeslots();
        }
    }

    public static long getNormalizedTimestamp() {
        return System.currentTimeMillis() - (long) (boundaryTreshold * amountOfTemporarySavedLocations * LocationUpdater.timeBetweenMeasures);
    }

    public Loc getInterpolatedPosition() {
        return locCache.getInterpolatedPosition();
    }

    public LatLng getInterpolatedPositionInLatLng() {
        return new LatLng(locCache.getInterpolatedPosition().getLatitude(), locCache.getInterpolatedPosition().getLongitude());
    }

    public static Loc convertLocationToLoc(Location loc) {
        Loc result = null;
        if ((loc.getLatitude() != 0) && (loc.getLongitude() != 0)) {
            result = new Loc(loc.getLatitude(), loc.getLongitude(), (loc.getTime()));
        }
        if (loc.getAccuracy() > 0.0) {
            result.setAccuracyPenalty(LocationCache.getPenaltyFromAccuracy(loc.getAccuracy()));
        }
        if (loc.hasAltitude()) {
            result.setAccuracyPenalty(loc.getAltitude());
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
    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(ctx, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    private boolean googlePlayServiceConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            Toast toast = Toast.makeText(LocationServiceNewAPI.this, resultCode, Toast.LENGTH_LONG);
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







