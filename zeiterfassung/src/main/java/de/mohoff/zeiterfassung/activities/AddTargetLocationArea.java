package de.mohoff.zeiterfassung.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import de.mohoff.zeiterfassung.LocationUpdater;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;

import java.util.ArrayList;


public class AddTargetLocationArea extends ActionBarActivity{
    private DatabaseHelper dbHelper = null;

    Menu menu;

    GoogleMap map;
    Marker markerUserLocation;

    ArrayList<TargetLocationArea> areas = new ArrayList<TargetLocationArea>();
    ArrayList<Marker> markers = new ArrayList<Marker>();
    ArrayList<Circle> circles = new ArrayList<Circle>();

    Marker candidateMarker = null;
    Circle candidateCircle = null;
    int radius;

    EditText et;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tla);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setUpMapIfNeeded();
        drawExistingTargetLocationAreas();

        et = (EditText) findViewById(R.id.editText);
        updateRadiusFromEditText();

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                InputMethodManager imm = (InputMethodManager)getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                showOptionWithId(R.id.action_save);
                showOptionWithId(R.id.action_cancel);

                if(candidateMarker != null){
                    candidateMarker.setPosition(point);
                    candidateCircle.setCenter(point);
                    candidateCircle.setRadius(radius);
                } else {
                    candidateMarker = map.addMarker(new MarkerOptions()
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
                    candidateCircle = map.addCircle(circleOptions);
                }
            }
        });

        et.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                updateRadiusFromEditText();
                if(candidateCircle != null) {
                    candidateCircle.setRadius(radius);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });

        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
                candidateCircle.setVisible(false);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                if(arg0.equals(candidateMarker)){
                    candidateCircle.setCenter(arg0.getPosition());
                    candidateCircle.setVisible(true);
                }
            }

            @Override
            public void onMarkerDrag(Marker arg0) {

            }
        });
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }

    public void drawExistingTargetLocationAreas(){
        getDbHelper();

        for(TargetLocationArea tla : dbHelper.getTargetLocationAreas()){
            LatLng latLng = new LatLng(tla.getLatitude(), tla.getLongitude());
            markers.add(map.addMarker(new MarkerOptions()
                            .position(latLng)
                            .draggable(false)
                            .title(tla.getLocationName() + " @" + tla.getActivityName())
                            .snippet("latitude: " + tla.getLatitude() +
                                    "\nlongitude: " + tla.getLongitude() +
                                    "\nradius: " + tla.getRadius() +
                                    "\ncolorScheme: " + tla.getColorScheme() +
                                    "\n_id: " + tla.get_id()
                            )
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            ));
            CircleOptions circleOptions = new CircleOptions()
                    .center(latLng)
                    .radius(tla.getRadius())
                    .fillColor(Color.HSVToColor(100, new float[]{BitmapDescriptorFactory.HUE_GREEN, 1, 1}))
                    //.fillColor(Color.argb(100, 81, 112, 226))
                    .strokeWidth(0)
                    .strokeColor(Color.TRANSPARENT)
                    ;
            circles.add(map.addCircle(circleOptions));
        }
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

    private void showOptionWithId(int id){
        MenuItem item = menu.findItem(id);
        item.setVisible(true);
    }

    private void hideOptionWithId(int id){
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    private void insertCandidateIntoDB(){
        getDbHelper();
        int status = dbHelper.createNewTargetLocationArea(candidateMarker.getPosition().latitude, candidateMarker.getPosition().longitude, radius, "work", "ibm");
        if(status == 1){
            Toast toast = Toast.makeText(this, "db insert successful", Toast.LENGTH_LONG);
            toast.show();
        } else {
            Toast toast = Toast.makeText(this, "db insert failed", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void cancelCandidate(){
        if((candidateCircle != null) && (candidateMarker != null)){
            candidateCircle.remove();
            candidateCircle = null;
            candidateMarker.remove();
            candidateMarker = null;
            hideOptionWithId(R.id.action_save);
            hideOptionWithId(R.id.action_cancel);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.menu = menu;
        getMenuInflater().inflate(R.menu.add_tla, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.home:
            case android.R.id.home:
                Intent intent = new Intent(AddTargetLocationArea.this, MainActivity.class);
                startActivity(intent);
                finish();
                //overridePendingTransition(R.anim.enter_from_left, R.anim.exit_out_right);
                return true;
            case R.id.action_settings:
                return true;
            case R.id.action_save:
                insertCandidateIntoDB();
                return true;
            case R.id.action_cancel:
                cancelCandidate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

        /*
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);*/
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
                    Location mostRecentUserLocation = LocationUpdater.getLastKnownLocation(this);
                    LatLng markerPos = new LatLng(mostRecentUserLocation.getLatitude(), mostRecentUserLocation.getLongitude());
                    markerUserLocation = map.addMarker(new MarkerOptions()
                                    .position(markerPos)
                                    .draggable(false)
                                    .title("this is your last known location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                    );
                    CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(markerPos, 15);
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
}
