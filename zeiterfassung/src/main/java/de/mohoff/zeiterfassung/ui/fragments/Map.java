package de.mohoff.zeiterfassung.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.ui.MainActivity;
import de.mohoff.zeiterfassung.legacy.LocationUpdater;

public class Map extends Fragment implements OnMapReadyCallback {
    Activity parentActivity;
    private static View view;

    private MapFragment mapFragment;

    GoogleMap map;
    LatLng mostRecentUserLocation = null;
    List<Loc> userLocations = new ArrayList<>();
    List<Marker> markers = new ArrayList<Marker>();
    Marker markerUserLocation;
    Marker markerCandidate = null;
    Circle circle = null;

    private int amountOfTemporarySavedLocations = 5;
    private CircularFifoQueue locationCache = new CircularFifoQueue<Location>(amountOfTemporarySavedLocations); // fifo based queue

    EditText et;
    int radius;


    public Map(ArrayList<Loc> locs) {
        // Required empty public constructor
        this.userLocations = locs;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = (FrameLayout) inflater.inflate(R.layout.fragment_map, container, false);
        } catch (InflateException e) {
            /* map is already there, just return view as it is */
        }
        
        parentActivity = getActivity();

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*
        et = (EditText) findViewById(R.id.editText);
        updateRadiusFromEditText();
        */

        // store userLocations temporarly in bundle, to repopulate map after screen rotation
        /*if(savedInstanceState != null){
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
        }*/

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        /*if (map != null)
            setUpMap();

        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = ((MapFragment) MainActivity.fragM.findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (map != null)
                setUpMap();
        }*/


    }

    @Override
    public void onResume() {

        // set newest markers

        ArrayList<Loc> locs = ((MainActivity) getActivity()).getLocs();



        super.onResume();
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (map != null) {
            MainActivity.fragM.beginTransaction()
                    .remove(MainActivity.fragM.findFragmentById(R.id.map)).commit();
            map = null;
        }
    }

    public void drawLocationUpdate(Loc loc){
        // transform to my "Loc" datatype
        locationCache.add(loc);
        drawMarkerForLocation(loc);
        if(!markers.isEmpty()){
            markers.get(markers.size()-2).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        }
        try {
            // may cause racecondition with second listener implemenatation (following line might get old interpolated position)
            //drawMarkerForLatLng(luh.getInterpolatedPositionInLatLng());
        } catch(Exception e){
            e.printStackTrace();
        }


    }



    public void drawMarkerForLocation(Loc loc){
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        markerUserLocation = map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .draggable(false)
                        .title("acc " + String.valueOf(loc.getAccuracy()) + ", speed " + String.valueOf(loc.getSpeed()) + ", alt " + String.valueOf(loc.getAltitude()))
                                //.snippet(loc.getExtras().toString())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
        );
        //userLocations.add(loc);
        markers.add(markerUserLocation);
    }

    public void drawMarkerForLatLng(LatLng latLng){
        map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .draggable(false)
                                //.title("acc " + String.valueOf(loc.getAccuracyMultiplier()) + ", speed " + String.valueOf(loc.getSpeed()) + ", alt " + String.valueOf(loc.getAltitude()))
                                //.snippet(loc.getExtras().toString())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        );
    }
    /*
    public void updateRadiusFromEditText(){
        String inputString = et.getText().toString();
        try{
            radius = Integer.valueOf(inputString);
        } catch (Exception e){
            radius = -1;
        }
    }
    */
    /*@Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }*/

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        parentActivity.getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }*/

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()) {
            case R.id.home:
            case android.R.id.home:
                Intent intent = new Intent(Map.this, MainActivity.class);
                startActivity(intent);
                finish();
                //overridePendingTransition(R.anim.enter_from_left, R.anim.exit_out_right);
                return true;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
*/
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            //map = ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.map)).getMap();
            map = ((MapFragment) MainActivity.fragM.findFragmentById(R.id.map)).getMap();


            //map = ((MapFragment) MainActivity.fragM
            //        .findFragmentById(R.id.map)).getMap();

            // Check if we were successful in obtaining the map.
            if (map != null) {
                // The Map is verified. It is now safe to manipulate the map.
                setUpMap();
            }
        }
    }

    public void setUpMap(){

    }

    public void onMarkerClick(final Marker marker) {
        System.out.println("marker click!");
        /*if (marker.equals(markerIBM))
        {
            //handle click here
            markerIBM.hideInfoWindow();
        }*/
    }

    /*@Override
    protected void onSaveInstanceState(Bundle outState) {
        //if(mostRecentUserLocation != null){
        //    double[] latLng = {mostRecentUserLocation.latitude, mostRecentUserLocation.longitude};
        //    outState.putDoubleArray("mostRecentUserLocation", latLng);
        //}
        if(!userLocations.isEmpty()){
            outState.putParcelableArrayList("userLocations", userLocations);
        }

        super.onSaveInstanceState(outState);
    }*/

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //googleMap.setMyLocationEnabled(true); // displays current user location with bearing on the map

        mostRecentUserLocation = new LatLng(LocationUpdater.mostRecentLocation.getLatitude(), LocationUpdater.mostRecentLocation.getLongitude());
        markers.add(googleMap.addMarker(new MarkerOptions()
                        .position(mostRecentUserLocation)
                        .draggable(false)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
        ));

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(mostRecentUserLocation, 15));

        for(Loc loc : this.userLocations){
            drawMarkerForLocation(loc);
        }

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                InputMethodManager imm = (InputMethodManager) parentActivity.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et.getWindowToken(), 0);

                if (markerCandidate != null) {
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
                            .strokeColor(Color.TRANSPARENT);
                    circle = map.addCircle(circleOptions);
                }
            }
        });
        /*
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
        */
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
                circle.setVisible(false);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                if (arg0.equals(markerCandidate)) {
                    circle.setCenter(arg0.getPosition());
                    circle.setVisible(true);
                }
            }

            @Override
            public void onMarkerDrag(Marker arg0) {

            }
        });
    }
}
