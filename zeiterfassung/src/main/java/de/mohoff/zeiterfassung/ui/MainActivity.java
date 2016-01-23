package de.mohoff.zeiterfassung.ui;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import de.mohoff.zeiterfassung.helpers.GeneralHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.locationservice.LocationChangeListener;
import de.mohoff.zeiterfassung.locationservice.LocationService;
import de.mohoff.zeiterfassung.locationservice.LocationServiceStatus;
import de.mohoff.zeiterfassung.locationservice.TimeslotEventListener;
import de.mohoff.zeiterfassung.ui.components.about.About;
import de.mohoff.zeiterfassung.ui.components.overview.Overview;
import de.mohoff.zeiterfassung.ui.components.settings.Settings;
import de.mohoff.zeiterfassung.ui.components.statistics.Statistics;
import de.mohoff.zeiterfassung.ui.components.zones.Zones;
import de.mohoff.zeiterfassung.ui.components.NavigationDrawerListener;
import de.mohoff.zeiterfassung.ui.components.maplive.MapLive;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.intro.Intro;

// TODO: add lite version of google maps to lower area of navigation drawer and maybe 'About' page

public class MainActivity extends AppCompatActivity implements NavigationDrawerListener {
    private static final int PERMISSION_REQUEST_LOCATION = 1;


    private DatabaseHelper dbHelper;

    public static FragmentManager fragM;
    FragmentTransaction fragT;
    private Fragment nextFragment;

    public CoordinatorLayout coordinatorLayout;
    public NavigationView navigationView;
    public View navigationViewHeader;
    public Toolbar toolbar;
    public FloatingActionButton fab;
    private NavigationDrawerListener drawerListener;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private Button buttonStartService;
    private Button buttonStopService;

    public LocationService mService;
    private LocationServiceConnection serviceCon = null;
    private MainActivity that = this;
    public LocationServiceStatus serviceStatus;

    // listener for MapLive fragment
    // When fragment is active, it can update its map on new locations instantly
    private LocationChangeListener newLocationListener;
    private TimeslotEventListener newTimeslotEventListener;

    public void setOnNewLocationListener(LocationChangeListener listen) {
        newLocationListener = listen;
    }
    public void setOnTimeslotEventListener(TimeslotEventListener listen) {
        newTimeslotEventListener = listen;
    }
    public void removeOnNewLocationListener() {
        newLocationListener = null;
    }
    public void removeOnTimeslotEventListener() {
        newTimeslotEventListener = null;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main);
        getDbHelper();

        // Show Intro Activity when app is started the first time
        // Declare a new thread to do a preference check
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);
                if(isFirstStart){
                    showAppIntro();
                    //  Edit preference to make it false because we don't want this to run again
                    SharedPreferences.Editor e = getPrefs.edit();
                    e.putBoolean("firstStart", false);
                    e.apply();
                }
            }
        });
        t.start();

        fragM = getFragmentManager();
        fragT = fragM.beginTransaction();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        navigationView = ((NavigationView) findViewById(R.id.navigationView));
        navigationViewHeader = navigationView.getHeaderView(0);
        buttonStartService = (Button) navigationViewHeader.findViewById(R.id.buttonStartService);
        buttonStopService = (Button) navigationViewHeader.findViewById(R.id.buttonStopService);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            //title = actionBar.getTitle();
        }

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);
                selectedDrawerItem(menuItem);
                return true;
            }
        });
        // Select the first menu item at app start. Don't select first when savedInstanceState is
        // not null (e.g. when screen is rotated).
        if(savedInstanceState == null) {
            navigationView.getMenu().getItem(0).setChecked(true);
            navigationView.getMenu().performIdentifierAction(R.id.item_overview, 0);
        }

        drawerToggle = new ActionBarDrawerToggle(
                this,                  // host Activity
                drawerLayout,          // DrawerLayout object
                R.string.drawer_open,  // "open drawer" description for accessibility
                R.string.drawer_close  // "close drawer" description for accessibility
        ) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getSupportActionBar().setTitle(title);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                //if(nextFragmentAvailable){
                //    replaceFragment(nextFragment, false);
                //    nextFragmentAvailable = false;
                //}
            }
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                //toolbar.setAlpha(1 - slideOffset / 2);
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerToggle.syncState();
            }
        });

        // Initializes LocationCache (singleton) and fills it with locations from DB if the
        // locations are not too old.
        GeneralHelper.setupLocationCache(dbHelper);

        // Initialize LocationServiceStatus and sync its state with the LocationService
        serviceStatus = new LocationServiceStatus();
        serviceStatus.setIsRunning(LocationService.IS_SERVICE_RUNNING);

        updateServiceButtons();

        LocalBroadcastManager.getInstance(this).registerReceiver(timeslotEventReceiver,
                new IntentFilter("locationServiceTimeslotEvents"));
        LocalBroadcastManager.getInstance(this).registerReceiver(locUpdateEventReceiver,
                new IntentFilter("locationServiceLocUpdateEvents"));
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceEventReceiver,
                new IntentFilter("serviceEventUpdate"));

        // Load preferences' default values into SharedPreference.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    public void showAppIntro(){
        Intent i = new Intent(MainActivity.this, Intro.class);
        startActivity(i);
    }

    public void startAndBindToLocationService() {
        boolean bindResult;
        // Calling startService() first prevents it from being killed on unbind()
        if(startService(new Intent(MainActivity.this, LocationService.class)) != null){
            serviceStatus.setIsRunning(true);
            //isServiceRunning = true;
            updateServiceButtons();

            bindResult = bindLocationService();
        } else {
            throw new RuntimeException("Unable to start service.");
        }
        if(!bindResult){
            throw new RuntimeException("Unable to start service.");
        }
    }

    public void unbindAndStopService(){
        // The order of following executed lines is debatable. I value UI
        // responsiveness over waiting for stopService call. Since stopService
        // does not provide any feedback about its success, I prefer UI feedback
        // first.

        serviceStatus.setIsRunning(false);
        updateServiceButtons();
        unbindLocationService();
        stopService(new Intent(MainActivity.this, LocationService.class));
    }

    private boolean bindLocationService(){
        // It's ok when service is already bound: Will return TRUE
        serviceCon = new LocationServiceConnection();
        return bindService(
                new Intent(this, LocationService.class),
                serviceCon,
                BIND_AUTO_CREATE
        );
    }

    public void unbindLocationService(){
        // It's ok when service is already unbound
        if(serviceCon != null){
            unbindService(serviceCon);
            serviceCon = null;
        }
    }

    protected class LocationServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            //LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            //refThis.service = (LocationService) binder.getService();
            LocationService.LocalBinder localBinder = (LocationService.LocalBinder)service;
            mService = (LocationService) localBinder.getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //service = null;
        }
    }

    @Override
    public void StartButtonClicked() {
        requestPermissionsAndStartService();
    }

    @Override
    public void StopButtonClicked() {
        unbindAndStopService();
    }

    public void requestPermissionsAndStartService(){
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied
            // it. If so, we want to give more explanation about why the permission is needed.
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                // Show our own UI to explain to the user why we need to read the contacts
                // before actually requesting the permission and showing the default UI
                Snackbar.make(
                        coordinatorLayout,
                        "Camera access is required to display the camera preview.",
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction("GRANT", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Request the permission
                                ActivityCompat.requestPermissions(
                                        MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        PERMISSION_REQUEST_LOCATION
                                );
                            }
                        })
                .show();
            }

            // Fire off an async request to actually get the permission
            // This will show the standard permission request dialog UI
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION
            );
        } else {
            // When permissions have already been granted previously, start the LocationService.
            startAndBindToLocationService();
        }
    }

    // Callback with the request from calling requestPermissions(...)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(
                            coordinatorLayout,
                            "Access Locations permission granted.",
                            Snackbar.LENGTH_LONG)
                            .show();
                    // Since we have needed permissions now, we can start the LocationService
                    startAndBindToLocationService();
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(this)
                            .setPositiveButton("SETTINGS", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    // Open relevant system settings
                                    Intent intent = new Intent();
                                    intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                }
                            })
                            .setTitle("Important Note")
                            .setMessage("In order to track your movements, this app needs Location permissions. Since you once chose to deny them without getting asked again, you now need to grant Location permissions manually. To do so, click SETTINGS in this dialog and then activate Location under 'Permissions'.")
                            .create();
                    alertDialog.show();
                }
                return;
            }
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void onItemSelected(View view, int position) {
    }

    private void updateServiceButtons(){
        if(serviceStatus.isRunning()){
            // manage start button
            buttonStartService.getBackground().setColorFilter(getResources().getColor(R.color.grey_25), PorterDuff.Mode.SRC_ATOP);
            buttonStartService.setEnabled(false);
            buttonStartService.setTextColor(getResources().getColor(R.color.white)); // need to setIsRunning text color explicitly after setEnabled(false). Else text color gets grey somehow
            buttonStartService.setOnClickListener(null);
            // manage stop button
            buttonStopService.getBackground().setColorFilter(getResources().getColor(R.color.greenish), PorterDuff.Mode.SRC_ATOP);
            buttonStopService.setEnabled(true);
            buttonStopService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // stop secondLine service
                    StopButtonClicked();
                    //if (navDrawerListener != null) {
                    //  navDrawerListener.StopButtonClicked();
                    //}
                }
            });
        } else {
            // manage start button
            buttonStartService.getBackground().setColorFilter(getResources().getColor(R.color.greenish), PorterDuff.Mode.SRC_ATOP);
            buttonStartService.setEnabled(true);
            buttonStartService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // start and connect to secondLine service
                    StartButtonClicked();
                    //if (navDrawerListener != null) {
                    //    navDrawerListener.StartButtonClicked();
                    //}
                }
            });
            // manage stop button
            buttonStopService.getBackground().setColorFilter(getResources().getColor(R.color.grey_25), PorterDuff.Mode.SRC_ATOP);
            buttonStopService.setEnabled(false);
            buttonStopService.setTextColor(getResources().getColor(R.color.white)); // need to setIsRunning text color explicitly after setEnabled(false). Else text color gets grey somehow
            buttonStopService.setOnClickListener(null);
        }
    }

    public void selectedDrawerItem(MenuItem item){
        item.setChecked(true);
        Fragment next = null;

        switch (item.getItemId()) {
            case R.id.item_overview:
                next = new Overview();
                break;
            case R.id.item_zones:
                next = new Zones();
                break;
            case R.id.item_maplive:
                next = new MapLive();
                break;
            case R.id.item_statistics:
                next = new Statistics();
                break;
            case R.id.item_settings:
                next = new Settings();
                break;
            case R.id.item_about:
                next = new About();
                break;
            /*default:
                next = new Overview();*/
        }

        if(next != null){
            replaceFragment(next, false);
            setTitle(item.getTitle());
            drawerLayout.closeDrawers();
        }
    }
    /* // onOptionsItemSelected when you don't use an ActionBarDrawerToggle (here: drawerToggle as variable)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onBackPressed() {
        GeneralHelper.hideSoftKeyboard(this);

        // Close navigation drawer if it's open. If not, go back to previous fragment if there is one
        // on the back-stack.
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawers();
        } else if (getFragmentManager().getBackStackEntryCount() > 0){
            getFragmentManager().popBackStack();
            if(getFragmentManager().getBackStackEntryCount() > 0){
                drawerToggle.setDrawerIndicatorEnabled(false);
            } else {
                drawerToggle.setDrawerIndicatorEnabled(true);
            }

        } else {
            //drawerToggle.setDrawerIndicatorEnabled(true);
            super.onBackPressed();
        }
    }

    public void replaceFragment(Fragment fragment, boolean addToBackStack){
        // TODO: Investigate why .show() doesn't work in 'Manage Zones'
        //fab.hide();
        String backStateName = fragment.getClass().getName();
        String fragmentTag = backStateName;

        FragmentManager manager = getFragmentManager();
        boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

        if (!fragmentPopped && manager.findFragmentByTag(fragmentTag) == null && fragment != null) { //fragment not in back stack, create it.
            FragmentTransaction ft = manager.beginTransaction();
            ft.replace(R.id.content_frame, fragment, fragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            if(addToBackStack) {
                ft.addToBackStack(backStateName);
            }
            //ft.commit();
            ft.commitAllowingStateLoss();
            // new
            //manager.executePendingTransactions();
        }
    }

    public ActionBarDrawerToggle getDrawerToggle(){
        return drawerToggle;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        // TODO: add releaseHelper (following lines) also to all other fragments which use dbHelper
        if (dbHelper != null) {
            OpenHelperManager.releaseHelper();
            dbHelper = null;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(timeslotEventReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locUpdateEventReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceEventReceiver);

        unbindLocationService();
        super.onDestroy();
    }

    // BroadcastReceiver, which receives Events from LocationService, such as "newTimeslotStarted" as message
    private BroadcastReceiver timeslotEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String receivedMessage = intent.getStringExtra("type");
            if(receivedMessage.equals("opened")){
                Snackbar.make(that.coordinatorLayout, getString(R.string.timeslot_opened), Snackbar.LENGTH_LONG)
                        .show();
            } else if(receivedMessage.equals("closed")){
                Snackbar.make(that.coordinatorLayout, getString(R.string.timeslot_closed), Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    };
    private BroadcastReceiver locUpdateEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            double lat = Double.valueOf(intent.getStringExtra("lat"));
            double lng = Double.valueOf(intent.getStringExtra("lng"));
            double accuracy = Double.valueOf(intent.getStringExtra("accuracy"));
            boolean isRealUpdate = intent.getBooleanExtra("isRealUpdate", true);
            Loc newLocation = new Loc(lat, lng, accuracy);
            newLocation.setIsRealUpdate(isRealUpdate);

            if (newLocationListener != null) {
                newLocationListener.onNewLocation(newLocation);
            }
        }
    };
    private BroadcastReceiver serviceEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String receivedMessage = intent.getStringExtra("type");
            if(receivedMessage.equals("start")){
                serviceStatus.setIsRunning(true);
            } else if(receivedMessage.equals("stop")){
                serviceStatus.setIsRunning(false);
            }
        }
    };

    @Override
    protected void onResume() {
        //GeneralHelper.clearBackStack(this);
        //drawerToggle.setDrawerIndicatorEnabled(false);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public static interface ClickListener {
        public void onClick(View view, int position);
        public void onLongClick(View view, int position);
    }

    static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {
        private GestureDetector gestureDetector;
        private ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                    }
                }
            });
        }
        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child));
            }
            return false;
        }
        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }
        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    }
}
