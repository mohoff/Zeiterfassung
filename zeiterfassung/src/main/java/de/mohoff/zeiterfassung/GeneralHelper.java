package de.mohoff.zeiterfassung;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

import de.mohoff.zeiterfassung.locationservice.LocationServiceNewAPI;

/**
 * Created by TPPOOL01 on 26.11.2014.
 */
public class GeneralHelper {
    // SINGLETON variables
    private static GeneralHelper instance;
    private Context context;

    // LocationService variables
    private LocationServiceConnection lsc = null;
    private LocationServiceNewAPI service;
    private boolean serviceIsRunning = false;

    // SINGLETON
    public static GeneralHelper getInitialInstance(Context c) {
        if (instance == null) {
            instance = new GeneralHelper(c);
        }
        return instance;
    }
    public static GeneralHelper getInstance(){
        if (instance == null) {
            instance = new GeneralHelper();
        }
        return instance;
    }
    private GeneralHelper(Context c){
        this.context = c;
    }
    private GeneralHelper(){
        super();
    }


    // HELPER METHODS
    // --------------

    // LocationService Methods
    public void startAndConnectToLocationService() {
        context.startService(new Intent(context, LocationServiceNewAPI.class)); // Calling startService() first prevents it from being killed on unbind()
        lsc = new LocationServiceConnection();  // connect to it

        boolean result = context.bindService(
                new Intent(context, LocationServiceNewAPI.class),
                lsc,
                Context.BIND_AUTO_CREATE
        );

        if(!result){
            throw new RuntimeException("Unable to bind with service.");
        }
        serviceIsRunning = true;
    }
    protected class LocationServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationServiceNewAPI.LocalBinder binder = (LocationServiceNewAPI.LocalBinder) service;
            instance.service = (LocationServiceNewAPI) binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            instance.service = null;
        }
    }
    public void unbindLocationService(){
        if(lsc != null && serviceIsRunning){
            context.unbindService(lsc);
            lsc = null;
        }
    }
    public void stopLocationService(){
        if(serviceIsRunning){
            this.unbindLocationService();
            service.stopService(new Intent(context, LocationServiceNewAPI.class));
            serviceIsRunning = false;
        }
    }

    public static void showToast(Context ctx, String msg) {
        Toast toast = Toast.makeText(ctx, msg, Toast.LENGTH_LONG);
        toast.show();
    }


}
