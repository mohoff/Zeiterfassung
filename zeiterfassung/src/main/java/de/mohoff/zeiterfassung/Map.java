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


public class Map extends ActionBarActivity implements LocationChangeListener{
    LocationUpdater lu;

    GoogleMap map;
    LatLng userLocation;
    Marker markerUserLocation;
    Marker markerCandidate = null;
    Circle circle;
    EditText et;
    int radius;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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
                circle.setRadius(radius);
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
    }

    public void drawMarkerForLocation(Location loc){
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .draggable(false)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        );
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
                    userLocation = new LatLng(LocationUpdater.mostRecentLocation.getLatitude(), LocationUpdater.mostRecentLocation.getLongitude());
                } catch(Exception e){
                    System.out.println("fehler beim abrufen der mostRecentLocation");
                }
                markerUserLocation = map.addMarker(new MarkerOptions()
                                .position(userLocation)
                                .draggable(false)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                );
                CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(userLocation, 15);
                map.animateCamera(cu);
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
}
