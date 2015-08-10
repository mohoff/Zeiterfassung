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

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.locationservice.LocationChangeListener;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class Map extends Fragment implements OnMapReadyCallback, LocationChangeListener {
    MainActivity parentActivity;
    private static View view;

    private MapFragment mapFragment;
    private GoogleMap map;
    private int newLocsReceivedWhileMapVisible = 0;

    CircularFifoQueue<Loc> userLocations;
    List<Marker> markers = new ArrayList<Marker>();
    Marker markerUserLocation;
    Marker markerCandidate = null;
    Circle circle = null;

    private int amountOfTemporarySavedLocations = 5;
    private CircularFifoQueue locationCache = new CircularFifoQueue<Location>(amountOfTemporarySavedLocations); // fifo based queue

    EditText et;
    int radius;

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

        parentActivity = (MainActivity) getActivity();

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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

        // get most recent locations from MainActivity
        this.userLocations = parentActivity.getLocs();
        this.newLocsReceivedWhileMapVisible = 0;
        parentActivity.setOnNewLocationListener(this); // set listener

        super.onResume();
    }

    @Override
    public void onPause() {
        parentActivity.setOnNewLocationListener(null); // remove listener
        super.onPause();
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (map != null) {
            MainActivity.fragM.beginTransaction()
                    .remove(MainActivity.fragM.findFragmentById(R.id.map)).commit();
            map = null;
        }
    }

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
        this.map = googleMap;
        //googleMap.setMyLocationEnabled(true); // displays current user location with bearing on the map

        if(this.userLocations.size() > 0){
            for(Loc loc : userLocations){
                addMarkerToMap(this.map, GeneralHelper.convertLocToLatLng(loc));
            }
        } else {
            GeneralHelper.showToast(parentActivity, "no location data available.");
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

    private void addMarkerToMap(GoogleMap map, LatLng latLng){
        this.markers.add(map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .draggable(false)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                //.title("acc " + String.valueOf(loc.getAccuracy()) + ", speed " + String.valueOf(loc.getSpeed()) + ", alt " + String.valueOf(loc.getAltitude()))
        ));
        if(this.markers.size() > 1){
            // change color for old marker
            this.markers.get(markers.size()-2).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
            // move center of map to new marker ... in some cases not wanted --> TODO: checkbox on UI asking "follow location updates on the map"
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        } else {
            // zoom map in to marker, if the marker is the first one on the map
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }

    public void onNewLocation(Loc loc) {
        newLocsReceivedWhileMapVisible++;
        if(this.map != null){
            addMarkerToMap(this.map, GeneralHelper.convertLocToLatLng(loc));
            // ensure that there are only userLocations.maxSize() locations displayed to prevent memory leak
            if(this.userLocations.size() == this.userLocations.maxSize()){ // if circularFifoQueue is full...
                this.markers.get(0).remove(); // remove oldest marker from map
                this.markers.remove(0); // remove oldest/first element from marker list
            }
        } else {
            GeneralHelper.showToast(parentActivity, "no map object initialized.");
        }
    }
}
