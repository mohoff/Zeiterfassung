package de.mohoff.zeiterfassung;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;


public class Map extends ActionBarActivity implements LocationChangeListener{
    LocationUpdater lu;

    GoogleMap map;
    LatLng mostRecentUserLocation = null;
    ArrayList<Location> userLocations = new ArrayList<Location>();
    ArrayList<Marker> markers = new ArrayList<Marker>();
    Marker markerUserLocation;
    Marker markerCandidate = null;
    Circle circle = null;

    private int amountOfTemporarySavedLocations = 10;
    private CircularFifoQueue locationCache = new CircularFifoQueue<Location>(amountOfTemporarySavedLocations); // fifo based queue

    EditText et;
    int radius;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        lu = LocationUpdater.getInstance(this);
        lu.setTheListener(this);

        setUpMapIfNeeded();

        et = (EditText) findViewById(R.id.editText);
        updateRadiusFromEditText();

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                InputMethodManager imm = (InputMethodManager)getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                if(markerCandidate != null){
                    markerCandidate.setPosition(point);
                    circle.setCenter(point);
                    circle.setRadius(radius);
                } else {
                    markerCandidate = map.addMarker(new MarkerOptions()
                            .position(point)
                            .draggable(true)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    );
                    CircleOptions circleOptions = new CircleOptions()
                            .center(point)
                            .radius(radius)
                            .fillColor(Color.argb(100, 81, 112, 226))
                            .strokeWidth(0)
                            .strokeColor(Color.TRANSPARENT)

                    ;
                    circle = map.addCircle(circleOptions);
                }
            }
        });

        et.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                updateRadiusFromEditText();
                if(circle != null) {
                    circle.setRadius(radius);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });

        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
                circle.setVisible(false);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                if(arg0.equals(markerCandidate)){
                    circle.setCenter(arg0.getPosition());
                    circle.setVisible(true);
                }
            }

            @Override
            public void onMarkerDrag(Marker arg0) {

            }
        });

        // store userLocations temporarly in bundle, to repopulate map after screen rotation
        if(savedInstanceState != null){
            if(savedInstanceState.containsKey("userLocations")){
                userLocations = savedInstanceState.getParcelableArrayList("userLocations");
                for(int i=0; i<userLocations.size(); i++){
                    drawMarkerForLocation(userLocations.get(i));
                }
            }
            //if(savedInstanceState.containsKey("mostRecentUserLocation")){
            //    double lat = savedInstanceState.getDoubleArray("mostRecentUserLocation")[0];
            //    double lng = savedInstanceState.getDoubleArray("mostRecentUserLocation")[1];
            //    mostRecentUserLocation = new LatLng(lat, lng);
            //}
        }
    }



    public void handleLocationUpdate(Location loc){
        locationCache.add(loc);
        drawMarkerForLocation(loc);
        if(!markers.isEmpty()){
            markers.get(markers.size()-2).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        }


        getInterpolatedPosition();
    }

    public LatLng getInterpolatedPosition(){
        float[] score = new float[amountOfTemporarySavedLocations];
        for(int i=0; i<locationCache.size(); i++){
            Location currentLoc = (Location) locationCache.get(i);
            float timeWeight = (float) (1-((float)(i+1)/(float)amountOfTemporarySavedLocations)*0.5);
            float accuracyWeight;
            if(currentLoc.getAccuracy() <= 30){
                accuracyWeight = 0;
            } else {
                accuracyWeight = (currentLoc.getAccuracy()-30)/10f;
            }
            //float speedWeight = currentLoc.getSpeed() / 10f;
            score[i] = timeWeight / accuracyWeight;
        }
        for(int i=0; i<locationCache.size(); i++){
            Location currentLoc = (Location)locationCache.get(i);

        }

    }

    public void drawMarkerForLocation(Location loc){
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        markerUserLocation = map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .draggable(false)
                        .title("acc " + String.valueOf(loc.getAccuracy()) + ", speed " + String.valueOf(loc.getSpeed()) + ", alt " + String.valueOf(loc.getAltitude()))
                        .snippet(loc.getExtras().toString())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
        );
        userLocations.add(loc);
        markers.add(markerUserLocation);
    }

    public void updateRadiusFromEditText(){
        String inputString = et.getText().toString();
        try{
            radius = Integer.valueOf(inputString);
        } catch (Exception e){
            radius = -1;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (map != null) {
                // The Map is verified. It is now safe to manipulate the map.
                try {
                    mostRecentUserLocation = new LatLng(LocationUpdater.mostRecentLocation.getLatitude(), LocationUpdater.mostRecentLocation.getLongitude());
                    markers.add(map.addMarker(new MarkerOptions()
                                    .position(mostRecentUserLocation)
                                    .draggable(false)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                    ));
                    CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(mostRecentUserLocation, 15);
                    map.animateCamera(cu);

                } catch(Exception e){
                    System.out.println("fehler beim abrufen der mostRecentLocation");
                }
            }
        }
    }

    public void onMarkerClick(final Marker marker) {
        System.out.println("marker click!");
        /*if (marker.equals(markerIBM))
        {
            //handle click here
            markerIBM.hideInfoWindow();
        }*/
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //if(mostRecentUserLocation != null){
        //    double[] latLng = {mostRecentUserLocation.latitude, mostRecentUserLocation.longitude};
        //    outState.putDoubleArray("mostRecentUserLocation", latLng);
        //}
        if(!userLocations.isEmpty()){
            outState.putParcelableArrayList("userLocations", userLocations);
        }

        super.onSaveInstanceState(outState);
    }
}
