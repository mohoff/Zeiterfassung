package de.mohoff.zeiterfassung.ui.components.maplive;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
//import com.google.maps.android.clustering.Cluster;
//import com.google.maps.android.clustering.ClusterManager;
//import de.mohoff.zeiterfassung.ui.components.LocClusterRenderer;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.helpers.GeneralHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.datamodel.Timeslot;
import de.mohoff.zeiterfassung.locationservice.LocationChangeListener;
import de.mohoff.zeiterfassung.ui.components.MapAbstract;


// TODO: Eventually add Cluster/ClusterManager. Also add polylines (how to compute polyline dots in cluster center?)
// TODO: replace marker icon with greenish dot
// TODO: Maintain two caches: One which represents activeCache, so location that are used for in/outbound computations. And a cache which holds all location updates. Show both on map, first with 100% opacity, second with 20% or similar.
// TODO: Integrate "connection lost" icon


public class MapLive extends MapAbstract implements LocationChangeListener{
    //CircularFifoQueue<Loc> userLocs = new CircularFifoQueue<>();
    //private ClusterManager<Loc> clusterManager;
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
        //userLocs = parentActivity.getLocs();

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
        CircularFifoQueue<Loc> cache = LocationCache.getInstance().getPassiveCache();
        //clusterManager = new ClusterManager<Loc>(getActivity(), map);
        //map.setOnCameraChangeListener(clusterManager);
        //map.setOnInfoWindowClickListener(clusterManager);
        //map.setOnMarkerClickListener(clusterManager);
        //clusterManager.setOnClusterClickListener(this);
        //clusterManager.setOnClusterInfoWindowClickListener(this);
        //clusterManager.setOnClusterItemClickListener(this);
        //clusterManager.setOnClusterItemInfoWindowClickListener(this);
        //clusterManager.setRenderer(new LocClusterRenderer(getActivity(), map, clusterManager));
        //clusterManager.setRenderer(new LocClusterRenderer(getActivity(), map, clusterManager));


        if(cache != null && cache.size() > 0){
            for(int i=0; i<cache.size(); i++){
                //clusterManager.addItems(cache);
                Loc loc = cache.get(i);
                long lastMarkerTimestamp = (i-1)>= 0 ? cache.get(i-1).getTimestampInMillis() : 0;
                super.addMarkerToMap(
                        map,
                        markers,
                        GeneralHelper.convertLocToLatLng(loc),
                        GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy()),
                        "Location",
                        "A:" + loc.getAccuracy() +
                                "\n O:" + GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy()) +
                                "\n t:" + Timeslot.getReadableDuration(lastMarkerTimestamp, loc.getTimestampInMillis(), false, false),
                        loc.isRealUpdate()
                );
            }
            currentPolyline = super.addPolylineToMap(map, cache);
        } else {
            GeneralHelper.showToast(parentActivity, "no location data available.");
        }
    }

    public void onNewLocation(Loc loc) {
        if(map != null){
            // Update markers
            long timestampLastMarker;
            try {
                timestampLastMarker = LocationCache.getInstance().getPassiveCache().get(1).getTimestampInMillis();
            } catch (Exception e){
                // No other markers exist
                timestampLastMarker = 0;
            }
            //clusterManager.addItem(loc);
            //clusterManager.cluster();
            super.addMarkerToMap(
                    map,
                    markers,
                    GeneralHelper.convertLocToLatLng(loc),
                    GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy()),
                    "Location",
                    "A:" + loc.getAccuracy() +
                            "\n O:" + GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy()) +
                            "\n t:" + Timeslot.getReadableDuration(timestampLastMarker, loc.getTimestampInMillis(), false, false),
                    loc.isRealUpdate()
            );

            // Ensure that there are only passiveCache.maxSize() markers displayed to prevent memory leak.
            // If passiveQueue is full and at least one drop happened already in it, remove oldest marker.
            if(LocationCache.getInstance().hasFirstPassiveQueueDropHappened()){
                // Remove oldest marker from map
                markers.get(0).remove();
                // Remove oldest/first element from marker list
                markers.remove(0);
            }

            // Update polyline
            if(currentPolyline != null){
                currentPolyline.remove();
            }
            currentPolyline = super.addPolylineToMap(map, LocationCache.getInstance().getPassiveCache());
        } else {
            GeneralHelper.showToast(parentActivity, "no map object initialized.");
        }
    }

    // not applicable because you can't modify UI elements in doInBackground ("map" in this case)
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
                    // (method signature updated meanwhile...)
                    //addMarkerToMap(map, markers, GeneralHelper.convertLocToLatLng(loc), 1);
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
