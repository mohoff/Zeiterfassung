package de.mohoff.zeiterfassung.ui;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.locationservice.LocationChangeListener;
import de.mohoff.zeiterfassung.locationservice.LocationService;
import de.mohoff.zeiterfassung.locationservice.TimeslotEventListener;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationDrawerAdapter;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationDrawerListener;
import de.mohoff.zeiterfassung.ui.navdrawer.NavigationListItem;
import de.mohoff.zeiterfassung.ui.fragments.*;
import de.mohoff.zeiterfassung.ui.fragments.MapLive;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements NavigationDrawerListener {
    /*// In order to avoid "fragment not attached to activity" errors, some colors are preassigned.
    // Now fragments can access below color variables anytime.
    public int colorGreenish = getResources().getColor(R.color.greenish);
    public int colorGreenish50 = getResources().getColor(R.color.greenish_50);*/
    // Set relative to update interval time so max markers on map =~ 2h for example
    // NOT the same as queueSize in LocationCache.class
    private static int LOC_QUEUE_SIZE = 50;
    SimpleDateFormat sdf;
    public static FragmentManager fragM;
    FragmentTransaction fragT;
    private Fragment nextFragment;
    private boolean nextFragmentAvailable = false;

    private Toolbar toolbar;
    private RelativeLayout leftDrawer;
    private RecyclerView recyclerView;
    private NavigationDrawerListener drawerListener;

    /*public CircularFifoQueue<Loc> getLocs() {
        return locs;
    }*/
    //private CircularFifoQueue<Loc> locs = new CircularFifoQueue<>(MainActivity.LOC_QUEUE_SIZE);
    //private ArrayList<Loc> locsTmp = new ArrayList<>();

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationListItem[] items = new NavigationListItem[8];
    private CharSequence title;

    private Button buttonStartService;
    private Button buttonStopService;
    private TextView outputTV;
    private DatabaseHelper dbHelper = null;

    //private LocationService service;
    private LocationServiceConnection lsc = null;
    //private MainActivity refThis = this;
    private boolean isServiceRunning = false;

    // listener for MapLive fragment
    // When fragment is active, it can update its map on new locations instantly
    private LocationChangeListener newLocationListener;
    private TimeslotEventListener newTimeslotEventListener;

    private NavigationDrawerListener navDrawerListener;

    public void setNavigationDrawerListener(NavigationDrawerListener listen) {
        navDrawerListener = listen;
    }
    public void removeNavigationDrawerListener() {
        navDrawerListener = null;
    }
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
        setContentView(R.layout.navigation_drawer_main);
        getDbHelper();
        fragM = getFragmentManager();
        fragT = fragM.beginTransaction();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //toolbar.setTitle("Zeiterfassung");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        title = getSupportActionBar().getTitle();
        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#025167")));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        //mDrawerToggle.setDrawerIndicatorEnabled(false);
        //getActionBar().setDisplayHomeAsUpEnabled(true);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,          /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                if(nextFragmentAvailable){
                    replaceFragment(nextFragment, false);
                    nextFragmentAvailable = false;
                }
            }
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                toolbar.setAlpha(1 - slideOffset / 2);
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerToggle.syncState();
            }
        });

        leftDrawer = (RelativeLayout) findViewById(R.id.left_drawer);
        recyclerView = (RecyclerView) findViewById(R.id.drawerList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        NavigationDrawerAdapter adapter = new NavigationDrawerAdapter(this, getListItems());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                //drawerListener.onItemSelected(view, position); // leads to error...
                selectItem(position);
                NavigationDrawerAdapter.CURRENTLY_SELECTED = position;
                recyclerView.getAdapter().notifyDataSetChanged();
                drawerLayout.closeDrawer(leftDrawer);
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));


        /*
        // TODO: remove "locs" from savedInstanceState when MapLive-fragment is destroyed?!
        if(savedInstanceState != null){
            // restore location marker data after screen rotation
            if (savedInstanceState.containsKey("locs")) {
                locsTmp = savedInstanceState.getParcelableArrayList("locs");
                for (Loc e : locsTmp) {
                    locs.add(e); // --> MapFragment retrieves locs on onResume
                }
            }
        } else {
            // initial display of main fragment with id=0
            selectItem(0);
        }
        */

        // Initializes LocationCache (singleton) and fills it with locations from DB if the
        // locations are not too old.
        // TODO: Check if it also needs to be called in onCreate() of LocationService.
        GeneralHelper.setupLocationCache(dbHelper);


        // Show overview at initial app start
        if(savedInstanceState == null){
            selectItem(0);
        }

        buttonStartService = (Button) findViewById(R.id.buttonStartService);
        buttonStopService = (Button) findViewById(R.id.buttonStopService);

        if(isMyServiceRunning(LocationService.class)){
            isServiceRunning = true;
        }
        updateServiceButtons();

        LocalBroadcastManager.getInstance(this).registerReceiver(timeslotReceiver,
                new IntentFilter("locationServiceTimeslotEvents"));
        LocalBroadcastManager.getInstance(this).registerReceiver(locUpdateReceiver,
                new IntentFilter("locationServiceLocUpdateEvents"));

        sdf = new SimpleDateFormat("dd.MM.yyyy - HH:mm");

        //int result = getDbHelper().deleteAllTLAs();
        //int bla = 0;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startAndBindToLocationService() {
        boolean bindResult;
        // Calling startService() first prevents it from being killed on unbind()
        if(startService(new Intent(MainActivity.this, LocationService.class)) != null){
            isServiceRunning = true;
            updateServiceButtons();

            bindResult = bindLocationService();
        } else {
            throw new RuntimeException("Unable to start service.");
        }
        if(!bindResult){
            throw new RuntimeException("Unable to start service.");
        }
    }

    public void unbindAndStopLocationService(){
        unbindLocationService();
        /*if(stopService(new Intent(MainActivity.this, LocationService.class))){
            isServiceRunning = false;
            updateServiceButtons();
        }*/

        stopService(new Intent(MainActivity.this, LocationService.class));
        isServiceRunning = false;
        updateServiceButtons();
    }

    private boolean bindLocationService(){
        // it's ok when service is already bound: Will return TRUE
        lsc = new LocationServiceConnection();  // connect to it
        return bindService(
                new Intent(this, LocationService.class),
                lsc,
                BIND_AUTO_CREATE
        );
    }

    public void unbindLocationService(){
        // it's ok when service is already unbound
        if(lsc != null){
            unbindService(lsc);
            lsc = null;
        }
    }

    protected class LocationServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            //LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            //refThis.service = (LocationService) binder.getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //service = null;
        }
    }

    @Override
    public void StartButtonClicked() {
        startAndBindToLocationService();
    }

    @Override
    public void StopButtonClicked() {
        unbindAndStopLocationService();
    }

    private void updateServiceButtons(){
        if(isServiceRunning){
            // manage start button
            buttonStartService.getBackground().setColorFilter(getResources().getColor(R.color.grey_25), PorterDuff.Mode.MULTIPLY);
            buttonStartService.setEnabled(false);
            buttonStartService.setTextColor(getResources().getColor(R.color.white)); // need to set text color explicitly after setEnabled(false). Else text color gets grey somehow
            buttonStartService.setOnClickListener(null);
            // manage stop button
            buttonStopService.getBackground().setColorFilter(getResources().getColor(R.color.greenish), PorterDuff.Mode.MULTIPLY);
            buttonStopService.setEnabled(true);
            buttonStopService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // stop location service
                    StopButtonClicked();
                    if (navDrawerListener != null) {
                       navDrawerListener.StopButtonClicked();
                    }
                }
            });
        } else {
            // manage start button
            buttonStartService.getBackground().setColorFilter(getResources().getColor(R.color.greenish), PorterDuff.Mode.MULTIPLY);
            buttonStartService.setEnabled(true);
            buttonStartService.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // start and connect to location service
                    StartButtonClicked();
                    if (navDrawerListener != null) {
                        navDrawerListener.StartButtonClicked();
                    }
                }
            });
            // manage stop button
            buttonStopService.getBackground().setColorFilter(getResources().getColor(R.color.grey_25), PorterDuff.Mode.MULTIPLY);
            buttonStopService.setEnabled(false);
            buttonStopService.setTextColor(getResources().getColor(R.color.white)); // need to set text color explicitly after setEnabled(false). Else text color gets grey somehow
            buttonStopService.setOnClickListener(null);
        }
    }

    @Override
    public void onItemSelected(View view, int position) {
        selectItem(position);
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        nextFragment = new Fragment();
        nextFragmentAvailable = true;

        // pass arguments to fragment
        /*Bundle args = new Bundle();
        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);*/

        switch(position) {
            case 0:
                nextFragment = new Overview();
                break;
            case 1:
                nextFragment = new ManageTLAs();
                break;
            case 2:
                nextFragment = new MapLive();
                break;
            case 4:
                nextFragment = new Settings();
                break;
            case 5:
                nextFragment = new About();
                break;
        }

        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            // user selected other fragment from drawer.
            // new fragment is loaded in onDrawerClosed()
            drawerLayout.closeDrawer(leftDrawer);
        } else {
            // display inital fragment after app start
            replaceFragment(nextFragment, false);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title;
        getSupportActionBar().setTitle(title);
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
            drawerLayout.closeDrawer(leftDrawer);
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

    /*private void replaceFragment(Fragment nextFragment){
        fragM.beginTransaction()
                .replace(R.id.content_frame, nextFragment)
                .commit();
    }*/

    public void replaceFragment(Fragment fragment, boolean addToBackStack){
        String backStateName = fragment.getClass().getName();
        String fragmentTag = backStateName;

        FragmentManager manager = getFragmentManager();
        boolean fragmentPopped = manager.popBackStackImmediate(backStateName, 0);

        if (!fragmentPopped && manager.findFragmentByTag(fragmentTag) == null) { //fragment not in back stack, create it.
            FragmentTransaction ft = manager.beginTransaction();
            ft.replace(R.id.content_frame, fragment, fragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            if(addToBackStack) {
                ft.addToBackStack(backStateName);
            }
            ft.commit();
        }
    }

    public ActionBarDrawerToggle getDrawerToggle(){
        return drawerToggle;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            OpenHelperManager.releaseHelper();
            dbHelper = null;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(timeslotReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locUpdateReceiver);

        unbindLocationService();
        super.onDestroy();
    }

    // BroadcastReceiver, which receives Events from LocationService, such as "newTimeslotStarted" as message
    private BroadcastReceiver timeslotReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String receivedMessage = intent.getStringExtra("message");
            if(receivedMessage.equals("newTimeslotStarted")){
                if (newTimeslotEventListener != null) {
                    newTimeslotEventListener.onNewTimeslot(intent.getIntExtra("id", 0)); // Why need to provide default value?
                }
            } else if(receivedMessage.equals("openTimeslotSealed")){
                if (newTimeslotEventListener != null) {
                    newTimeslotEventListener.onTimeslotSealed(intent.getIntExtra("id", 0));
                }
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
            //locs.add(newLocation);

            if (newLocationListener != null) {
                newLocationListener.onNewLocation(newLocation);
            }
            // GEHT NOCH NICHT, mapFragment ist jedes mal NULL
            //getFragmentManager().executePendingTransactions();
            //MapLive mapFragment = (MapLive)getFragmentManager().findFragmentByTag("MAP");
            //if (mapFragment.isVisible()) {
            //    mapFragment.drawLocationUpdate(new Loc(lat, lng, accuracy));
            //}
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
    public boolean onCreateOptionsMenu(Menu menu) {
        //  adds items to the toolbar
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawerToggle.onOptionsItemSelected(item)) {
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
        // save locations before screen rotation in order to recover map markers after
        // convert circularFifoQueue to ArrayList
        /*
        for(Loc e : locs) {
            locsTmp.add(e);
        }
        // add ArrayList to instance state
        outState.putParcelableArrayList("locs", locsTmp);
        */
        super.onSaveInstanceState(outState);
    }

    private ArrayList<String> getListItems(){
        ArrayList<String> list = new ArrayList<>();
        /*list.add(new NavigationDrawerItem(true, "Overview"));
        list.add(new NavigationDrawerItem(true, "Manage TLAs"));
        list.add(new NavigationDrawerItem(true, "MapLive"));
        list.add(new NavigationDrawerItem(true, "Location Service"));
        list.add(new NavigationDrawerItem(true, "About"));*/
        list.add("Overview");
        list.add("Manage TLAs");
        list.add("Live Map");
        list.add("---------------"); // list[3] now hardcoded as separator (only hardcoded possible I think)
        list.add("Settings");
        list.add("About");
        return list;
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
