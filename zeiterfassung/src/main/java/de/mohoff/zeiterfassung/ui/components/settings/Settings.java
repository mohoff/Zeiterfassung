package de.mohoff.zeiterfassung.ui.components.settings;


import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.LocationCache;
import de.mohoff.zeiterfassung.datamodel.Zone;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;
import de.mohoff.zeiterfassung.ui.components.MapAbstract;

public class Settings extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    MainActivity context;
    DatabaseHelper dbHelper;
    SharedPreferences sp;

    Preference showAppIntro;
    Preference cleanMap, deleteAllTimeslots, deleteAllZones, resetAllSettings;
    Preference mapDefaultZoomLevel;
    Preference sendCrashlogs;

    public Settings() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        initSummaries(getPreferenceScreen());

        showAppIntro = findPreference(getString(R.string.setting_general_show_intro));
        deleteAllTimeslots = findPreference(getString(R.string.setting_delete_timeslots));
        deleteAllZones = findPreference(getString(R.string.setting_delete_zones));
        cleanMap = findPreference(getString(R.string.setting_clean_map));
        resetAllSettings = findPreference(getString(R.string.setting_reset_settings));
        mapDefaultZoomLevel = findPreference(getString(R.string.setting_map_default_zoom));
        sendCrashlogs = findPreference(getString(R.string.setting_crashlog_report));

        showAppIntro.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                context.showAppIntro();
                return true;
            }
        });

        deleteAllTimeslots.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setPositiveButton(getString(R.string.dialog_delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                if (dbHelper.deleteAllTimeslots() == 1) {
                                    Snackbar.make(
                                            context.coordinatorLayout,
                                            getString(R.string.settings_delete_entries_success),
                                            Snackbar.LENGTH_LONG)
                                    .show();
                                } else {
                                    Snackbar.make(
                                            context.coordinatorLayout,
                                            getString(R.string.settings_delete_entries_failure),
                                            Snackbar.LENGTH_LONG)
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
                                    Snackbar.make(
                                            context.coordinatorLayout,
                                            getString(R.string.settings_delete_zones_success),
                                            Snackbar.LENGTH_LONG)
                                    .show();
                                } else {
                                    Snackbar.make(
                                            context.coordinatorLayout,
                                            getString(R.string.settings_delete_zones_failure),
                                            Snackbar.LENGTH_LONG)
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
                                    Snackbar.make(
                                            context.coordinatorLayout,
                                            getString(R.string.settings_clean_map_success),
                                            Snackbar.LENGTH_LONG)
                                    .show();
                                } else {
                                    Snackbar.make(
                                            context.coordinatorLayout,
                                            getString(R.string.settings_clean_map_failure),
                                            Snackbar.LENGTH_LONG)
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

        resetAllSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setPositiveButton(getString(R.string.dialog_reset), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                if(sp.edit().clear().commit()){
                                    setPreferenceScreen(null);
                                    addPreferencesFromResource(R.xml.preferences);
                                    initSummaries(getPreferenceScreen());
                                    Snackbar.make(
                                            context.coordinatorLayout,
                                            getString(R.string.settings_reset_settings_success),
                                            Snackbar.LENGTH_LONG)
                                    .show();
                                } else {
                                    Snackbar.make(
                                            context.coordinatorLayout,
                                            getString(R.string.settings_reset_settings_failure),
                                            Snackbar.LENGTH_LONG)
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
                        .setMessage(getString(R.string.settings_alert_msg_reset_settings))
                        .create();
                alertDialog.show();
                return true;
            }
        });

        sendCrashlogs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SwitchPreference pref = (SwitchPreference) preference;
                boolean isEnabled = (boolean) newValue;
                if(!isEnabled){
                    //pref.setChecked(true);
                    // TODO: Yeah.. rework that later on ;)
                    Snackbar.make(context.coordinatorLayout, getString(R.string.settings_report_crashlogs_block), Snackbar.LENGTH_LONG)
                            .show();
                    return false;
                }
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
        context.fab.hide();

        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private DatabaseHelper getDbHelper(Context context) {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary(findPreference(key));
    }

    private void initSummaries(Preference p){
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pg = (PreferenceGroup) p;
            for (int i = 0; i < pg.getPreferenceCount(); i++) {
                initSummaries(pg.getPreference(i));
            }
        } else {
            updateSummary(p);
        }
    }

    private void updateSummary(Preference p) {
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if(p.getKey().equals(getString(R.string.setting_zones_default_radius))){
                int input = Integer.parseInt(editTextPref.getText());
                if(input < Zone.MIN_RADIUS){
                    editTextPref.setText(String.valueOf(Zone.MIN_RADIUS));
                    Snackbar.make(
                            context.coordinatorLayout,
                            getString(R.string.settings_alert_msg_zones_default_radius_failure, Zone.MIN_RADIUS),
                            Snackbar.LENGTH_LONG)
                    .show();
                }
                p.setSummary(editTextPref.getText() + " meters");
            } else if(p.getKey().equals(getString(R.string.setting_map_default_zoom))){
                // Clamp input to interval [1 ... 15]
                int input = Integer.parseInt(editTextPref.getText());
                if(input > (MapAbstract.MAX_ZOOM_LEVEL - MapAbstract.ZOOM_LEVEL_OFFSET_FOR_PREFS)){
                    input = MapAbstract.MAX_ZOOM_LEVEL - MapAbstract.ZOOM_LEVEL_OFFSET_FOR_PREFS; // 21-6=15
                    Snackbar.make(
                            context.coordinatorLayout,
                            getString(R.string.settings_alert_msg_map_default_zoom_toohigh, Zone.MIN_RADIUS),
                            Snackbar.LENGTH_LONG)
                    .show();
                }
                // Usually we need to compute treshold with MapAbstract.ZOOM_LEVEL_OFFSET_FOR_PREFS
                // like it is done above. But both the lower hard limit and the lower pref limit
                // are 1, we can just use MapAbstract.MIN_ZOOM_LEVEL here for comparison.
                if(input < MapAbstract.MIN_ZOOM_LEVEL) {
                    input = MapAbstract.MIN_ZOOM_LEVEL;
                    Snackbar.make(
                            context.coordinatorLayout,
                            getString(R.string.settings_alert_msg_map_default_zoom_toolow, Zone.MIN_RADIUS),
                            Snackbar.LENGTH_LONG)
                    .show();
                }
                editTextPref.setText(String.valueOf(input));
                editTextPref.setSummary(input + " (" + (input-1)*100/14+ "%)");
            } else {
                p.setSummary(editTextPref.getText());
            }
        }
    }

    public static int getRealZoomLevel(int fromPref){
        // fromPref: [1 .. 15]
        // real zoom level values: [1 .. 21]
        int realZoom = fromPref + MapAbstract.ZOOM_LEVEL_OFFSET_FOR_PREFS;
        if(realZoom > MapAbstract.MAX_ZOOM_LEVEL) return MapAbstract.MAX_ZOOM_LEVEL;
        if(realZoom < MapAbstract.MIN_ZOOM_LEVEL) return MapAbstract.MIN_ZOOM_LEVEL;
        return realZoom;
    }

    public static long getTimeInPastForArrayIndex(Context context, int i){
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        switch (i){
            case 0:
                // Today
                cal.set(Calendar.HOUR_OF_DAY, 0);
                return cal.getTimeInMillis();
            case 1:
                // Yesterday
                // By using 'Yesterday', the calling method needs to call this method again with i=0.
                // Then the time interval of the last day will be computed correctly.
                // First call: yesterdayStarttime, Second call: yesterdayEndtime
                cal.add(Calendar.DATE, -1);
                return cal.getTimeInMillis();
            case 2:
                // This week
                // Workaround because cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) is not working reliably.
                int currentDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 7 - cal.getFirstDayOfWeek()) % 7;
                cal.add(Calendar.DAY_OF_YEAR, -currentDayOfWeek);
                return cal.getTimeInMillis();
            case 3:
                // Last 10 days
                cal.add(Calendar.DATE, -10);
                return cal.getTimeInMillis();
            case 4:
                // This month
                cal.set(Calendar.DAY_OF_MONTH, 1);
                return cal.getTimeInMillis();
            case 5:
                // This year
                cal.set(Calendar.DAY_OF_YEAR, 1);
                return cal.getTimeInMillis();
            case 6:
                // Alltime
                return 0;
            default:
                // index not found. Should not happen.
                return -1;
        }
    }
}
