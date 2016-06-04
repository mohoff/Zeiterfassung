package de.mohoff.zeiterfassung.ui.components;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;
import de.mohoff.zeiterfassung.ui.components.settings.Settings;

/**
 * Created by moo on 8/16/15.
 */
public class MapAbstract extends Fragment implements OnMapReadyCallback {
    protected static boolean SHOW_MAP_ANIMATIONS;
    protected static int DEFAULT_ZOOM_LEVEL = 17;
    public static int MIN_ZOOM_LEVEL = 1;
    public static int MAX_ZOOM_LEVEL = 21;
    public static int ZOOM_LEVEL_OFFSET_FOR_PREFS = 6;
    private static int MARKER_WIDTH = 50; // px
    private static int MARKER_HEIGHT = 79; // px
    private static int MARKER_DOT_DIM = MARKER_WIDTH; // px
    private static int MARKER_VIEWPORT_PADDING = 200; // px

    protected MainActivity context;
    protected static View view;
    protected ProgressBar progressBar;

    protected MapFragment mapFragment;
    protected GoogleMap map;
    protected Geocoder geocoder;

    protected SharedPreferences sp;
    protected DatabaseHelper dbHelper = null;

    protected ArrayList<Marker> fixMarkers = new ArrayList<Marker>();
    protected ArrayList<Circle> fixCircles = new ArrayList<Circle>();

    // Bitmaps for Marker icons in 3 different variants
    protected Bitmap markerAccurate, markerInaccurate, markerNoConnection;
    // Bitmap for Markers which will be displayed with a number on them.
    // While generating this Bitmap, the number will be printed on of the 3 templates above.
    protected Bitmap markerWithNumbers;
    // Bitmap for Marker which shows most recent user secondLine on top of
    // markerAccurate||markerInaccurate||markerNoConnection
    protected Bitmap markerCurrentLocation;
    // Bitmap for Markers which show center of a Zone area.
    protected Bitmap markerFixLocation, markerCandidateLocation;

    protected MarkerOptions optionsFixMarker, optionsCandidateMarker;
    protected CircleOptions optionsFixCircle, optionsCandidateCircle;
    protected int colorFixLocationCircle, colorCandidateLocationMarker;
    protected int colorFixLocationMarker, colorCandidateLocationCircle;


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

        //mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // TODO: This block was commented out before Jan07-2016 ... needed?
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.fragment_map, container, false);
        } catch (InflateException e) {
            // map is already there, just return view as it is
        }

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        //mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = (MainActivity) getActivity();
        geocoder = new Geocoder(getActivity());
        dbHelper = getDbHelper(context);
        sp = PreferenceManager.getDefaultSharedPreferences(context);

        // Prevents a NullPointerException with BitmapDescriptorFactory
        // see http://stackoverflow.com/questions/13935725/ibitmapdescriptorfactory-is-not-initialized-error
        MapsInitializer.initialize(context.getApplicationContext());

        // Handle setting 'Default Zoom Level'
        DEFAULT_ZOOM_LEVEL = Settings.getRealZoomLevel(
                Integer.parseInt(sp.getString(
                        context.getString(R.string.setting_map_default_zoom),
                        String.valueOf(context.getString(R.string.setting_map_default_zoom_default_value))
                ))
        );
        // Handle setting 'Zoom In Animation'
        SHOW_MAP_ANIMATIONS = sp.getBoolean(
                context.getString(R.string.setting_map_zoomin),
                Boolean.valueOf(context.getString(R.string.setting_map_zoomin_default_value))
        );

        colorFixLocationMarker = context.getResources().getColor(R.color.grey_50);
        colorFixLocationCircle = context.getResources().getColor(R.color.grey_10_alpha);
        markerFixLocation = createBitmapFromDrawable(
                getTintedDrawable("markers/marker_50px.png", colorFixLocationMarker),
                MARKER_WIDTH,
                MARKER_HEIGHT,
                true);
        // TODO: Based on drawables, not on assets. Maybe works when trying to support wider range of screen sizes.
        /*markerFixLocation = createBitmapFromDrawable(
                tintDrawable(getResources().getDrawable(R.drawable.ic_launcher), colorFixLocationMarker),
                MARKER_WIDTH,
                MARKER_HEIGHT,
                true);*/
        colorCandidateLocationMarker = context.getResources().getColor(R.color.greenish_100);
        colorCandidateLocationCircle = context.getResources().getColor(R.color.greenish_50);
        markerCandidateLocation = createBitmapFromDrawable(
                getTintedDrawable("markers/marker_50px.png", colorCandidateLocationMarker),
                MARKER_WIDTH,
                MARKER_HEIGHT,
                true);
        markerAccurate = createBitmapFromDrawable(
                context.getResources().getDrawable(R.drawable.mapmarker_accurate),
                MARKER_DOT_DIM,
                MARKER_DOT_DIM,
                true);
        markerInaccurate = createBitmapFromDrawable(
                context.getResources().getDrawable(R.drawable.mapmarker_inaccurate),
                MARKER_DOT_DIM,
                MARKER_DOT_DIM,
                true);
        markerNoConnection = createBitmapFromDrawable(
                context.getResources().getDrawable(R.drawable.mapmarker_noconnection),
                MARKER_DOT_DIM,
                MARKER_DOT_DIM,
                true);
        markerCurrentLocation = createBitmapFromAsset(
                context,
                "markers/marker_50px_padding.png");
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
            // TODO: Check if we really need distinction in Android versions here...
            // Every Android OS older than Marshmallow (< 6.0)
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                MapFragment fragment = (MapFragment) (getFragmentManager().findFragmentById(R.id.map));
                FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
                ft.remove(fragment);
                //ft.commit();
                ft.commitAllowingStateLoss();
            } else {
                // TODO: Maybe <6.0 also can work with getChildFragmentManager() ?
                // For Android Marshmallow and above (>= 6.0):
                MapFragment fragment = (MapFragment) (getChildFragmentManager().findFragmentById(R.id.map));
                FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                ft.remove(fragment);
                //ft.commit();
                ft.commitAllowingStateLoss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onDestroyView();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        // Disable "Navigation" and "GPS Pointer" buttons which are visible by default
        map.getUiSettings().setMapToolbarEnabled(false);
        // Enables "Show my secondLine" button which shows current secondLine with bearing on the map
        // map.getUiSettings().setMyLocationButtonEnabled(true);

        progressBar.setVisibility(View.GONE);
    }

    // It is easier to implement this method in non-abstract classes
    /*protected void addMarkerToMap(GoogleMap map, List<Marker> markers, LatLng latLng, float opacity, String title, String snippet, boolean isRealUpdate){
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
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL));
            } else {
                // Zoom map in to marker, if the marker is the first one on the map.
                // Zoom level 17 turns out to be nice because its the lowest one in which you can
                // see building outlines.
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL));
            }
        } else {
            // No list "markers" is passed, so we are in "Add new Zone".
            // Just move camera towards desired position without resetting the zoom level every time.
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }*/

    protected void centerMapTo(LatLng cameraCenter, int zoomLevel, boolean showAnimation) {
        if (zoomLevel < MIN_ZOOM_LEVEL || zoomLevel > MAX_ZOOM_LEVEL) {
            zoomLevel = DEFAULT_ZOOM_LEVEL;
        }
        centerMapTo(CameraUpdateFactory.newLatLngZoom(cameraCenter, zoomLevel), showAnimation);
    }

    protected void centerMapTo(CameraUpdate cu, boolean showAnimation) {
        if (showAnimation) {
            map.animateCamera(cu);
        } else {
            map.moveCamera(cu);
        }
    }

    /*protected MarkerOptions createMarkerOptions(boolean isDraggable, String title, String snippet, BitmapDescriptor bitmapDescriptor){
        // without position
        return new MarkerOptions()
                .draggable(isDraggable)
                .title(title)
                .snippet(snippet)
                .colorBarIcon(bitmapDescriptor);
    }*/

    protected CircleOptions createCircleOptions(int fillColor) {
        // without position and radius
        return new CircleOptions()
                .fillColor(fillColor)
                .strokeWidth(0)
                .strokeColor(Color.TRANSPARENT);
    }

    protected void drawExistingZones() {
        // Initialize MarkerOptions
        optionsFixMarker = new MarkerOptions()
                .draggable(false)
                .icon(BitmapDescriptorFactory.fromBitmap(markerFixLocation));
        optionsFixCircle = createCircleOptions(colorFixLocationCircle);

        optionsCandidateMarker = new MarkerOptions()
                .draggable(true)
                .icon(BitmapDescriptorFactory.fromBitmap(markerCandidateLocation));
        optionsCandidateCircle = createCircleOptions(colorCandidateLocationCircle);
    }


    public static CameraUpdate getMapViewport(ArrayList<LatLng> latLngList) {
        LatLngBounds.Builder boundBilder = LatLngBounds.builder();
        for (LatLng latLng : latLngList) {
            boundBilder.include(latLng);
        }
        LatLngBounds bounds = boundBilder.build();

        return CameraUpdateFactory.newLatLngBounds(bounds, MARKER_VIEWPORT_PADDING);
    }

    // Can be used directly without other method calls when icon doesn't need to be tinted.
    public static Bitmap createBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();
        InputStream istr;
        Bitmap bitmap = null;

        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
            //istr.close(); // TODO: Should we close input stream here? Stackoverflow snippet was without close().
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;
    }

    protected Drawable getTintedDrawable(String filePath, int tintColor) {
        Drawable d = null;
        try {
            d = Drawable.createFromStream(context.getAssets().open(filePath), null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return tintDrawable(d, tintColor);
    }

    protected Drawable tintDrawable(Drawable d, int tintColor) {
        if (tintColor != 0 && d != null) {
            d.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        }
        return d;
    }

    protected Bitmap createBitmapFromDrawable(Drawable d, int width, int height, boolean fullOpacity) {
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(b);
        d.setAlpha(fullOpacity ? 255 : 150);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(canvas);
        return b;
    }

    protected Bitmap addTextToBitmap(Bitmap b, String text) {
        if (text == null) text = "";

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

    protected DatabaseHelper getDbHelper(Context context) {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }


}


