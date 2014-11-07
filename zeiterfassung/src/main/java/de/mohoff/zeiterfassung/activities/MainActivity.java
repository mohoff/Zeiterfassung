package de.mohoff.zeiterfassung.activities;

import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import de.mohoff.zeiterfassung.LocationService;
import de.mohoff.zeiterfassung.legacy.LocationUpdateHandler;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.legacy.TimeslotEventListener;
import de.mohoff.zeiterfassung.database.DatabaseHelper;

import java.text.SimpleDateFormat;


public class MainActivity extends ActionBarActivity {
    SimpleDateFormat sdf;

    private Button goToMap;
    private Button addNewTLA;
    private Button stopLocationService;
    private Button manageTLAs;
    private TextView outputTV;
    private LocationUpdateHandler luh;
    private DatabaseHelper dbHelper = null;

    private LocationService service;
    private LocationServiceConnection lsc = null;
    private MainActivity refThis = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getDbHelper();

        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#025167")));

        /*// START SERVICE
        Intent i = new Intent(this, LocationService.class);
        // potentially add data to the intent
        //i.putExtra("KEY1", "Value to be used by the service");
        this.startService(i);
        */


        sdf = new SimpleDateFormat("dd.MM.yyyy - HH:mm");
        outputTV = (TextView) findViewById(R.id.textView2);

        goToMap = (Button) findViewById(R.id.button);
        addNewTLA = (Button) findViewById(R.id.buttonTLA);
        stopLocationService = (Button) findViewById(R.id.buttonStopService);
        manageTLAs = (Button) findViewById(R.id.buttonManageTLA);
        goToMap.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, Map.class);
                startActivity(intent);
                finish();
            }
        });
        addNewTLA.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, AddTargetLocationArea.class);
                startActivity(intent);
                finish();
            }
        });
        stopLocationService.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                unbindLocationService();
                service.stopService(new Intent(MainActivity.this, LocationService.class));
            }
        });
        manageTLAs.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, ManageTLA.class);
                startActivity(intent);
                finish();
            }
        });


        connectToLocationService();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            OpenHelperManager.releaseHelper();
            dbHelper = null;
        }
        unbindLocationService();
    }

    private void unbindLocationService(){
        if(lsc != null){
            unbindService(lsc);
            lsc = null;
        }
    }


    // BroadcastReceiver, which receives Events from LocationService, such as "newTimeslotStarted" as message
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String receivedMessage = intent.getStringExtra("message");
            if(receivedMessage.equals("newTimeslotStarted")){
                timeslotStartedEvent(Long.valueOf(intent.getStringExtra("timestamp")), intent.getStringExtra("activityName"), intent.getStringExtra("locationName"));
            } else if(receivedMessage.equals("openTimeslotSealed")){
                timeslotSealedEvent(Long.valueOf(intent.getStringExtra("timestamp")), intent.getStringExtra("activityName"), intent.getStringExtra("locationName"));
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("locationServiceEvents"));
    }
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    public void timeslotStartedEvent(long millis, String activityName, String locationName) {
        String humanReadable = sdf.format(millis);
        outputTV.append("ENTER: " + humanReadable + ",   " +locationName + " @" + activityName + "\n");
    }

    public void timeslotSealedEvent(long millis, String activityName, String locationName) {
        String humanReadable = sdf.format(millis);
        outputTV.append("QUIT: " + humanReadable + ",   " +locationName + " @" + activityName + "\n");
    }


    private void connectToLocationService() {
        startService(new Intent(this, LocationService.class)); // Calling startService() first prevents it from being killed on unbind()
        lsc = new LocationServiceConnection();  // connect to it

        boolean result = bindService(
            new Intent(this, LocationService.class),
            lsc,
            BIND_AUTO_CREATE
        );

        if(!result){
            throw new RuntimeException("Unable to bind with service.");
        }
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }



    protected class LocationServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            MainActivity.this.service = (LocationService) binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
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

    // LEGACY
    /*
    @Override
    public void timeslotStartedEvent(TargetLocationArea tla, long timestampToPersist) {
        String humanReadable = sdf.format(timestampToPersist*1000*60);
        outputTV.append("ENTER: " + humanReadable + ",   " + tla.getLocationName() + " @" + tla.getActivityName() + "\n");
    }

    @Override
    public void timeslotFinishedEvent(TargetLocationArea tla, long timestampToPersist) {
        String humanReadable = sdf.format(timestampToPersist*1000*60);
        outputTV.append("QUIT: " + humanReadable + ",   " + tla.getLocationName() + " @" + tla.getActivityName() + "\n");
    }
    */
}
