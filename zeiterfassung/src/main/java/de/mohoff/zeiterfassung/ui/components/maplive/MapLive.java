package de.mohoff.zeiterfassung.ui.components.maplive;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
//import com.google.maps.android.clustering.Cluster;
//import com.google.maps.android.clustering.ClusterManager;
//import de.mohoff.zeiterfassung.ui.components.LocClusterRenderer;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.helpers.GeneralHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.datamodel.Timeslot;
import de.mohoff.zeiterfassung.locationservice.LocationChangeListener;
import de.mohoff.zeiterfassung.locationservice.LocationService;
import de.mohoff.zeiterfassung.ui.components.MapAbstract;


// TODO: Eventually add Cluster/ClusterManager. Also add polylines (how to compute polyline dots in cluster center?)
// TODO: replace marker colorBarIcon with greenish dot
// TODO: Maintain two caches: One which represents activeCache, so secondLine that are used for in/outbound computations. And a cache which holds all secondLine updates. Show both on map, first with 100% opacity, second with 20% or similar.
// TODO: Integrate "connection lost" colorBarIcon


public class MapLive extends MapAbstract implements LocationChangeListener{
    //CircularFifoQueue<Loc> userLocs = new CircularFifoQueue<>();
    //private ClusterManager<Loc> clusterManager;
    List<Marker> markers;
    Polyline currentPolyline;
    int polylineColor = Color.BLACK;
    Marker currentLocation;

    private static boolean FOLLOW_MAP_UPDATES;
    private static int CURRENT_LOC_MAX_AGE = 1000 * 60 * 5; // 5 min
    private static int MARKER_ANIMATION_DURATION = 1000; // ms

    private long timestampPreviousMarker = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        markers = new ArrayList<Marker>(LocationCache.getInstance().getPassiveCacheMaxSize());

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateViewWithLayout(inflater, container, savedInstanceState, R.layout.fragment_map);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        polylineColor = getResources().getColor(R.color.greenish_50);
        //context.toolbar.setVisibility(View.GONE);

        FOLLOW_MAP_UPDATES = sp.getBoolean(
                context.getString(R.string.setting_map_follow_updates),
                Boolean.valueOf(context.getString(R.string.setting_map_follow_updates_default_value))
        );

        // Set FAB colorBarIcon and click listener
        context.fab.setImageBitmap(markerCurrentLocation);
        context.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null) {
                    // Only change camera center, do not change zoom level here.
                    centerMapTo(currentLocation.getPosition(), (int) map.getCameraPosition().zoom, true);
                } else if (markers == null || markers.isEmpty()) {
                    Snackbar.make(
                            context.coordinatorLayout,
                            getString(R.string.error_no_data),
                            Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    Snackbar.make(
                            context.coordinatorLayout,
                            getString(R.string.error_no_recent_data),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        });
        context.fab.show();
    }



    /*public Bitmap createCurrentLocationBitmap(Context context, String filePath){
        // Original from asset (png file)
        Bitmap base = createBitmapFromAsset(context, filePath);

        // Resize original down to with of 50px
        int newWidth = 50; // px
        int newHeight = (newWidth * base.getHeight()) / base.getWidth();
        Bitmap resized = Bitmap.createScaledBitmap(base, newWidth, newHeight, false);

        // Add bottom padding of 25px
        int bottomPadding = 25; // px
        Bitmap result = Bitmap.createBitmap(resized.getWidth(), resized.getHeight() + bottomPadding, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(resized, 0, 0, null);

        return result;
    }*/



    @Override
    public void onResume() {
        context.setOnNewLocationListener(this); // setIsRunning listener
        super.onResume();
    }

    @Override
    public void onPause() {
        //context.setOnNewLocationListener(null); // remove listener
        super.onPause();
    }

    @Override
    public void onDestroy() {
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
        super.onMapReady(googleMap);
        CircularFifoQueue<Loc> cache = LocationCache.getInstance().getPassiveCache();

        if(cache != null && cache.size() > 0){
            for(int i=0; i<cache.size(); i++){
                Loc loc = cache.get(i);
                MarkerOptions markerOptions = createMarkerOptions(loc, LocationService.ACCURACY_TRESHOLD);
                addMarkerToMap(map, markers, markerOptions);
                if((i == cache.size()-1) && loc.isNotOlderThan(CURRENT_LOC_MAX_AGE)){
                    updateCurrentLocationMarker(map, loc);
                }
            }
            currentPolyline = addPolylineToMap(map, cache);
            followWithCamera(markers, SHOW_MAP_ANIMATIONS);
        } else {
            Snackbar.make(context.coordinatorLayout, getString(R.string.error_no_data), Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private MarkerOptions createMarkerOptions(Loc loc, double accuracyTreshold){
        // Setup marker properties
        LatLng latLng = loc.getLatLng();
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .draggable(false)
                //.alpha(GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy()))
                .anchor(0.5f, 0.5f) // anchor in the very center of the marker colorBarIcon
                .title("Location")
                .snippet("A:" + loc.getAccuracy() +
                                "\n O:" + GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy()) +
                                "\n t:" + Timeslot.getReadableDuration(timestampPreviousMarker, loc.getTimestampInMillis(), false, false)
                );

        // Artificially created (non-real) Locs are always accurate because they are retrieved
        // from activeCache. Therefore there is no need for distinguishing accurate and inaccurate
        // markers visually.
        if(loc.isRealUpdate()){
            if(loc.getAccuracy() < accuracyTreshold){
                markerOptions = setIcon(markerAccurate, markerOptions, latLng);
            } else {
                markerOptions = setIcon(markerInaccurate, markerOptions, latLng);
            }
        } else {
            markerOptions = setIcon(markerNoConnection, markerOptions, latLng);
        }
        return markerOptions;
    }

    private MarkerOptions setIcon(Bitmap template, MarkerOptions mo, LatLng newLatLng){
        int latLngSeries = getLengthOfLatLngSeries(newLatLng);
        if(latLngSeries > 1){
            markerWithNumbers = template.copy(template.getConfig(), true);
            markerWithNumbers = addTextToBitmap(markerWithNumbers, String.valueOf(latLngSeries));
            mo.icon(BitmapDescriptorFactory.fromBitmap(markerWithNumbers));
        } else {
            mo.icon(BitmapDescriptorFactory.fromBitmap(template));
        }
        return mo;
    }

    private int getLengthOfLatLngSeries(LatLng latLng){
        int result = 1;
        // Only check for markers array interval [0...size-2] since the last element (size-1) will
        // drop out this iteration and latLng will be added to markers at index 0.
        for(int i=0; i<=markers.size()-2; i++){
            if(markers.get(i).getPosition().equals(latLng)){
                result++;
            } else {
                return result;
            }
        }
        return result;
    }

    private void addMarkerToMap(GoogleMap map, List<Marker> markers, MarkerOptions markerOptions) {
        // Ensure that there are only passiveCache.maxSize() markers displayed to prevent memory leak.
        // If passiveQueue is full and at least one drop happened already in it
        // (= hasFirstPassiveQueueDropHappened()), remove oldest marker.
        if(LocationCache.getInstance().hasFirstPassiveQueueDropHappened() && markers.size() == LocationCache.getInstance().getPassiveCacheMaxSize()){
            try {
                // Determine index which should be deleted in markers arraylist. Markers with numbers
                // are placed on top of each other. If there one marker of that marker stack needs to
                // be removed, delete the one with the highest number (=youngest one).
                // Implementation: If all elements of arraylist are distinct,
                // deleteIndex = markers.size()-1. If not, iterate from [size()-2 ... 0] to isRunning the
                // youngest marker which position is the same as the oldest marker. Delete the
                // youngest marker in this series of markers of same positions.
                Marker oldest = markers.get(markers.size()-1);
                int deleteIndex = markers.size()-1;
                for(int i=markers.size()-2; i>=0; i--){
                    if(!markers.get(i).getPosition().equals(oldest.getPosition())) {
                        deleteIndex = i+1;
                        break;
                    }
                    deleteIndex = i;
                }
                // Remove marker from map (marker object is still in markers arraylist)
                markers.get(deleteIndex).remove();
                // Remove marker from markers arraylist, so marker is completely removed.
                markers.remove(deleteIndex);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        // Add new marker to map and to arraylist markers. markers' size is at most markers.maxSize()-1
        // as we ensured above in this method. Thus inserting the new marker at index 0 is ok because
        // all other elements can be shifted so that the empty space in the arraylist is filled.
        // Now the arraylist is full (again) and in the next iteration one element needs to be removed
        // again to create one empty index in arraylist.
        Marker marker = map.addMarker(markerOptions);
        markers.add(0, marker);
    }

    private Polyline addPolylineToMap(GoogleMap map, CircularFifoQueue<Loc> locs){
        PolylineOptions options = new PolylineOptions()  // .geodesic(false)
                .color(polylineColor)
                .width(15);

        for(Loc loc : locs){
            options.add(GeneralHelper.convertLocToLatLng(loc));
        }

        return map.addPolyline(options);
    }

    private void followWithCamera(List<Marker> markers, boolean showAnimation){
        ArrayList<LatLng> respectedLatLngs = new ArrayList<>();
        try {
            respectedLatLngs.add(markers.get(0).getPosition());
            respectedLatLngs.add(markers.get(1).getPosition());
        } catch (Exception e){
            // .isRunning(1) failed because it's not yet filled.
        }
        centerMapTo(getMapViewport(respectedLatLngs), showAnimation);
    }

    public void onNewLocation(Loc loc) {
        if(map != null){
            MarkerOptions markerOptions = createMarkerOptions(loc, LocationService.ACCURACY_TRESHOLD);
            addMarkerToMap(map, markers, markerOptions);
            updateCurrentLocationMarker(map, loc);

            if(FOLLOW_MAP_UPDATES){
                followWithCamera(markers, true);
            }

            // Update polyline
            if(currentPolyline != null){
                currentPolyline.remove();
            }
            currentPolyline = addPolylineToMap(map, LocationCache.getInstance().getPassiveCache());
        } else {
            Snackbar.make(context.coordinatorLayout, getString(R.string.error_no_init), Snackbar.LENGTH_LONG)
                    .show();
        }
        timestampPreviousMarker = loc.getTimestampInMillis();
    }

    private void updateCurrentLocationMarker(GoogleMap map, Loc loc){
        if(currentLocation == null){
            currentLocation = map.addMarker(new MarkerOptions()
                    .position(loc.getLatLng())
                    .draggable(false)
                            //.colorBarIcon(BitmapDescriptorFactory.fromAsset("markers/marker1.png"))
                    .icon(BitmapDescriptorFactory.fromBitmap(markerCurrentLocation))
                    //.colorBarIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title(context.getString(R.string.map_current_location)))
                    ;
        } else {
            animateMarker(currentLocation, loc.getLatLng());
        }
    }

    // Following snippet allows marker animations from start to end secondLine.
    // Code taken from https://gist.github.com/broady/6314689
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void animateMarker(Marker marker, LatLng finalPosition) {
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);
        animator.setDuration(MARKER_ANIMATION_DURATION);
        animator.start();
    }

    private LatLng interpolate(float fraction, LatLng a, LatLng b) {
        double lat = (b.latitude - a.latitude) * fraction + a.latitude;
        double lngDelta = b.longitude - a.longitude;

        // Take the shortest path across the 180th meridian.
        if (Math.abs(lngDelta) > 180) {
            lngDelta -= Math.signum(lngDelta) * 360;
        }
        double lng = lngDelta * fraction + a.longitude;
        return new LatLng(lat, lng);
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
                //GeneralHelper.showToast(context, "no secondLine data available.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            // hide spinner
            progressBar.setVisibility(View.GONE);
            if(!this.markersAdded){
                //Snackbar.make(context.coordinatorLayout, "Currently no secondLine data available.", Snackbar.LENGTH_LONG)
                //        .show();
            }
        }
    }

}
