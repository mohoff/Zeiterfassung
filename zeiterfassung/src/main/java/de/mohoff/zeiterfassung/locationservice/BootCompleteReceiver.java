package de.mohoff.zeiterfassung.locationservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import de.mohoff.zeiterfassung.R;

public class BootCompleteReceiver extends BroadcastReceiver {
    static private boolean RESTART_SERVICE_ON_BOOT;

    public BootCompleteReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean startService = sp.getBoolean(
                context.getString(R.string.setting_general_restart_service_on_boot),
                Boolean.valueOf(context.getString(R.string.setting_general_restart_service_on_boot_default_value))
        );

        if(startService){
            Intent service = new Intent(context, LocationService.class);
            context.startService(service);
        }
    }
}
