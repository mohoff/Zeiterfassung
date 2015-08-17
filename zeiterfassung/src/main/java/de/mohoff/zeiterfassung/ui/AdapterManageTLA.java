package de.mohoff.zeiterfassung.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;

/**
 * Created by moo on 8/17/15.
 */
public class AdapterManageTLA extends RecyclerView.Adapter<AdapterManageTLA.TLAViewHolder>{
    private Context context;
    private DatabaseHelper dbHelper = null;
    private LayoutInflater li;
    List<TargetLocationArea> tlas;
    List<String> activityNames = new ArrayList<String>();
    List<EditText> editTextList = new ArrayList<EditText>();
    private boolean inEditMode = false;

    private Map<String, AdapterManageTLAInner> locationAdapterMap = new HashMap<>();

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterManageTLA(Context context) {
        getDbHelper();
        this.tlas = dbHelper.getAllTLAs();
        this.activityNames = dbHelper.getDistinctActivityNames();
        this.context = context;

        li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static class TLAViewHolder extends RecyclerView.ViewHolder {
        EditText activityName;
        RecyclerView recyclerView;

        TLAViewHolder(View itemView) {
            super(itemView);
            recyclerView = (RecyclerView)itemView.findViewById(R.id.innerRecyclerView);
            activityName = (EditText)itemView.findViewById(R.id.activityName);
            //locationName = (EditText)itemView.findViewById(R.id.locationName);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AdapterManageTLA.TLAViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_manage_tlas_card_outer, parent, false);
        // set the view's size, margins, paddings and layout parameters


        TLAViewHolder outerHolder = new TLAViewHolder(v);
        outerHolder.recyclerView.setHasFixedSize(false);
        de.mohoff.zeiterfassung.ui.LinearLayoutManager innerLinLayoutManager = new de.mohoff.zeiterfassung.ui.LinearLayoutManager(context);
        innerLinLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        outerHolder.recyclerView.setLayoutManager(innerLinLayoutManager);

        return outerHolder;
    }


    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(TLAViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        final String currentActivity = activityNames.get(position);
        final List<TargetLocationArea> relevantTLAs = new ArrayList<TargetLocationArea>();

        for(TargetLocationArea tla : tlas){
            if(tla.getActivityName().equals(currentActivity)){
                relevantTLAs.add(tla);
            }
        }
        holder.activityName.setText(currentActivity);


        for(TargetLocationArea tla : relevantTLAs) {

            //holder.outerCardView.addView();
        }

        // Create an adapter if none exists
        if (!locationAdapterMap.containsKey(currentActivity)) {
            locationAdapterMap.put(currentActivity, new AdapterManageTLAInner(context, relevantTLAs));
        }

        holder.recyclerView.setAdapter(locationAdapterMap.get(currentActivity));
    }

    @Override
    public int getItemCount() {
        return activityNames.size();
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }






    private class AdapterManageTLAInner extends RecyclerView.Adapter<AdapterManageTLAInner.ViewHolder> {
        Context context;
        List<TargetLocationArea> relevantTLAs;

        class ViewHolder extends RecyclerView.ViewHolder {
            public TextView locationName;

            public ViewHolder(View view) {
                super(view);
                locationName = (TextView) view.findViewById(R.id.locationName);
            }
        }

        public AdapterManageTLAInner(Context context, List<TargetLocationArea> relevantTLAs) {
            this.context = context;
            this.relevantTLAs = relevantTLAs;
        }

        @Override
        public AdapterManageTLAInner.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //Log.d(TAG, "onCreateViewHolder(" + parent.getId() + ", " + i + ")");
            // Create a new view by inflating the row item xml.
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_manage_tlas_card_inner, parent, false);

            // Set the view to the ViewHolder
            ViewHolder holder = new ViewHolder(v);
            //holder.mCoverImageView.setOnClickListener(BookListAdapter.this); // Download or Open
            //holder.mCoverImageView.setTag(holder);

            return holder;
        }

        @Override
        public void onBindViewHolder(AdapterManageTLAInner.ViewHolder holder, int position) {
            TargetLocationArea tla = relevantTLAs.get(position);

            holder.locationName.setText(tla.getLocationName());
        }

        @Override
        public int getItemCount() {
            return relevantTLAs.size();
        }
    }



    // custom LinearLayoutManager




}
