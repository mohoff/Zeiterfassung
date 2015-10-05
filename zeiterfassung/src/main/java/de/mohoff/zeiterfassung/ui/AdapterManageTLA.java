package de.mohoff.zeiterfassung.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
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

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;
import de.mohoff.zeiterfassung.ui.fragments.About;
import de.mohoff.zeiterfassung.ui.fragments.ManageTLAs;
import de.mohoff.zeiterfassung.ui.fragments.MapManageTLAs;
import de.mohoff.zeiterfassung.ui.fragments.Overview;

/**
 * Created by moo on 8/17/15.
 */
public class AdapterManageTLA extends RecyclerView.Adapter<RecyclerView.ViewHolder>{ //RecyclerView.Adapter<AdapterManageTLA.TLAViewHolder>{
    private Context context;
    private DatabaseHelper dbHelper = null;
    //private LayoutInflater li;
    List<TargetLocationArea> tlas;
    List<String> activityNames = new ArrayList<String>();
    //List<EditText> editTextList = new ArrayList<EditText>();
    //private boolean inEditMode = false;

    private Map<String, AdapterManageTLAInner> locationAdapterMap = new HashMap<>();
    private final static int VIEWTYPE_NORMAL = 1;
    private final static int VIEWTYPE_ADD = 2;

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterManageTLA(Context context) {
        getDbHelper();
        this.tlas = dbHelper.getAllTLAs();
        this.activityNames = dbHelper.getDistinctActivityNames();
        this.context = context;
        //li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // Activity ViewHolder (outer holder)
    public static class ActViewHolder extends RecyclerView.ViewHolder {
        EditText activityName;
        RecyclerView recyclerView;
        ImageButton deleteButton;

        ActViewHolder(View itemView) {
            super(itemView);
            recyclerView = (RecyclerView) itemView.findViewById(R.id.innerRecyclerView);
            activityName = (EditText) itemView.findViewById(R.id.activityName);
            deleteButton = (ImageButton) itemView.findViewById(R.id.deleteActivityButton);
        }
    }

    // Location ViewHolder (inner holder)
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

    // AddButton ViewHolder (for outer and inner)
    public static class AddHolder extends RecyclerView.ViewHolder {
        ImageButton addButton;

        AddHolder(View itemView) {
            super(itemView);
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
            ActViewHolder actHolder = (ActViewHolder) holder;
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final String currentActivity = activityNames.get(position);
            final List<TargetLocationArea> relevantTLAs = new ArrayList<TargetLocationArea>();

            for (TargetLocationArea tla : tlas) {
                if (tla.getActivityName().equals(currentActivity)) {
                    relevantTLAs.add(tla);
                }
            }
            actHolder.activityName.setText(currentActivity);
            actHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: Popup "Do you really want to delete this activity?"
                    if (dbHelper.deleteTLAsByActivity(currentActivity) == 1) {
                        // TODO: show toast "Activity and related Locations deleted."
                        notifyDataSetChanged();
                    } else {
                        // TODO: show toast "Couldn't delete Activity. Does it still exist?"
                    }
                }
            });

            // Create an adapter if none exists
            if (!locationAdapterMap.containsKey(currentActivity)) {
                locationAdapterMap.put(currentActivity, new AdapterManageTLAInner(context, relevantTLAs));
            }

            actHolder.recyclerView.setAdapter(locationAdapterMap.get(currentActivity));

        } else if (holder.getItemViewType() == VIEWTYPE_ADD) {
            AddHolder addButtonHolder = (AddHolder) holder;
            addButtonHolder.addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: add on click listener for "add new activty"
                    // TODO: also provide this action in top menubar with "+"-icon
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
        // if position is last index
        if (position == getItemCount()-1) {
            return VIEWTYPE_ADD;
        } else {
            return VIEWTYPE_NORMAL;
        }
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }






    private class AdapterManageTLAInner extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Context context;
        List<TargetLocationArea> relevantTLAs;

        public AdapterManageTLAInner(Context context, List<TargetLocationArea> relevantTLAs) {
            this.context = context;
            this.relevantTLAs = relevantTLAs;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            // show normal Location
            if(viewType == VIEWTYPE_NORMAL) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_manage_tlas_card_inner, parent, false);
                return new LocViewHolder(v);
            }
            // show "add" option as card listed last
            if(viewType == VIEWTYPE_ADD) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_manage_tlas_card_inner_add, parent, false);
                return new AddHolder(v);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if(holder.getItemViewType() == VIEWTYPE_NORMAL) {
                LocViewHolder locHolder = (LocViewHolder) holder;
                final TargetLocationArea tla = relevantTLAs.get(position);

                locHolder.locationName.setText(tla.getLocationName());

                locHolder.repinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Fragment nextFragment = new MapManageTLAs();
                        // pass TLAId to map fragment
                        Bundle args = new Bundle();
                        args.putInt("TLAId", tla.get_id());
                        nextFragment.setArguments(args);

                        ((Activity) context).getFragmentManager()
                                .beginTransaction()
                                .add(R.id.content_frame, nextFragment)
                                //.replace(R.id.content_frame, nextFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                });
                locHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: Popup "Do you really want to delete this activity?"
                        if (dbHelper.deleteTLAById(tla.get_id()) == 1) {
                            // TODO: show toast "Activity and related Locations deleted."
                            notifyDataSetChanged();
                        } else {
                            // TODO: show toast "Couldn't delete Activity. Does it still exist?"
                        }
                    }
                });
            } else if (holder.getItemViewType() == VIEWTYPE_ADD) {
                AddHolder addButtonHolder = (AddHolder) holder;
                addButtonHolder.addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: add on click listener for "add new activty"
                        // TODO: also provide this action in top menubar with "+"-icon
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
}
