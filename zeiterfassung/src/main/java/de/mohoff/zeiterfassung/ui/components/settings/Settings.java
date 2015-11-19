package de.mohoff.zeiterfassung.ui.components.settings;


import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;

/**
 * A simple {@link Fragment} subclass.
 */
public class Settings extends PreferenceFragment {
    MainActivity context;
    DatabaseHelper dbHelper;

    Preference deleteAllTimeslots, deleteAllZones, cleanMap;

    public Settings() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        deleteAllTimeslots = findPreference(getString(R.string.setting_delete_timeslots));
        deleteAllZones = findPreference(getString(R.string.setting_delete_zones));
        cleanMap = findPreference(getString(R.string.setting_clean_map));

        deleteAllTimeslots.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setPositiveButton(getString(R.string.dialog_delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                if (dbHelper.deleteAllTimeslots() == 1) {
                                    Snackbar.make(context.coordinatorLayout, getString(R.string.settings_delete_entries_success), Snackbar.LENGTH_LONG)
                                            .show();
                                    dialog.dismiss();
                                } else {
                                    Snackbar.make(context.coordinatorLayout, getString(R.string.settings_delete_entries_failure), Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        })
                        .setMessage(getString(R.string.settings_alert_msg_delete_entries))
                        .create();
                alertDialog.show();
                return true;
            }
        });

        deleteAllZones.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setPositiveButton(getString(R.string.dialog_delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                if(dbHelper.deleteAllZones() == 1){
                                    Snackbar.make(context.coordinatorLayout, getString(R.string.settings_delete_zones_success), Snackbar.LENGTH_LONG)
                                            .show();
                                    dialog.dismiss();
                                } else {
                                    Snackbar.make(context.coordinatorLayout, getString(R.string.settings_delete_zones_failure), Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        })
                        .setMessage(getString(R.string.settings_alert_msg_delete_zones))
                        .create();
                alertDialog.show();
                return true;
            }
        });

        cleanMap.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setPositiveButton(getString(R.string.dialog_delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                // Clear passiveCache in LocationCache singleton and execute delete on DB
                                if(LocationCache.getInstance().clearPassiveCache() == 1
                                        && dbHelper.cleanLocs() == 1){
                                    Snackbar.make(context.coordinatorLayout, getString(R.string.settings_clean_map_success), Snackbar.LENGTH_LONG)
                                            .show();
                                    dialog.dismiss();
                                } else {
                                    Snackbar.make(context.coordinatorLayout, getString(R.string.settings_clean_map_failure), Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        })
                        .setMessage(getString(R.string.settings_alert_msg_clean_map))
                        .create();
                alertDialog.show();
                return true;
            }
        });


        // Use Settings elsewhere:
        /*
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String strUserName = SP.getString("username", "NA");
        boolean bAppUpdates = SP.getBoolean("applicationUpdates",false);
        String downloadType = SP.getString("downloadType","1");
        */

        // Saved in path:
        /*
        data/data/packagename/shared_prefs/packagename_preferences.xml.
         */
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = (MainActivity) getActivity();
        getDbHelper(context);
    }

    private DatabaseHelper getDbHelper(Context context) {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }
}
