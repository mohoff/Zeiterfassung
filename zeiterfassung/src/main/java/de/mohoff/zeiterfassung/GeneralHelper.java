package de.mohoff.zeiterfassung;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.locationservice.LocationService;

/**
 * Created by TPPOOL01 on 26.11.2014.
 */
public class GeneralHelper {
    // SINGLETON variables
    private static GeneralHelper instance;
    private Context context;

    // LocationService variables
    private LocationServiceConnection lsc = null;
    private LocationService service;
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
        context.startService(new Intent(context, LocationService.class)); // Calling startService() first prevents it from being killed on unbind()
        lsc = new LocationServiceConnection();  // connect to it

        boolean result = context.bindService(
                new Intent(context, LocationService.class),
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
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            instance.service = (LocationService) binder.getService();
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
            service.stopService(new Intent(context, LocationService.class));
            serviceIsRunning = false;
        }
    }

    public static void showToast(Context ctx, String msg) {
        Toast toast = Toast.makeText(ctx, msg, Toast.LENGTH_LONG);
        toast.show();
    }

    public static LatLng convertLocToLatLng(Loc loc){
        return new LatLng(loc.getLatitude(), loc.getLongitude());
    }

    public static List<LatLng> convertListLocToListLatLng(List<Loc> listLoc){
        List<LatLng> listLatLng = new ArrayList<LatLng>();
        for(Loc loc : listLoc){
            listLatLng.add(convertLocToLatLng(loc));
        }
        return listLatLng;
    }

    public static float getOpacityFromAccuracy(double accuracy){
        // acc < 50m : 1        --> == opaque
        // acc 100m  : 0.8      --> == 80% opaque / 20% transparent
        // acc 200m  : 0.6      --> ...
        // acc 500m  : 0        --> == 100% transparent
        // acc 1000m : -1
        float unclampedOpacity = 1.0f - (float) ((int)accuracy/50) /10.0f;

        // clamp unclampedOpacity to interval [ 0.2 , 1 ]
        return Math.max(1.0f, Math.min(0.2f, unclampedOpacity));
    }


}
