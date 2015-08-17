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

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.locationservice.LocationChangeListener;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class Map extends MapAbstract implements LocationChangeListener {
    CircularFifoQueue<Loc> userLocations;
    List<Marker> markers = new ArrayList<Marker>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        // set newest markers

        // get most recent locations from MainActivity
        userLocations = parentActivity.getLocs();
        parentActivity.setOnNewLocationListener(this); // set listener
        super.onResume();
    }

    @Override
    public void onPause() {
        parentActivity.setOnNewLocationListener(null); // remove listener
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

        if(userLocations != null && userLocations.size() > 0){
            for(Loc loc : userLocations){
                super.addMarkerToMap(markers, map, GeneralHelper.convertLocToLatLng(loc));
            }
        } else {
            GeneralHelper.showToast(parentActivity, "no location data available.");
        }
    }

    public void onNewLocation(Loc loc) {
        if(map != null){
            addMarkerToMap(markers, map, GeneralHelper.convertLocToLatLng(loc));
            // ensure that there are only userLocations.maxSize() locations displayed to prevent memory leak
            if(userLocations.size() == userLocations.maxSize()){ // if circularFifoQueue is full...
                markers.get(0).remove(); // remove oldest marker from map
                markers.remove(0); // remove oldest/first element from marker list
            }
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
                    addMarkerToMap(this.markers, this.map, GeneralHelper.convertLocToLatLng(loc));
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
