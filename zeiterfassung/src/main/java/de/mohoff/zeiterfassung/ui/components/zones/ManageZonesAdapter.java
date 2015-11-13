package de.mohoff.zeiterfassung.ui.components.zones;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
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

/**
 * Created by moo on 8/17/15.
 */
// --- Outer adapter ---
public class ManageZonesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private MainActivity context;
    private DatabaseHelper dbHelper = null;
    //private LayoutInflater li;
    List<Zone> zones;
    List<String> activityNames = new ArrayList<String>();
    private ManageZonesAdapter outerAdapter = this;

    private Map<String, AdapterManageZoneInner> locationAdapterMap = new HashMap<>();
    private final static int VIEWTYPE_NORMAL = 1;
    private final static int VIEWTYPE_ADD = 2;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ManageZonesAdapter(Activity context) {
        getDbHelper();
        this.zones = dbHelper.getAllZones();
        this.activityNames = dbHelper.getDistinctActivityNames();
        this.context = (MainActivity)context;
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
        public TextView locationName;
        public ImageButton repinButton;
        public ImageButton deleteButton;

        public LocViewHolder(View view) {
            super(view);
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
        // show "add" option as card listed last
        if(viewType == VIEWTYPE_ADD) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_manage_zones_card_outer_add, parent, false);
            return new AddHolder(v);
        }
        return null;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if(holder.getItemViewType() == VIEWTYPE_NORMAL) {
            final ActViewHolder actHolder = (ActViewHolder) holder;
            // - isRunning element from your dataset at this position
            // - replace the contents of the view with that element
            final String activity = activityNames.get(position);

            // Create an adapter if none exists, and put in the map
            if (!locationAdapterMap.containsKey(activity)) {
                locationAdapterMap.put(activity, new AdapterManageZoneInner(this, context, getRelevantZonesByActivity(zones, activity)));
            }

            actHolder.recyclerView.setAdapter(locationAdapterMap.get(activity));

            actHolder.activityName.setText(activity);
            actHolder.activityName.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final EditText et = new EditText(context);
                    AlertDialog alertDialog = new AlertDialog.Builder(context)
                            .setPositiveButton("save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    if (et.getText().toString().equals(actHolder.activityName.getText().toString())) {
                                        GeneralHelper.showToast(context, "Name has not changed.");
                                    } else {
                                        // Execute update on DB
                                        int result = dbHelper.updateZoneActivityName(activity, et.getText().toString());
                                        if (result == 1) {
                                            updateList(outerAdapter, locationAdapterMap.get(activity)); // (outerAdapter, innerAdapter)
                                            GeneralHelper.showToast(context, "Updated successfully.");
                                            dialog.dismiss();
                                        } else {
                                            GeneralHelper.showToast(context, "Could not update Activity.");
                                        }
                                    }
                                }
                            })
                            .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    // Cancel action
                                    dialog.dismiss();
                                }
                            })
                            .setTitle("Edit Activity")
                            .setMessage("Please enter a new name for the Activity")
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
                            .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    // Delete action
                                    if (dbHelper.deleteZonesByActivity(activity) == 1) {
                                        GeneralHelper.showToast(context, "Activity deleted.");
                                        actHolder.recyclerView.setAdapter(null);
                                        updateList(outerAdapter, null); // outerAdapter == this
                                    } else {
                                        GeneralHelper.showToast(context, "Couldn't delete Activity. Does it still exist?");
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    // Cancel action
                                    dialog.dismiss();
                                }
                            })
                            .setTitle("Delete Activity")
                            .setMessage("Are you sure that you want to delete Activity \"" + activity + "\" with all its Locations?")
                            .create();
                    // TODO: add app icon to the alertDialog.
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

    // Updates the the complete list (outer and inner adapter). To be called when there has happened
    // a DB-change in order to reflect that on UI.
    private void updateList(ManageZonesAdapter outerAdapter, AdapterManageZoneInner innerAdapter){
        // To update the outer adapter, we first have to retrieve all Zones from the DB.
        this.zones = dbHelper.getAllZones();
        this.activityNames = dbHelper.getDistinctActivityNames();
        outerAdapter.notifyDataSetChanged();

        // To update the inner adapter, we first have to compose the relevant Zone list from scratch.
        // When a whole Activity is deleted, innerAdapter is null so we need to check here. In this
        // case, the inner recyclerView will be garbage collected. SetAdapter(null) is called inside
        // alertDialog.
        if (innerAdapter != null) {
            String activity = innerAdapter.relevantZones.get(0).getActivityName();
            innerAdapter.relevantZones = getRelevantZonesByActivity(this.zones, activity);
            innerAdapter.notifyDataSetChanged();
        }
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

    // --- Inner adapter ---
    private class AdapterManageZoneInner extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        MainActivity context;
        List<Zone> relevantZones;
        AdapterManageZoneInner innerAdapter = this;
        ManageZonesAdapter outerAdapter;

        public AdapterManageZoneInner(ManageZonesAdapter outerAdapter, MainActivity context, List<Zone> relevantZones) {
            this.outerAdapter = outerAdapter;
            this.context = context;
            this.relevantZones = relevantZones;
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

                locHolder.locationName.setText(zone.getLocationName());
                locHolder.locationName.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        final EditText et = new EditText(context);
                        AlertDialog alertDialog = new AlertDialog.Builder(context)
                                .setPositiveButton("save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        if(et.getText().toString().equals(locHolder.locationName.getText().toString())){
                                            GeneralHelper.showToast(context, "Name has not changed.");
                                        } else {
                                            // Execute update on DB
                                            int result = dbHelper.updateZoneLocationName(relevantZones.get(position).get_id(), et.getText().toString());
                                            if(result == 1){
                                                // Directly update the adapter's model, so we can avoid a new DB query
                                                relevantZones.get(position).setLocationName(et.getText().toString());
                                                innerAdapter.notifyDataSetChanged();
                                                GeneralHelper.showToast(context, "Updated successfully.");
                                                dialog.dismiss();
                                            } else {
                                                GeneralHelper.showToast(context, "Could not update Location.");
                                            }
                                        }
                                    }
                                })
                                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        // Cancel action
                                        dialog.dismiss();
                                    }
                                })
                                .setTitle("Edit Location")
                                .setMessage("Please enter a new name for the Location")
                                .setView(GeneralHelper.getAlertDialogEditTextContainer(context, et, locHolder.locationName.getText().toString()))
                                .create();
                        // TODO: add app icon to the alertDialog.
                        alertDialog.show();
                        return true;
                    }
                });
                //locHolder.locationName.setOnTouchListener(null);

                locHolder.repinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Fragment nextFragment = new EditZonesMap();
                        // pass ZoneId to map fragment
                        Bundle args = new Bundle();
                        args.putInt("ZoneId", zone.get_id());
                        nextFragment.setArguments(args);

                        context.replaceFragment(nextFragment, true);
                    }
                });
                locHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog alertDialog = new AlertDialog.Builder(
                                context)
                                .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        // Delete action
                                        if (dbHelper.deleteZoneById(zone.get_id()) == 1) {
                                            GeneralHelper.showToast(context, "Location deleted.");
                                            updateList(outerAdapter, innerAdapter); // innerAdapter == this
                                        } else {
                                            GeneralHelper.showToast(context, "Couldn't delete Location. Does it still exist?");
                                        }
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        // Cancel action
                                        dialog.dismiss();
                                    }
                                })
                                .setTitle("Delete Location")
                                .setMessage("Are you sure that you want to delete Location \"" + zone.getLocationName() + "\"?")
                                .create();
                                // TODO: add app icon to the alertDialog.
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
                        args.putString("activityName", relevantZones.get(0).getActivityName());
                        nextFragment.setArguments(args);
                        context.replaceFragment(nextFragment, true);
                        // TODO: Also provide this action in top menubar with "+"-icon. Or by placing the round red bottom right button (see material design)
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
            // if position is last index
            if (position == getItemCount()-1) {
                return VIEWTYPE_ADD;
            } else {
                return VIEWTYPE_NORMAL;
            }
        }
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }
}
