package de.mohoff.zeiterfassung.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

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
import com.google.android.gms.maps.model.Polyline;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.locationservice.LocationChangeListener;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class Map extends MapAbstract implements LocationChangeListener {
    CircularFifoQueue<Loc> userLocs = new CircularFifoQueue<>();
    List<Marker> markers = new ArrayList<Marker>();
    Polyline currentPolyline;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateViewWithLayout(inflater, container, savedInstanceState, R.layout.fragment_map);
    }

    @Override
    public void onResume() {
        // set newest fixMarkers

        // Get most recent locations from MainActivity and convert to LatLngs (google map objects)
        //CircularFifoQueue<Loc> userLocs = parentActivity.getLocs();
        //for(Loc loc : userLocs){
        //    userLatLngs.add(GeneralHelper.convertLocToLatLng(loc));
        //}
        userLocs = parentActivity.getLocs();

        parentActivity.setOnNewLocationListener(this); // set listener
        super.onResume();
    }

    @Override
    public void onPause() {
        //parentActivity.setOnNewLocationListener(null); // remove listener
        super.onPause();
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
        super.onMapReady(googleMap);

        if(userLocs != null && userLocs.size() > 0){
            for(Loc loc : userLocs){
                super.addMarkerToMap(map, markers,
                        GeneralHelper.convertLocToLatLng(loc),
                        GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy()));
            }
            currentPolyline = super.addPolylineToMap(map, userLocs);
        } else {
            GeneralHelper.showToast(parentActivity, "no location data available.");
        }
    }

    public void onNewLocation(Loc loc) {
        LatLng latLng = GeneralHelper.convertLocToLatLng(loc);
        float opacity = GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy());

        userLocs.add(loc);
        if(map != null){
            // Update markers
            addMarkerToMap(map, markers, latLng, opacity);
            // Ensure that there are only userLocations.maxSize() locations displayed to prevent memory leak
            if(userLocs.size() == userLocs.maxSize()){ // if circularFifoQueue is full...
                // Remove oldest marker from map
                markers.get(0).remove();
                // Remove oldest/first element from marker list
                markers.remove(0);
            }

            // Update polyline
            if(currentPolyline != null){
                currentPolyline.remove();
            }
            currentPolyline = super.addPolylineToMap(map, userLocs);
        } else {
            GeneralHelper.showToast(parentActivity, "no map object initialized.");
        }
    }






    // not applyable because you can't modify UI elements in doInBackground ("map" in this case)
    private class LoadingMapTask extends AsyncTask<Void, Void, Void> {
        private CircularFifoQueue<Loc> locs;
        private GoogleMap map;
        private List<Marker> markers;
        private boolean markersAdded;

        LoadingMapTask(List<Marker> markers, CircularFifoQueue locs, GoogleMap map){
            this.locs = locs;
            this.map = map;
            this.markers = markers;
            this.markersAdded = false;
        }

        @Override
        protected void onPreExecute() {
            // show spinner
            /*dialog = new ProgressDialog(Main.this);
            dialog.setMessage("Loading....");
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.show(); //Maybe you should call it in ruinOnUIThread in doInBackGround as suggested from a previous answer
            */
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(this.locs.size() > 0){
                this.markersAdded = true;
                for( Loc loc : this.locs){
                    addMarkerToMap(map, markers, GeneralHelper.convertLocToLatLng(loc), 1);
                }
            } else {
                //GeneralHelper.showToast(parentActivity, "no location data available.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            // hide spinner
            progressBar.setVisibility(View.GONE);
            if(!this.markersAdded){
                GeneralHelper.showToast(parentActivity, "no location data available.");
            }
        }
    }

}
