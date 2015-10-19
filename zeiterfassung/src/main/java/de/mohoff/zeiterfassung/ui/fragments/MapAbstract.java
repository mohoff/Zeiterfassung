package de.mohoff.zeiterfassung.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.ui.MainActivity;

/**
 * Created by moo on 8/16/15.
 */
public class MapAbstract extends Fragment implements OnMapReadyCallback {
    protected MainActivity parentActivity;
    protected static View view;
    protected ProgressBar progressBar;

    protected MapFragment mapFragment;
    protected GoogleMap map;
    protected Geocoder geocoder;
    // Should be replaced with "greenish_50" in onCreateView().
    // Work around for "fragment not attached to activity" error.
    int polylineColor = Color.BLACK;

    protected DatabaseHelper dbHelper = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateViewWithLayout(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState, int layout) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = (RelativeLayout) inflater.inflate(layout, container, false);
        } catch (InflateException e) {
            // map is already there, just return view as it is
        }
        //return this.onCreateView(inflater, container, savedInstanceState);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        /*if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = (RelativeLayout) inflater.inflate(R.layout.fragment_map, container, false);
        } catch (InflateException e) {
            // map is already there, just return view as it is
        }*/

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        parentActivity = (MainActivity) getActivity();
        geocoder = new Geocoder(getActivity());
        dbHelper = getDbHelper(parentActivity);

        polylineColor = getResources().getColor(R.color.greenish_50);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            OpenHelperManager.releaseHelper();
            dbHelper = null;
        }
        super.onDestroy();
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
        // Disable "Navigation" and "GPS Pointer" buttons whiche are visible by default
        map.getUiSettings().setMapToolbarEnabled(false);
        // Enables "Show my location" button which shows current location with bearing on the map
        // map.getUiSettings().setMyLocationButtonEnabled(true);

        progressBar.setVisibility(View.GONE);
    }

    protected void addMarkerToMap(GoogleMap map, List<Marker> markers, LatLng latLng, float opacity, String title, String snippet){
        Marker marker = map.addMarker(new MarkerOptions()
                .position(latLng)
                .draggable(false)
                .alpha(opacity)
                .icon(BitmapDescriptorFactory.defaultMarker(193))      // BitmapDescriptorFactory.HUE_MAGENTA
                .title(title)
                .snippet(snippet)
        );

        if(markers != null){
            markers.add(marker);
            if(markers.size() > 1){
                // Change color for old marker
                markers.get(markers.size()-2).setIcon(BitmapDescriptorFactory.defaultMarker(0)); // BitmapDescriptorFactory.HUE_VIOLET
                // Move center of map to new marker ... in some cases not wanted --> TODO: checkbox on UI asking "follow location updates on the map"
                // Really reset zoomLevel each call to 17?
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            } else {
                // Zoom map in to marker, if the marker is the first one on the map.
                // Zoom level 17 turns out to be nice because its the lowest one in which you can
                // see building outlines.
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            }
        } else {
            // No list "markers" is passed, so we are in "Add new TLA/Zone".
            // Just move camera towards desired position without resetting the zoom level every time.
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    protected Polyline addPolylineToMap(GoogleMap map, CircularFifoQueue<Loc> queueLocs){
        PolylineOptions options = new PolylineOptions()  // .geodesic(false)
                .color(polylineColor)
                .width(15);

        for(Loc loc : queueLocs){
            options.add(GeneralHelper.convertLocToLatLng(loc));
        }

        return map.addPolyline(options);
    }

    protected void centerMapTo(LatLng cameraCenter){
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(cameraCenter, 15);
        map.animateCamera(cu);
    }

    protected MarkerOptions createMarkerOptions(LatLng pos, boolean isDraggable, String title, String snippet, BitmapDescriptor bitmapDescriptor){
        return new MarkerOptions()
                .position(pos)
                .draggable(isDraggable)
                .title(title)
                .snippet(snippet)
                .icon(bitmapDescriptor);
    }

    protected CircleOptions createCircleOptions(LatLng pos, int radius, int fillColor){
        return new CircleOptions()
                .center(pos)
                .radius(radius)
                .fillColor(fillColor)
                .strokeWidth(0)
                .strokeColor(Color.TRANSPARENT);
    }


    protected DatabaseHelper getDbHelper(Context context) {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }
    

}


