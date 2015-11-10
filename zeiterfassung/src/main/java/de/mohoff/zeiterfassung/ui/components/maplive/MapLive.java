package de.mohoff.zeiterfassung.ui.components.maplive;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
// TODO: replace marker icon with greenish dot
// TODO: Maintain two caches: One which represents activeCache, so location that are used for in/outbound computations. And a cache which holds all location updates. Show both on map, first with 100% opacity, second with 20% or similar.
// TODO: Integrate "connection lost" icon


public class MapLive extends MapAbstract implements LocationChangeListener{
    //CircularFifoQueue<Loc> userLocs = new CircularFifoQueue<>();
    //private ClusterManager<Loc> clusterManager;
    List<Marker> markers = new ArrayList<Marker>();
    Polyline currentPolyline;
    int polylineColor = Color.BLACK;
    Marker currentLocation;

    // Bitmaps for Marker icons in 3 different variants
    Bitmap markerAccurate, markerInaccurate, markerNoConnection;
    // Bitmap for Markers which will be displayed with a number on them.
    // While generating this Bitmap, the number will be printed on of the 3 templates above.
    Bitmap markerWithNumbers;

    private static int MARKER_DIM = 50; // px
    private static int MARKER_ANIMATION_DURATION = 1000;

    private long timestampPreviousMarker = 0;

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        polylineColor = getResources().getColor(R.color.greenish_50);

        markerAccurate = createBitmapFromDrawable(MARKER_DIM, R.drawable.mapmarker, true, "");
        markerInaccurate = createBitmapFromDrawable(MARKER_DIM, R.drawable.mapmarker, false, "");
        markerNoConnection = createBitmapFromDrawable(MARKER_DIM, R.drawable.mapmarker_noconnection, true, "");
    }

    private Bitmap createBitmapFromDrawable(int dim, int drawable, boolean fullOpacity, String text){
        if(text == null) text = "";
        //Bitmap b = BitmapFactory.decodeResource(getResources(), drawable);
        //b = b.copy(Bitmap.Config.ARGB_8888, true);

        Bitmap b = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        Drawable shape = ContextCompat.getDrawable(parentActivity, drawable);
        shape.setAlpha(fullOpacity ? 255 : 150);
        shape.setBounds(0, 0, b.getWidth(), b.getHeight());
        shape.draw(canvas);


        return b;
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
            currentPolyline = addPolylineToMap(map, cache);
        } else {
            GeneralHelper.showToast(parentActivity, "no location data available.");
        }
    }

    private MarkerOptions createMarkerOptions(Loc loc, double accuracyTreshold){
        // Setup marker properties
        LatLng latLng = loc.getLatLng();
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .draggable(false)
                .alpha(GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy()))
                .anchor(0.5f, 0.5f) // anchor in the very center of the marker icon
                        //.icon(BitmapDescriptorFactory.fromAsset("markers/marker1.png"))
                        //.icon(BitmapDescriptorFactory.defaultMarker(HUE_MAGENTA))
                .title("Location")
                .snippet("A:" + loc.getAccuracy() +
                                "\n O:" + GeneralHelper.getOpacityFromAccuracy(loc.getAccuracy()) +
                                "\n t:" + Timeslot.getReadableDuration(timestampPreviousMarker, loc.getTimestampInMillis(), false, false)
                )
                ;

        if(loc.getAccuracy() < accuracyTreshold){
            setIcon(markerAccurate, markerOptions, latLng);
        } else {
            setIcon(markerInaccurate, markerOptions, latLng);
        }
        // Overwrite .icon property if provided Loc is not a real update
        if(!loc.isRealUpdate()){
            setIcon(markerNoConnection, markerOptions, latLng);
        }
        return markerOptions;
    }

    private void setIcon(Bitmap b, MarkerOptions mo, LatLng newLatLng){
        int latLngSeries = getLengthOfLatLngSeries(newLatLng);
        if(latLngSeries > 1){
            markerWithNumbers = b.copy(b.getConfig(), true);
            markerWithNumbers = addTextToBitmap(markerWithNumbers, String.valueOf(latLngSeries));
            mo.icon(BitmapDescriptorFactory.fromBitmap(markerWithNumbers));
        } else {
            mo.icon(BitmapDescriptorFactory.fromBitmap(b));
        }
    }

    private Bitmap addTextToBitmap(Bitmap b, String text){
        Canvas canvas = new Canvas(b);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(35);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        float x = b.getWidth() / 2.0f;
        float y = (b.getHeight() - bounds.height()) / 2.0f - bounds.top;

        canvas.drawText(text, x, y, paint);
        return b;
    }

    private int getLengthOfLatLngSeries(LatLng latLng){
        int result = 1;
        for(Marker m : markers){
            if(m.getPosition().equals(latLng)){
                result++;
            } else {
                return result;
            }
        }
        return result;
    }

    private void addMarkerToMap(GoogleMap map, List<Marker> markers, MarkerOptions markerOptions) {
        // Add marker to map
        Marker marker = map.addMarker(markerOptions);
        markers.add(0, marker); // Existing elements will be shifted if index 0 is given.//markers.add(marker);

        // Change color for old marker
        //if(markers.size() > 1){
        //    markers.get(1).setIcon(BitmapDescriptorFactory.defaultMarker(0)); // BitmapDescriptorFactory.HUE_VIOLET
        //}

        // Ensure that there are only passiveCache.maxSize() markers displayed to prevent memory leak.
        // If passiveQueue is full and at least one drop happened already in it, remove oldest marker.
        if(LocationCache.getInstance().hasFirstPassiveQueueDropHappened()){
            try {
                // Remove oldest marker from map
                markers.get(markers.size()-1).remove();
                // Remove oldest/first element from marker list
                //markers.remove(0);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
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

    private void followWithCamera(List<Marker> markers){
        ArrayList<LatLng> respectedLatLngs = new ArrayList<>();
        try {
            respectedLatLngs.add(markers.get(0).getPosition());
            respectedLatLngs.add(markers.get(1).getPosition());
        } catch (Exception e){
            // .get(1) failed because it's not yet filled.
        }
        centerMapTo(getMapViewport(respectedLatLngs, 200));
    }

    public void onNewLocation(Loc loc) {
        if(map != null){
            MarkerOptions markerOptions = createMarkerOptions(loc, LocationService.ACCURACY_TRESHOLD);
            addMarkerToMap(map, markers, markerOptions);
            updateCurrentLocationMarker(map, loc);
            // Move center of map to new marker ... in some cases not wanted --> TODO: checkbox on UI asking "follow location updates on the map"
            // Really reset zoomLevel each call to 17?
            followWithCamera(markers);


            // Update polyline
            if(currentPolyline != null){
                currentPolyline.remove();
            }
            currentPolyline = addPolylineToMap(map, LocationCache.getInstance().getPassiveCache());
        } else {
            GeneralHelper.showToast(parentActivity, "No map object initialized.");
        }
        timestampPreviousMarker = loc.getTimestampInMillis();
    }

    private void updateCurrentLocationMarker(GoogleMap map, Loc loc){
        if(currentLocation == null){
            currentLocation = map.addMarker(new MarkerOptions()
                    .position(loc.getLatLng())
                    .draggable(false)
                            //.icon(BitmapDescriptorFactory.fromAsset("markers/marker1.png"))
                    .icon(BitmapDescriptorFactory.defaultMarker(0))
                    .title("Current location"))
                    ;
        } else {
            animateMarker(currentLocation, loc.getLatLng());
        }
    }

    // Following snippet allows marker animations from start to end location.
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
