package de.mohoff.zeiterfassung.activities;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
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

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.LocationServiceNewAPI;
import de.mohoff.zeiterfassung.NavigationDrawerListener;
import de.mohoff.zeiterfassung.NavigationListAdapter;
import de.mohoff.zeiterfassung.NavigationListItem;
import de.mohoff.zeiterfassung.NavigationListItemLabel;
import de.mohoff.zeiterfassung.NavigationListItemLabelService;
import de.mohoff.zeiterfassung.NavigationListItemSection;
import de.mohoff.zeiterfassung.legacy.LocationUpdateHandler;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;

import java.text.SimpleDateFormat;


public class MainActivity extends ActionBarActivity implements NavigationDrawerListener {
    SimpleDateFormat sdf;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.navigation_drawer);
        getDbHelper();

        title = getSupportActionBar().getTitle();
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#025167")));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        this.items[0] = NavigationListItemSection.create(1, "ALL");
        this.items[1] = NavigationListItemLabel.create(2, "Overview", "R.drawable.ic_overview", true, this);
        this.items[2] = NavigationListItemLabel.create(3, "Manage TLAs", "R.drawable.ic_location", true, this);
        this.items[3] = NavigationListItemSection.create(4, "DEBUG");
        this.items[4] = NavigationListItemLabel.create(5, "Map", "R.drawable.ic_debug", true, this);
        this.items[5] = NavigationListItemLabelService.create(6, "Location Service", "Start", "Stop", "drawable/ic_action_edit_location", this, false);
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
            }

            public void onDrawerOpened(View drawerView) {
                //getSupportActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        drawerLayout.setDrawerListener(drawerActionBarToggle);

        if (savedInstanceState == null) {
            selectItem(1);
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

        RecyclerView recList = (RecyclerView) findViewById(R.id.cardList);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        //recList.setHasFixedSize(true); // allows for optimizations
        recList.setAdapter(new CardAdapterMainActivity());
        recList.setLayoutManager(new LinearLayoutManager(this));
        recList.setItemAnimator(new DefaultItemAnimator());

        recList.setLayoutManager(llm);*/

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
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItemWithView(View v, int position){
        TextView label = (TextView)v.findViewById(R.id.navigationListLabelText);
        label.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void selectItem(int position) {
        NavigationListItem selected = items[position];
        drawerList.setItemChecked(position, true);
        if(selected.getType() == 1){
            //drawerList.getSelectedView().setBackgroundColor(0x11000000);
            if (selected.updateActionBarTitle()) {
                drawerLayout.closeDrawer(drawerList);
                setTitle(selected.getLabel());
            }

        }

        /*
        if (drawerLayout.isDrawerOpen(drawerList)) {
            drawerLayout.closeDrawer(drawerList);
        }
        */

        // update the main content by replacing fragments
        /*Fragment fragment = new PlanetFragment();
        Bundle args = new Bundle();
        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        */
        // update selected item and title, then close the drawer


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
}
