package de.mohoff.zeiterfassung.ui.components;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.helpers.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.ui.MainActivity;

/**
 * Created by moo on 8/16/15.
 */
public class MapAbstract extends Fragment implements OnMapReadyCallback {
    private static int DEFAULT_ZOOM_LEVEL = 17;
    private static int MARKER_VIEWPORT_PADDING = 200; // px

    protected MainActivity parentActivity;
    protected static View view;
    protected ProgressBar progressBar;

    protected MapFragment mapFragment;
    protected GoogleMap map;
    protected Geocoder geocoder;
    // Should be replaced with "greenish_50" in onCreateView().
    // Work around for "fragment not attached to firstLine" error.


    protected DatabaseHelper dbHelper = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateViewWithLayout(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState, int layout) {
        //view = inflater.inflate(layout, container, false);
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(layout, container, false);
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
    public void onDestroyView() {
        /* Following snippet is a workaround for an error which leads to an inflate exception because
           the wrapping fragment can't contain another fragment, MapFragment in this case.
           We use the FragmentManager to remove the MapFragment manually from the parent view so that
           the parent view can be inflated properly with a new MapFragment.
         */
        try {
            MapFragment fragment = (MapFragment)(getFragmentManager().findFragmentById(R.id.map));
            FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
            ft.remove(fragment);
            //ft.commit();
            ft.commitAllowingStateLoss();
        } catch(Exception e){
            e.printStackTrace();
        }

        super.onDestroyView();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        // Disable "Navigation" and "GPS Pointer" buttons whiche are visible by default
        map.getUiSettings().setMapToolbarEnabled(false);
        // Enables "Show my secondLine" button which shows current secondLine with bearing on the map
        // map.getUiSettings().setMyLocationButtonEnabled(true);

        progressBar.setVisibility(View.GONE);
    }

    protected void addMarkerToMap(GoogleMap map, List<Marker> markers, LatLng latLng, float opacity, String title, String snippet, boolean isRealUpdate){
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .draggable(false)
                .alpha(opacity)
                .anchor(0.5f, 0.5f)
                //.colorBarIcon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                //.colorBarIcon(BitmapDescriptorFactory.fromAsset("markers/marker1.png"))
                .icon(BitmapDescriptorFactory.defaultMarker(193))      // BitmapDescriptorFactory.HUE_MAGENTA
                .title(title)
                .snippet(snippet)
        ;

        if(!isRealUpdate){
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        }

        Marker marker = map.addMarker(markerOptions);

        if(markers != null){
            markers.add(marker);
            if(markers.size() > 1){
                // Change color for old marker
                markers.get(markers.size()-2).setIcon(BitmapDescriptorFactory.defaultMarker(0)); // BitmapDescriptorFactory.HUE_VIOLET
                // Move center of map to new marker ... in some cases not wanted --> TODO: checkbox on UI asking "follow secondLine updates on the map"
                // Really reset zoomLevel each call to 17?
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            } else {
                // Zoom map in to marker, if the marker is the first one on the map.
                // Zoom level 17 turns out to be nice because its the lowest one in which you can
                // see building outlines.
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            }
        } else {
            // No list "markers" is passed, so we are in "Add new Zone".
            // Just move camera towards desired position without resetting the zoom level every time.
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    protected void centerMapTo(LatLng cameraCenter){
        centerMapTo(CameraUpdateFactory.newLatLngZoom(cameraCenter, DEFAULT_ZOOM_LEVEL));
    }

    protected void centerMapTo(LatLng cameraCenter, int zoomLevel){
        if(zoomLevel <= 0){
            zoomLevel = DEFAULT_ZOOM_LEVEL;
        }
        centerMapTo(CameraUpdateFactory.newLatLngZoom(cameraCenter, zoomLevel));
    }

    protected  void centerMapTo(CameraUpdate cu){
        map.animateCamera(cu);
    }

    /*protected MarkerOptions createMarkerOptions(boolean isDraggable, String title, String snippet, BitmapDescriptor bitmapDescriptor){
        // without position
        return new MarkerOptions()
                .draggable(isDraggable)
                .title(title)
                .snippet(snippet)
                .colorBarIcon(bitmapDescriptor);
    }*/

    protected CircleOptions createCircleOptions(int fillColor){
        // without position and radius
        return new CircleOptions()
                .fillColor(fillColor)
                .strokeWidth(0)
                .strokeColor(Color.TRANSPARENT);
    }

    public static CameraUpdate getMapViewport(ArrayList<LatLng> latLngList){
        LatLngBounds.Builder boundBilder = LatLngBounds.builder();
        for(LatLng latLng : latLngList){
            boundBilder.include(latLng);
        }
        LatLngBounds bounds = boundBilder.build();

        return CameraUpdateFactory.newLatLngBounds(bounds, MARKER_VIEWPORT_PADDING);
    }

    protected DatabaseHelper getDbHelper(Context context) {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }
    

}


