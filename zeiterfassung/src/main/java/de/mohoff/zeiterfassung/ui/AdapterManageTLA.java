package de.mohoff.zeiterfassung.ui;

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

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;
import de.mohoff.zeiterfassung.ui.fragments.AddTLA;
import de.mohoff.zeiterfassung.ui.fragments.MapEditTLAs;

/**
 * Created by moo on 8/17/15.
 */
// --- Outer adapter ---
public class AdapterManageTLA extends RecyclerView.Adapter<RecyclerView.ViewHolder>{ //RecyclerView.Adapter<AdapterManageTLA.TLAViewHolder>{
    private MainActivity context;
    private DatabaseHelper dbHelper = null;
    //private LayoutInflater li;
    List<TargetLocationArea> tlas;
    List<String> activityNames = new ArrayList<String>();
    private AdapterManageTLA outerAdapter = this;

    private Map<String, AdapterManageTLAInner> locationAdapterMap = new HashMap<>();
    private final static int VIEWTYPE_NORMAL = 1;
    private final static int VIEWTYPE_ADD = 2;

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterManageTLA(Activity context) {
        getDbHelper();
        this.tlas = dbHelper.getAllTLAs();
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
                    .inflate(R.layout.fragment_manage_tlas_card_outer, parent, false);

            ActViewHolder outerHolder = new ActViewHolder(v);
            outerHolder.recyclerView.setHasFixedSize(false);
            de.mohoff.zeiterfassung.ui.LinearLayoutManager innerLinLayoutManager = new de.mohoff.zeiterfassung.ui.LinearLayoutManager(context);
            innerLinLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            outerHolder.recyclerView.setLayoutManager(innerLinLayoutManager);

            return outerHolder;
        }
        // show "add" option as card listed last
        if(viewType == VIEWTYPE_ADD) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_manage_tlas_card_outer_add, parent, false);
            return new AddHolder(v);
        }
        return null;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if(holder.getItemViewType() == VIEWTYPE_NORMAL) {
            final ActViewHolder actHolder = (ActViewHolder) holder;
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final String activity = activityNames.get(position);

            // Create an adapter if none exists, and put in the map
            if (!locationAdapterMap.containsKey(activity)) {
                locationAdapterMap.put(activity, new AdapterManageTLAInner(this, context, getRelevantTLAsByActivity(tlas, activity)));
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
                                        int result = dbHelper.updateTLAActivityName(activity, et.getText().toString());
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
                                    if (dbHelper.deleteTLAsByActivity(activity) == 1) {
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
        } else if (holder.getItemViewType() == VIEWTYPE_ADD) {
            AddHolder addButtonHolder = (AddHolder) holder;
            addButtonHolder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: also provide this action in top menubar with "+"-icon
                    // TODO: Combine following code with onClickListener for Location-addButton. Latter with arguments, so Activity can be preset in AddTLA-View.
                    Fragment nextFragment = new AddTLA();
                    context.replaceFragment(nextFragment, true);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return activityNames.size() + 1; // +1 to add a addEntry at the end ...
    }

    @Override
    public int getItemViewType(int position) {
        // Check if position number corresponds to the last index. If so, make this element the
        // "add"-Panel. Else treat it as regular list element.
        if (position == getItemCount()-1) {
            return VIEWTYPE_ADD;
        } else {
            return VIEWTYPE_NORMAL;
        }
    }

    // Updates the the complete list (outer and inner adapter). To be called when there has happened
    // a DB-change in order to reflect that on UI.
    private void updateList(AdapterManageTLA outerAdapter, AdapterManageTLAInner innerAdapter){
        // To update the outer adapter, we first have to retrieve all TLAs from the DB.
        this.tlas = dbHelper.getAllTLAs();
        this.activityNames = dbHelper.getDistinctActivityNames();
        outerAdapter.notifyDataSetChanged();

        // To update the inner adapter, we first have to compose the relevant TLA list from scratch.
        // When a whole Activity is deleted, innerAdapter is null so we need to check here. In this
        // case, the inner recyclerView will be garbage collected. SetAdapter(null) is called inside
        // alertDialog.
        if (innerAdapter != null) {
            String activity = innerAdapter.relevantTLAs.get(0).getActivityName();
            innerAdapter.relevantTLAs = getRelevantTLAsByActivity(this.tlas, activity);
            innerAdapter.notifyDataSetChanged();
        }
    }

    // Helper function to reduce all TLAs to a set which elements all correspond to one Activity.
    private List<TargetLocationArea> getRelevantTLAsByActivity(List<TargetLocationArea> list, String activity){
        ArrayList<TargetLocationArea> relevantTLAs = new ArrayList<>();
        for (TargetLocationArea entry : list) {
            if (entry.getActivityName().equals(activity)) {
                relevantTLAs.add(entry);
            }
        }
        return relevantTLAs;
    }

    // --- Inner adapter ---
    private class AdapterManageTLAInner extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        MainActivity context;
        List<TargetLocationArea> relevantTLAs;
        AdapterManageTLAInner innerAdapter = this;
        AdapterManageTLA outerAdapter;

        public AdapterManageTLAInner(AdapterManageTLA outerAdapter, MainActivity context, List<TargetLocationArea> relevantTLAs) {
            this.outerAdapter = outerAdapter;
            this.context = context;
            this.relevantTLAs = relevantTLAs;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Show normal Location
            if(viewType == VIEWTYPE_NORMAL) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_manage_tlas_card_inner, parent, false);
                return new LocViewHolder(v);
            }
            // Show "add" option as card listed last
            if(viewType == VIEWTYPE_ADD) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_manage_tlas_card_inner_add, parent, false);
                return new AddHolder(v);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if(holder.getItemViewType() == VIEWTYPE_NORMAL) {
                final LocViewHolder locHolder = (LocViewHolder) holder;

                // Get TLA at position to setup textView and clickListeners below
                final TargetLocationArea tla = relevantTLAs.get(position);

                locHolder.locationName.setText(tla.getLocationName());
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
                                            int result = dbHelper.updateTLALocationName(relevantTLAs.get(position).get_id(), et.getText().toString());
                                            if(result == 1){
                                                // Directly update the adapter's model, so we can avoid a new DB query
                                                relevantTLAs.get(position).setLocationName(et.getText().toString());
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
                        Fragment nextFragment = new MapEditTLAs();
                        // pass TLAId to map fragment
                        Bundle args = new Bundle();
                        args.putInt("TLAId", tla.get_id());
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
                                        if (dbHelper.deleteTLAById(tla.get_id()) == 1) {
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
                                .setMessage("Are you sure that you want to delete Location \"" + tla.getLocationName() + "\"?")
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
                        Fragment nextFragment = new AddTLA();
                        Bundle args = new Bundle();
                        args.putString("activityName", relevantTLAs.get(0).getActivityName());
                        nextFragment.setArguments(args);
                        context.replaceFragment(nextFragment, true);
                        // TODO: Also provide this action in top menubar with "+"-icon. Or by placing the round red bottom right button (see material design)
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return relevantTLAs.size() + 1;
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
