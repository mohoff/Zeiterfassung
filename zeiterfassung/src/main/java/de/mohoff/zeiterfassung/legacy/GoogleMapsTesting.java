package de.mohoff.zeiterfassung.legacy;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import de.mohoff.zeiterfassung.R;


public class GoogleMapsTesting extends ActionBarActivity {

    private GoogleMap map;
    private Marker markerIBM;


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            /*if (map != null) {
                // The Map is verified. It is now safe to manipulate the map.
            }*/
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpMapIfNeeded();


        // MAP SETTINGS
        map.setBuildingsEnabled(true);     // big cities have 3D tilt, true by default
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN); // MAP_TYPE_NORMAL, MAP_TYPE_NONE, MAP_TYPE_HYBRID, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN
        map.setIndoorEnabled(true); // shows indoor maps of buildings when zoom-level is high enough, default: true
        map.setPadding(50, 50, 50, 50); // sets padding (left, top, right, bottom) for user controls

        UiSettings uis = map.getUiSettings();
        uis.setZoomControlsEnabled(true);
        uis.setZoomGesturesEnabled(true);
        uis.setScrollGesturesEnabled(true);
        uis.setTiltGesturesEnabled(true);
        uis.setRotateGesturesEnabled(true);
        uis.setCompassEnabled(true);
        uis.setMyLocationButtonEnabled(true);
        uis.setIndoorLevelPickerEnabled(true);


        // MARKERS
        LatLng bbLatLng = new LatLng(48.665332, 9.037333);
        markerIBM = map.addMarker(new MarkerOptions()
                .position(bbLatLng)     // marker position in LatLng
                .draggable(false)       // draggable boolean
                .title("ibm böblingen")     // title (display when marker clicked/touched
                .snippet(bbLatLng.latitude + ", " + bbLatLng.longitude)     // text below title
                .alpha(0.9f)    // opacity
                .flat(false)     // flatten marker boolean (true: markers rotates with map; false:
                .anchor(0.5f, 0.5f)     // anchor ([0-1],[0-1]) set point which represents positionLatLng
                .rotation(90.0f)    // clockwise marker rotation in degrees
                        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow) // custom icon as marker image
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))      // predefined marker style, parameter [0, 360] representing color wheel
        );
        markerIBM.showInfoWindow(); // shows info window by default


        // CAMERA CONTROL
        CameraUpdate cu;
        cu = CameraUpdateFactory.zoomIn();
        cu = CameraUpdateFactory.zoomOut();
        cu = CameraUpdateFactory.zoomTo(5); // zooms to passed zoom-level
        cu = CameraUpdateFactory.zoomBy(-3); // zooms X zoom-level(s) apart the current zoom-level (parameter is offset)
        cu = CameraUpdateFactory.newLatLng(new LatLng(48,10)); // moves camera to specified LatLng
        cu = CameraUpdateFactory.newLatLngZoom(new LatLng(48,10), 15); // moves camera to specified LatLng with specified target zoom-level
        cu = CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(48,10), 15, 0, 0)); // sets map to passed camera position: (target, zoom, tilt, bearing)

        LatLngBounds latLngBounds = new LatLngBounds(new LatLng(48,10), new LatLng(48,9));
        cu = CameraUpdateFactory.newLatLngBounds(latLngBounds, 20); // sets view so that it fits passed LatLngBounds with padding (px) as 2nd paramter
        cu = CameraUpdateFactory.newLatLngBounds(new LatLngBounds(new LatLng(48,10), new LatLng(48,9)), 100, 150, 50); // like setting bounds but with custom viewport (bounds, width, height, padding)
        cu = CameraUpdateFactory.newLatLngZoom(latLngBounds.getCenter(), 20); // like setting bounds, but ignoring the extremes by calling getCenter(). 2nd parameter is zoom-level
        cu = CameraUpdateFactory.scrollBy(100f, 50f); // pans by passed pixel-offset (x, y), negative values allowed

        map.moveCamera(cu);     // moves camera instantly to passed CameraUpdate-object
        map.animateCamera(cu, 1000, null);     // moves camera smootly to passed CameraUpdate-object, within duration in ms (2nd parameter), and callback when finished. Can be null


        // SHAPES
        PolygonOptions polygonOptions = new PolygonOptions()
                .add(new LatLng(48, 10),
                        new LatLng(48.5, 10),
                        new LatLng(48.5, 10.5),
                        new LatLng(48, 10.5))
                        //.addHole(new LatLng(X, Y), ...)         // set smaller polygon for donat/hole shape
                .strokeColor(Color.RED)
                .fillColor(Color.YELLOW)
                ;
        Polygon polygon = map.addPolygon(polygonOptions);
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(new LatLng(49, 10),
                        new LatLng(49.5, 10),
                        new LatLng(49.5, 10.5),
                        new LatLng(49, 10.5))
                .width(25)
                .color(Color.BLUE)
                .geodesic(false)    // related to Mercator projection and sphere assumtion, default is false
                .visible(true)
                .zIndex(40)
        ;
        Polyline polyline = map.addPolyline(polylineOptions);
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(48, 9.8))    // center of circle as LatLng-object
                .radius(1000000);               // radius in meter
        Circle circle = map.addCircle(circleOptions);

        // altering existing shapes
        //polyline.setPoints();     // alters an existing polyline, pass list of LatLng-points
        //polygon.setPoints();
        //circle.setCenter();
        //circle.setRadius();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void onMarkerClick(final Marker marker) {
        System.out.println("marker click!");
        if (marker.equals(markerIBM))
        {
            //handle click here
            markerIBM.hideInfoWindow();
        }
    }

}
