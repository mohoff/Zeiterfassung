package de.mohoff.zeiterfassung.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.locationservice.LocationChangeListener;
import de.mohoff.zeiterfassung.locationservice.LocationServiceNewAPI;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationDrawerListener;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationListAdapter;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationListItem;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationListItemLabel;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationListItemLabelService;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationListItemSection;
import de.mohoff.zeiterfassung.ui.fragments.*;
import de.mohoff.zeiterfassung.ui.fragments.Map;
import de.mohoff.zeiterfassung.legacy.LocationUpdateHandler;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements NavigationDrawerListener {
    private static int LOC_QUEUE_SIZE = 50; // --> set relative to update interval time so max markers on map =~ 2h for example
    SimpleDateFormat sdf;
    public static FragmentManager fragM;
    FragmentTransaction fragT;
    private Fragment nextFragment;
    private boolean nextFragmentAvailable = false;

    public CircularFifoQueue<Loc> getLocs() {
        return locs;
    }

    private CircularFifoQueue<Loc> locs = new CircularFifoQueue<Loc>(MainActivity.LOC_QUEUE_SIZE);
    private ArrayList<Loc> locsTmp = new ArrayList<Loc>();
    //private ArrayList<Loc> locs = new ArrayList<Loc>();

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerActionBarToggle;
    private NavigationListItem[] items = new NavigationListItem[8];
    private CharSequence title;

    private Button goToMap;
    private Button addNewTLA;
    private Button stopLocationService;
    private Button manageTLAs;
    private TextView outputTV;
    private LocationUpdateHandler luh;
    private DatabaseHelper dbHelper = null;

    private LocationServiceNewAPI service;
    private LocationServiceConnection lsc = null;
    private MainActivity refThis = this;

    // listener for Map fragment
    // When fragment is active, it can update its map on new locations instantly
    private LocationChangeListener newLocationListener;

    public void setOnNewLocationListener(LocationChangeListener listen) {
        newLocationListener = listen;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.navigation_drawer_main);
        getDbHelper();
        fragM = getFragmentManager();
        fragT = fragM.beginTransaction();


        title = getSupportActionBar().getTitle();
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#025167")));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        this.items[0] = NavigationListItemSection.create(1, "ALL");
        this.items[1] = NavigationListItemLabel.create(2, "Overview", "R.drawable.ic_overview", true, this);
        this.items[2] = NavigationListItemLabel.create(3, "Manage TLAs", "R.drawable.ic_location", true, this);
        this.items[3] = NavigationListItemSection.create(4, "DEBUG");
        this.items[4] = NavigationListItemLabel.create(5, "Map", "R.drawable.ic_debug", true, this);
        this.items[5] = NavigationListItemLabelService.create(6, "Location Service", "drawable/ic_action_edit_location", this, false);
        //this.items[5] = NavigationListItemLabel.create(6, "Start LocationService", "R.drawable.ic_service_start", false, this);
        //this.items[6] = NavigationListItemLabel.create(7, "Stop LocationService", "R.drawable.ic_service_stop", false, this);
        this.items[6] = NavigationListItemSection.create(7, "MISC");
        this.items[7] = NavigationListItemLabel.create(8, "About", "drawable/ic_action_about", true, this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        /*drawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.navigation_drawer_item, titles));*/

        NavigationListAdapter navListAdapter = new NavigationListAdapter(this, R.layout.navigation_drawer_list_label, items);
        navListAdapter.setTheListener(this);
        drawerList.setAdapter(navListAdapter);


        // Set the list's click listener
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerActionBarToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,          /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                if(nextFragmentAvailable){
                    replaceFragment(nextFragment);
                    nextFragmentAvailable = false;
                }
            }

            public void onDrawerOpened(View drawerView) {
                //getSupportActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        drawerLayout.setDrawerListener(drawerActionBarToggle);

        selectItem(1);
        if (savedInstanceState == null) {
            //selectItem(1);
        } else {
            // restore location marker data after screen rotation
            if (savedInstanceState.containsKey("locs")) {
                locsTmp = savedInstanceState.getParcelableArrayList("locs");
                for (Loc e : locsTmp) {
                    locs.add(e); // --> MapFragment retrieves locs on onResume
                }
            }
        }


        /*// START SERVICE
        Intent i = new Intent(this, LocationService.class);
        // potentially add data to the intent
        //i.putExtra("KEY1", "Value to be used by the service");
        this.startService(i);
        */

        sdf = new SimpleDateFormat("dd.MM.yyyy - HH:mm");
        /*outputTV = (TextView) findViewById(R.id.textView2);

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
                service.stopService(new Intent(MainActivity.this, LocationServiceNewAPI.class));
            }
        });
        manageTLAs.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, ManageTLA.class);
                startActivity(intent);
                finish();
            }
        });

        */

        //startAndConnectToLocationService();
    }

    @Override
    public void StartButtonClicked() {
        startAndConnectToLocationService();
    }

    @Override
    public void StopButtonClicked() {
        stopLocationService();
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            //drawerLayout.closeDrawer(drawerList);
            /*new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    selectItem(position); // your fragment transactions go here
                }
            }, 150);*/

            // old approach: just call following line without Handler.postDelayed()
            selectItem(position);
        }
    }

    private void selectItemWithView(View v, int position){
        TextView label = (TextView)v.findViewById(R.id.navigationListLabelText);
        label.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /*/public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        Fragment fragment = new MyFragment1();
        FragmentManager fragmentManager = getFragmentManager();
        switch(position) {
            case 0:
                fragment = new MyFragment1();
                break;
            case 1:
                fragment = new MyFragment2();
                break;
        }
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }*/

    private void selectItem(int position) {
        NavigationListItem selected = items[position];
        drawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerList);

        // update the main content by replacing fragments
        nextFragment = new Fragment();
        nextFragmentAvailable = true;

        // pass arguments to fragment
        /*Bundle args = new Bundle();
        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);*/

        switch(position) {
            case 1:
                nextFragment = new Overview();
                break;
            case 2:
                nextFragment = new ManageTLAs();
                break;
            case 4:
                nextFragment = new Map();
                break;
            case 7:
                nextFragment = new About();
                break;
        }

        if(selected.getType() == 1){
            //drawerList.getSelectedView().setBackgroundColor(0x11000000);
            if (selected.updateActionBarTitle()) {
                //drawerLayout.closeDrawer(drawerList);
                setTitle(selected.getLabel());
            }
        }


        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            // user selected other fragment from drawer.
            // new fragment is loaded in onDrawerClosed()
            drawerLayout.closeDrawer(drawerList);
        } else {
            // display inital fragment after app start
            replaceFragment(nextFragment);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title;
        getSupportActionBar().setTitle(title);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerActionBarToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerActionBarToggle.onConfigurationChanged(newConfig);
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




    // BroadcastReceiver, which receives Events from LocationService, such as "newTimeslotStarted" as message
    private BroadcastReceiver timeslotReceiver = new BroadcastReceiver() {
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
    private BroadcastReceiver locUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            double lat = Double.valueOf(intent.getStringExtra("lat"));
            double lng = Double.valueOf(intent.getStringExtra("lng"));
            double accuracy = Double.valueOf(intent.getStringExtra("accuracy"));
            Loc newLocation = new Loc(lat, lng, accuracy);
            locs.add(newLocation);

            if (newLocationListener != null) {
                newLocationListener.onNewLocation(newLocation);
            }
            // GEHT NOCH NICHT, mapFragment ist jedes mal NULL
            //getFragmentManager().executePendingTransactions();
            //Map mapFragment = (Map)getFragmentManager().findFragmentByTag("MAP");
            //if (mapFragment.isVisible()) {
            //    mapFragment.drawLocationUpdate(new Loc(lat, lng, accuracy));
            //}
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(timeslotReceiver,
                new IntentFilter("locationServiceTimeslotEvents"));
        LocalBroadcastManager.getInstance(this).registerReceiver(locUpdateReceiver,
                new IntentFilter("locationServiceLocUpdateEvents"));
    }
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timeslotReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locUpdateReceiver);
    }

    public void timeslotStartedEvent(long millis, String activityName, String locationName) {
        String humanReadable = sdf.format(millis);
        outputTV.append("ENTER: " + humanReadable + ",   " +locationName + " @" + activityName + "\n");
    }

    public void timeslotSealedEvent(long millis, String activityName, String locationName) {
        String humanReadable = sdf.format(millis);
        outputTV.append("QUIT: " + humanReadable + ",   " +locationName + " @" + activityName + "\n");
    }

    public void startAndConnectToLocationService() {
        startService(new Intent(MainActivity.this, LocationServiceNewAPI.class)); // Calling startService() first prevents it from being killed on unbind()
        lsc = new LocationServiceConnection();  // connect to it

        boolean result = bindService(
                new Intent(this, LocationServiceNewAPI.class),
                lsc,
                BIND_AUTO_CREATE
        );

        if(!result){
            throw new RuntimeException("Unable to bind with service.");
        }
    }

    protected class LocationServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationServiceNewAPI.LocalBinder binder = (LocationServiceNewAPI.LocalBinder) service;
            refThis.service = (LocationServiceNewAPI) binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    }
    public void unbindLocationService(){
        if(lsc != null){
            unbindService(lsc);
            lsc = null;
        }
    }
    public void stopLocationService(){
        this.unbindLocationService();
        stopService(new Intent(MainActivity.this, LocationServiceNewAPI.class));
    }


    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawerActionBarToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch(item.getItemId()) {
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // convert circularFifoQueue to ArrayList
        for(Loc e : locs) {
            locsTmp.add(e);
        }
        // add ArrayList to instance state
        outState.putParcelableArrayList("locs", locsTmp);
        super.onSaveInstanceState(outState);
    }

    private void replaceFragment(Fragment nextFragment){
        fragM.beginTransaction()
                .replace(R.id.content_frame, nextFragment)
                .commit();
    }
}
