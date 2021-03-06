package de.mohoff.zeiterfassung.helpers;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
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

    public static GeneralHelper getInstance() {
        if (instance == null) {
            instance = new GeneralHelper();
        }
        return instance;
    }

    private GeneralHelper(Context c) {
        this.context = c;
    }

    private GeneralHelper() {
        super();
    }


    // HELPER METHODS
    // --------------

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

    public void unbindLocationService() {
        if (lsc != null && serviceIsRunning) {
            context.unbindService(lsc);
            lsc = null;
        }
    }

    public void stopLocationService() {
        if (serviceIsRunning) {
            this.unbindLocationService();
            service.stopService(new Intent(context, LocationService.class));
            serviceIsRunning = false;
        }
    }

    /*public static void showToast(Context ctx, String msg) {
        Toast toast = Toast.makeText(ctx, msg, Toast.LENGTH_LONG);
        toast.show();
    }*/

    public static LatLng convertLocToLatLng(Loc loc) {
        return new LatLng(loc.getLatitude(), loc.getLongitude());
    }

    public static List<LatLng> convertListLocToListLatLng(List<Loc> listLoc) {
        List<LatLng> listLatLng = new ArrayList<LatLng>();
        for (Loc loc : listLoc) {
            listLatLng.add(convertLocToLatLng(loc));
        }
        return listLatLng;
    }

    public static float getOpacityFromAccuracy(double accuracy) {
        // acc < 50m : 1        --> == opaque
        // acc 100m  : 0.8      --> == 80% opaque / 20% transparent
        // acc 200m  : 0.6      --> ...
        // acc 500m  : 0        --> == 100% transparent
        // acc 1000m : -1
        float unclampedOpacity = 1.0f - (float) ((int) accuracy / 50) / 10.0f;

        // clamp unclampedOpacity to interval [ 0.2 , 1 ]
        return Math.min(1.0f, Math.max(0.2f, unclampedOpacity));
    }

    public static void hideSoftKeyboard(Activity activity) {
        View v = activity.getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public static void hideSoftKeyboardWithView(Activity activity, View v) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static void clearBackStack(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack();
        }
    }

    public static void setupLocationCache(DatabaseHelper dbHelper) {
        List<Loc> locs = dbHelper.getLocs(System.currentTimeMillis() - LocationService.REGULAR_UPDATE_INTERVAL * LocationService.PASSIVE_CACHE_SIZE);
        // Bring List into CircularFifoQueue
        CircularFifoQueue<Loc> tmp = new CircularFifoQueue<>(LocationService.PASSIVE_CACHE_SIZE);
        tmp.addAll(locs);
        LocationCache.getInstance().setPassiveCache(tmp);
    }

    public static View getAlertDialogEditTextContainer(Context context, EditText et, String placeholder) {
        et.setText(placeholder);
        et.setSingleLine(true);

        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        et.setLayoutParams(params);
        container.addView(et);
        return container;
    }

    public static boolean isLocationServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("LocationService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
