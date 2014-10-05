package de.mohoff.zeiterfassung;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;
import org.apache.commons.collections4.queue.CircularFifoQueue;


import java.util.ArrayList;
import java.util.Iterator;

public class LocationUpdater {
    private ArrayList<LocationChangeListener> locChangeListener = new ArrayList<LocationChangeListener>();

    private static int amountOfTemporarySavedLocations = 10;
    private static int timeBetweenMeasures = 1000 * 60; // in ms

    private static LocationUpdater lm = null;
    private static Context ctx;
    private android.location.LocationManager locationmanager;
    private LocationListener locationListener;
    private static String locationProviderType = android.location.LocationManager.NETWORK_PROVIDER;  // LocationManager.NETWORK_PROVIDER or LocationManager.GPS_PROVIDER

    public static Location mostRecentLocation = null;
    //private RecordEntity currentLocationReference;

    private CircularFifoQueue lastLocations = new CircularFifoQueue<Location>(amountOfTemporarySavedLocations); // fifo based queue
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
                    // this method gets invoked every 20 sec (see attribute timeBetweenMeasures
                    // implement seperate CachedLocationHandler

                    mostRecentLocation = location;

                    delegateDrawMarker(location);



                    /*
                    lastLocations.add(location);
                    counter++;

                    if(counter%5 == 0){
                        Iterator iterator = lastLocations.iterator();


                        while(iterator.hasNext()){
                            Location loc = (Location) iterator.next();
                            //if(getRecordMatching(loc) != null) {
                            //   positives++;
                            //}

                        }
                        percentagePositives = (float) positives / lastLocations.size();






                        if(percentagePositives > 0.9){
                            if(!openTimeslot){
                                long startTime = System.currentTimeMillis()/1000 - timeBetweenMeasures/1000 * (lastLocations.size()-1);
                                //currentLocationReference = getRecordMatching(location);
                                // location hat getTimestamp methode...
                                //db.startNewTimeslot(startTime, currentLocationReference.getUsageName(), currentLocationReference.getLocationName());
                                //float slope = getAverageSlope();
                            }
                            openTimeslot = true;
                            // start GPS to verify user entered location (not possible when GPS isn't enabled by the user manually)
                        } else if (percentagePositives < 0.1){
                            if(openTimeslot){
                                long endTime = System.currentTimeMillis()/1000 - timeBetweenMeasures/1000 * (lastLocations.size()-1);
                                //db.endCurrentTimeslot(endTime);
                                //float slope = getAverageSlope();
                            }
                            openTimeslot = false;
                            // start GPS to verify user entered location (not possible when GPS isn't enabled by the user manually)
                        }


                        positives = 0;
                        counter = 0;

                    }
                    */
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
