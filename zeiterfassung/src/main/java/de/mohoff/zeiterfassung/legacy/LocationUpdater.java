package de.mohoff.zeiterfassung.legacy;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;
import de.mohoff.zeiterfassung.locationservice.LocationChangeListener;


import java.util.ArrayList;

public class LocationUpdater {
    private ArrayList<LocationChangeListener> locChangeListener = new ArrayList<LocationChangeListener>();

    public static int timeBetweenMeasures = 1000 * 60; // in ms

    private static LocationUpdater lm = null;
    private static Context ctx;
    private android.location.LocationManager locationmanager;
    private LocationListener locationListener;
    private static String locationProviderType = android.location.LocationManager.NETWORK_PROVIDER;  // LocationManager.NETWORK_PROVIDER or LocationManager.GPS_PROVIDER

    public static Location mostRecentLocation = null;
    //private RecordEntity currentLocationReference;

    private int counter = 0;
    private int positives = 0;
    private float percentagePositives;
    private boolean openTimeslot = false;


    // Singleton
    public static LocationUpdater getInstance(Context ctx){
        if (lm == null){
            LocationUpdater.ctx = ctx;
            lm = new LocationUpdater();
            //lm.db = DB_Timeslots.getInstance(ctx);
            //lm.tuc = TrackUsagesContainer.getInstance();
        }
        return lm;
    }

    // Listener
    public void addTheListener(LocationChangeListener listener) {
        locChangeListener.add(listener);
    }

    public void delegateDrawMarker(Location loc){
        if(!locChangeListener.isEmpty()) {
            for(LocationChangeListener lcl : locChangeListener){
                lcl.handleLocationUpdate(loc);
            }
        }
    }

    public static Location getLastKnownLocation(Context ctx){
        LocationManager lm = (android.location.LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        return lm.getLastKnownLocation(locationProviderType);
    }


    private LocationUpdater() {
        this.locationmanager = (android.location.LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        if(isNetworkEnabled()) {
            //if(!isGPSEnabled()) {
            //    Toast toast = Toast.makeText(lm.ctx.getApplicationContext(), "GPS ist aktuell deaktiviert. Aktiviere GPS um genauere Ergebnisse zu erhalten.", Toast.LENGTH_LONG);
            //    toast.show();
            //}

            this.locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // this method gets invoked every X sec (see attribute timeBetweenMeasures

                    mostRecentLocation = location;
                    delegateDrawMarker(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}
                public void onProviderEnabled(String provider) {
                    //Toast toast = Toast.makeText(lm.ctx.getApplicationContext(), "Das Location Tracking wird jetzt fortgeführt...", Toast.LENGTH_LONG);
                    //toast.show();
                }
                public void onProviderDisabled(String provider) {
                    //Toast toast = Toast.makeText(lm.ctx.getApplicationContext(), "Bitte aktiviere die Standortbestimmung über Mobilfunk in den Einstellungen", Toast.LENGTH_LONG);
                    //toast.show();
                }
            };
            locationmanager.requestLocationUpdates(locationProviderType, timeBetweenMeasures, 0, locationListener); // every 1000*20 msec
            this.mostRecentLocation = locationmanager.getLastKnownLocation(locationProviderType);

        } else {
            Toast toast = Toast.makeText(lm.ctx.getApplicationContext(), "Die App braucht Zugriff zu einem Ortungsdienst. Aktiviere bitte die Ortung mittlerer Genauigkeit in den Einstellungen", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public boolean isNetworkEnabled(){
        return this.locationmanager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean isGPSEnabled(){
        return this.locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


}
