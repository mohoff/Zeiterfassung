package de.mohoff.zeiterfassung;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;


public class MainActivity extends ActionBarActivity {

    //MapView mapView = (MapView) this.findViewById(R.id.mapview);
    //UserLocationOverlay userLocationOverlay = new UserLocationOverlay(this, mapView);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //MapView mapView = new MapView(this, "moson.hn35hbji");
        //this.setContentView(mapView);

        //userLocationOverlay.enableMyLocation();
        //userLocationOverlay.setDrawAccuracyEnabled(true);
        //mapView.getOverlays().add(userLocationOverlay);

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
}
