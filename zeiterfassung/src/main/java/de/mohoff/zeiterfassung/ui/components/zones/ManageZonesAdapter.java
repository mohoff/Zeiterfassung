package de.mohoff.zeiterfassung.ui.components.zones;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mohoff.zeiterfassung.datamodel.Zone;
import de.mohoff.zeiterfassung.helpers.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;
import de.mohoff.zeiterfassung.ui.colorpicker.ExtraColorPicker;

/**
 * Created by moo on 8/17/15.
 */
// --- Outer adapter ---
public class ManageZonesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private MainActivity context;
    private DatabaseHelper dbHelper = null;
    //private LayoutInflater li;
    List<Zone> zones;
    // TODO: make activityNames a HashMap, key:name, value: #(Zones referred to it). Then we can improve alert dialog message (from "delete Zone(s)" to "delete 3 Zones or delete 1 Zone" (see strings-dialogs.xml)
    List<String> activityNames = new ArrayList<String>();
    private ManageZonesAdapter outerAdapter = this;

    private Map<String, AdapterManageZoneInner> locationAdapterMap = new HashMap<>();
    private final static int VIEWTYPE_NORMAL = 1;
    private final static int VIEWTYPE_ADD = 2;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ManageZonesAdapter(Activity ctx) {
        getDbHelper();
        updateModel();
        context = (MainActivity)ctx;
    }

    // Runs when this Fragment is initially created or an update on the model happens,
    // so notifyDataIsChanged() is called.
    private void updateModel(){
        this.zones = dbHelper.getAllZones();
        this.activityNames = dbHelper.getDistinctActivityNames();
    }

    // Activity ViewHolderItem (outer holder)
    public static class ActViewHolder extends RecyclerView.ViewHolder {
        TextView activityName;
        RecyclerView recyclerView;
        ImageButton deleteButton;

        ActViewHolder(View itemView) {
            super(itemView);
            activityName = (TextView) itemView.findViewById(R.id.activityName);
            recyclerView = (RecyclerView) itemView.findViewById(R.id.innerRecyclerView);
            deleteButton = (ImageButton) itemView.findViewById(R.id.deleteActivityButton);
        }
    }

    // Location ViewHolderItem (inner holder)
    public static class LocViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public View colorBar;
        public TextView locationName;
        public ImageButton repinButton;
        public ImageButton deleteButton;

        public LocViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.card_view_inner);
            colorBar = view.findViewById(R.id.colorBar);
            locationName = (TextView) view.findViewById(R.id.locationName);
            repinButton = (ImageButton) view.findViewById(R.id.repinLocationButton);
            deleteButton = (ImageButton) view.findViewById(R.id.deleteLocationButton);
        }
    }

    // AddButton ViewHolderItem (for outer and inner)
    public static class AddHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageButton addButton;

        AddHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            addButton = (ImageButton) itemView.findViewById(R.id.addActivityButton);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // show normal Activity
        if(viewType == VIEWTYPE_NORMAL) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_manage_zones_card_outer, parent, false);

            ActViewHolder outerHolder = new ActViewHolder(v);
            outerHolder.recyclerView.setHasFixedSize(false);
            de.mohoff.zeiterfassung.ui.components.zones.LinearLayoutManager innerLinLayoutManager = new de.mohoff.zeiterfassung.ui.components.zones.LinearLayoutManager(context);
            innerLinLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            outerHolder.recyclerView.setLayoutManager(innerLinLayoutManager);

            return outerHolder;
        }
        // show ADD option as a card listed last
        if(viewType == VIEWTYPE_ADD) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_manage_zones_card_outer_add, parent, false);
            return new AddHolder(v);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if(holder.getItemViewType() == VIEWTYPE_NORMAL) {
            final ActViewHolder actHolder = (ActViewHolder) holder;
            // - isRunning element from your dataset at this position
            // - replace the contents of the view with that element
            final String activity = activityNames.get(position);

            // Create an adapter if none exists, and put in the map
            if (!locationAdapterMap.containsKey(activity)) {
                locationAdapterMap.put(activity, new AdapterManageZoneInner(
                        this,
                        context,
                        getRelevantZonesByActivity(
                                zones,
                                activity)
                ));
            }

            actHolder.recyclerView.setAdapter(locationAdapterMap.get(activity));

            actHolder.activityName.setText(activity);
            actHolder.activityName.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final EditText et = new EditText(context);
                    AlertDialog alertDialog = new AlertDialog.Builder(context)
                            .setPositiveButton(context.getString(R.string.dialog_save), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    if (et.getText().toString().equals(actHolder.activityName.getText().toString())) {
                                        Snackbar.make(
                                                context.coordinatorLayout,
                                                context.getString(R.string.error_input_name_color_equal),
                                                Snackbar.LENGTH_LONG)
                                        .show();
                                    } else {
                                        // Execute update on DB
                                        int result = dbHelper.updateZoneActivityName(activity, et.getText().toString());
                                        if (result > 0) {
                                            String newActivity = et.getText().toString();
                                            updateListForActivity(activity, newActivity);
                                            Snackbar.make(
                                                    context.coordinatorLayout,
                                                    context.getResources().getQuantityString(
                                                            R.plurals.update_zone_multiple_success,
                                                            result,
                                                            result),
                                                    Snackbar.LENGTH_LONG)
                                            .show();
                                            dialog.dismiss();
                                        } else {
                                            Snackbar.make(
                                                    context.coordinatorLayout,
                                                    context.getString(R.string.update_zone_multiple_failure),
                                                    Snackbar.LENGTH_LONG)
                                            .show();
                                        }
                                    }
                                }
                            })
                            .setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                }
                            })
                            .setTitle(context.getString(R.string.alert_title_update_zone_activity))
                            .setMessage(context.getString(R.string.alert_msg_update_zone))
                            .setView(GeneralHelper.getAlertDialogEditTextContainer(context, et, actHolder.activityName.getText().toString()))
                            .create();
                    alertDialog.show();
                    return true;
                }
            });

            actHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog alertDialog = new AlertDialog.Builder(
                            context)
                            .setPositiveButton(context.getString(R.string.dialog_delete), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    // Delete action
                                    int result = dbHelper.deleteZonesByActivity(activity);
                                    if (result > 0) {
                                        Snackbar.make(
                                                context.coordinatorLayout,
                                                context.getResources().getQuantityString(
                                                        R.plurals.delete_zone_multiple_success,
                                                        result,
                                                        result),
                                                Snackbar.LENGTH_LONG)
                                        .show();
                                        // To make sure the whole Activity gets removed from UI,
                                        // first set adapter to null.
                                        actHolder.recyclerView.setAdapter(null);
                                        updateListForActivity(activity, null);
                                    } else {
                                        Snackbar.make(
                                                context.coordinatorLayout,
                                                context.getString(R.string.delete_zone_multiple_failure),
                                                Snackbar.LENGTH_LONG)
                                        .show();
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                }
                            })
                            .setTitle(context.getString(R.string.alert_title_delete_zone_multiple))
                            .setMessage(context.getString(R.string.alert_msg_delete_zone_multiple, activity))
                                    .create();
                    // TODO: add app colorBarIcon to the alertDialog.
                    alertDialog.show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return activityNames.size();
    }

    @Override
    public int getItemViewType(int position) {
        return VIEWTYPE_NORMAL;
    }


    // --- Inner adapter ---
    private class AdapterManageZoneInner extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        MainActivity context;
        List<Zone> relevantZones;
        AdapterManageZoneInner innerAdapter = this;
        ManageZonesAdapter outerAdapter;
        LayoutInflater inflater;

        public AdapterManageZoneInner(ManageZonesAdapter outerAdapter, MainActivity context, List<Zone> relevantZones) {
            this.outerAdapter = outerAdapter;
            this.context = context;
            this.relevantZones = relevantZones;
            this.inflater = context.getLayoutInflater();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Show normal Location
            if(viewType == VIEWTYPE_NORMAL) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_manage_zones_card_inner, parent, false);
                return new LocViewHolder(v);
            }
            // Show "add" option as card listed last
            if(viewType == VIEWTYPE_ADD) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_manage_zones_card_inner_add, parent, false);
                return new AddHolder(v);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if(holder.getItemViewType() == VIEWTYPE_NORMAL) {
                final LocViewHolder locHolder = (LocViewHolder) holder;

                // Get Zone at position to setup textView and clickListeners below
                final Zone zone = relevantZones.get(position);

                locHolder.colorBar.setBackgroundColor(zone.getColor());

                locHolder.locationName.setText(zone.getLocationName());

                locHolder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        // TODO: add color picker in alertDialog to edit color in it as well.
                        View dialogView = inflater.inflate(R.layout.alert_dialog_zone_location, null);
                        final EditText et = (EditText) dialogView.findViewById(R.id.editText);
                        et.setText(zone.getLocationName());
                        final ExtraColorPicker colorPicker = (ExtraColorPicker) dialogView.findViewById(R.id.colorPicker);
                        colorPicker.setSelectedColor(zone.getColor());

                        AlertDialog alertDialog = new AlertDialog.Builder(context)
                                .setPositiveButton(context.getString(R.string.dialog_save), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        if(et.getText().toString().equals(locHolder.locationName.getText().toString()) &&
                                                colorPicker.getSelectedColor() == zone.getColor()){
                                            Snackbar.make(
                                                    context.coordinatorLayout,
                                                    context.getString(R.string.error_input_name_color_equal),
                                                    Snackbar.LENGTH_LONG)
                                            .show();
                                        } else {
                                            zone.setLocationName(et.getText().toString());
                                            zone.setColor(colorPicker.getSelectedColor());
                                            if(dbHelper.updateZone(zone) == 1){
                                                updateListForLocationEdit(innerAdapter, zone, position);

                                                // TODO: delete next 2 lines?
                                                //relevantZones.set(position, zone);
                                                //notifyDataSetChanged();

                                                Snackbar.make(
                                                        context.coordinatorLayout,
                                                        context.getString(R.string.update_zone_success),
                                                        Snackbar.LENGTH_LONG)
                                                .show();
                                                dialog.dismiss();
                                            } else {
                                                Snackbar.make(
                                                        context.coordinatorLayout,
                                                        context.getString(R.string.update_zone_failure),
                                                        Snackbar.LENGTH_LONG)
                                                .show();
                                            }
                                        }
                                    }
                                })
                                .setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        dialog.dismiss();
                                    }
                                })
                                .setTitle(context.getString(R.string.alert_title_update_zone_location))
                                .setView(dialogView)
                                .create();
                        // TODO: add app colorBarIcon to the alertDialog.
                        alertDialog.show();
                        return true;
                    }
                });

                locHolder.repinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Fragment nextFragment = new EditZonesMap();
                        // pass ZoneId to map fragment
                        Bundle args = new Bundle();
                        args.putInt(context.getString(R.string.arg_zone_id), zone.get_id());
                        nextFragment.setArguments(args);
                        context.replaceFragment(nextFragment, true);
                    }
                });
                locHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog alertDialog = new AlertDialog.Builder(
                                context)
                                .setPositiveButton(context.getString(R.string.dialog_delete), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        if(1 == dbHelper.deleteZoneById(zone.get_id())) {
                                            Snackbar.make(
                                                    context.coordinatorLayout,
                                                    context.getString(R.string.delete_zone_success),
                                                    Snackbar.LENGTH_LONG)
                                            .show();

                                            // Redirect deletion of Location to deletion of Activity when there
                                            // is only one Location for an Activity. So also the outer adapter
                                            // can update itself.
                                            if(relevantZones.size() == 1){
                                                updateListForActivity(zone.getActivityName(), null);
                                            } else {
                                                updateListForLocationDeletion(innerAdapter, zone.getActivityName());
                                            }
                                        } else {
                                            Snackbar.make(
                                                    context.coordinatorLayout,
                                                    context.getString(R.string.delete_zone_failure),
                                                    Snackbar.LENGTH_LONG)
                                            .show();
                                        }
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(context.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        dialog.dismiss();
                                    }
                                })
                                .setTitle(context.getString(R.string.alert_title_delete_zone))
                                .setMessage(context.getString(R.string.alert_msg_delete_zone, zone.getLocationName()))
                                .create();
                                // TODO: add app colorBarIcon to the alertDialog.
                        alertDialog.show();
                    }
                });
            } else if (holder.getItemViewType() == VIEWTYPE_ADD) {
                AddHolder addButtonHolder = (AddHolder) holder;
                addButtonHolder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Fragment nextFragment = new AddZone();
                        Bundle args = new Bundle();
                        args.putString(context.getString(R.string.arg_activity), relevantZones.get(0).getActivityName());
                        nextFragment.setArguments(args);
                        context.replaceFragment(nextFragment, true);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return relevantZones.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == getItemCount()-1) {
                return VIEWTYPE_ADD;
            } else {
                return VIEWTYPE_NORMAL;
            }
        }
    }



    // --- Methods for updating the nested lists in case of deletions and edits. ---


    // Used for both deletion and editing (of Activity name) of Activities.
    // In case of deletion, parameter newActivity must be null.
    // In case of editing, parameter newActivity must hold the new Activity name.
    // Also used for deletions of Locations when there are no other Locations for an Activity
    // available.
    private void updateListForActivity(String oldActivity, String newActivity){
        updateModel();
        // Remove entry in locationAdapterMap for old Activity name.
        locationAdapterMap.remove(oldActivity);
        // newActivity == null: Activity got deleted. Don't add new inner adapter.
        // newActivity != null: Activity name got edited. New name stored in newActivity. Add new inner
        // adapter.
        // Put entry in locationAdapterMap for new Activity name and init inner adapter.
        if(newActivity != null){
            locationAdapterMap.put(newActivity, new AdapterManageZoneInner(
                    outerAdapter,
                    context,
                    getRelevantZonesByActivity(
                            zones,
                            newActivity)
            ));
        }
        outerAdapter.notifyDataSetChanged();
    }

    // Used when a Location is deleted but there are still other Locations saved for the same Activity.
    // When there is only one Location within the Activity, the delete-call is passed to
    // updateListForActivity().
    private void updateListForLocationDeletion(AdapterManageZoneInner innerAdapter, String activity){
        updateModel();
        innerAdapter.relevantZones = getRelevantZonesByActivity(zones, activity);
        innerAdapter.notifyDataSetChanged();
    }

    // Used when an existing Location is edited (either name or color). We directly update the model
    // and reflect the changes in the inner adapter.
    private void updateListForLocationEdit(AdapterManageZoneInner innerAdapter, Zone zone, int position){
        innerAdapter.relevantZones.set(position, zone);
        innerAdapter.notifyDataSetChanged();
    }

    // Helper function to reduce all Zones to a setIsRunning which elements all correspond to one Activity.
    private List<Zone> getRelevantZonesByActivity(List<Zone> list, String activity){
        ArrayList<Zone> relevantZones = new ArrayList<>();
        for (Zone entry : list) {
            if (entry.getActivityName().equals(activity)) {
                relevantZones.add(entry);
            }
        }
        return relevantZones;
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }
}
