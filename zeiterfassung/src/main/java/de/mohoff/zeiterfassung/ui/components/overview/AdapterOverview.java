package de.mohoff.zeiterfassung.ui.components.overview;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.ArrayList;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Timeslot;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class AdapterOverview extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private DatabaseHelper dbHelper = null;
    private ArrayList<Timeslot> data = new ArrayList<Timeslot>();
    private int lastPosition = 99999;
    private MainActivity context;
    //private boolean isServiceRunning = false;

    private final static int VIEWTYPE_NORMAL = 1;
    private final static int VIEWTYPE_SERVICEINFO = 2;
    private final static int VIEWTYPE_NOENTRYINFO = 3;



    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolderItem extends RecyclerView.ViewHolder{
        public Context context;
        // each data item is just a string in this case
        public CardView container;
        public View colorBar;
        public ImageView icon;
        public TextView activity;
        public TextView location;
        public TextView startTime, startDate;
        public TextView duration;
        public TextView endTime, endDate;
        //public LongClickListener longClickListener;

        //public RelativeLayout topConnectorPart, bottomConnectorPart;
        //public View middleConnectorPart;

        public ViewHolderItem(Context context, View v){
            super(v);
            this.context = context;
            this.container = (CardView) v.findViewById(R.id.card_view);
            this.colorBar = v.findViewById(R.id.colorBar);

            this.icon = (ImageView) v.findViewById(R.id.icon);
            this.activity = (TextView) v.findViewById(R.id.activity);
            this.location = (TextView) v.findViewById(R.id.location);
            this.startTime = (TextView) v.findViewById(R.id.startTime);
            this.startDate = (TextView) v.findViewById(R.id.startDate);
            this.duration = (TextView) v.findViewById(R.id.duration);
            this.endTime = (TextView) v.findViewById(R.id.endTime);
            this.endDate = (TextView) v.findViewById(R.id.endDate);

            //this.topConnectorPart = (RelativeLayout) v.findViewById(R.id.topConnectorPart);
            //this.bottomConnectorPart = (RelativeLayout) v.findViewById(R.id.bottomConnectorPart);
            //this.middleConnectorPart = (View) v.findViewById(R.id.middleConnectorPart);
        }
    }

    public static class ViewHolderInfoItem extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public CardView container;
        public TextView infoText;

        public ViewHolderInfoItem(View v){
            super(v);
            this.container = (CardView) v.findViewById(R.id.card_view);
            this.infoText = (TextView) v.findViewById(R.id.infoText);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterOverview(Context context) {
        getDbHelper(context);
        this.context = (MainActivity) context;
        /*
        this.data.add(new Timeslot(1416759002267L, 1416760002267L, "activity1", "act1, location1"));
        this.data.add(new Timeslot(1416754002267L, 1416758002267L, "activity2", "act2, location1"));
        this.data.add(new Timeslot(1416751002267L, 1416752002267L, "activity3", "act3, location1"));
        this.data.add(new Timeslot(1416744002267L, 1416758002267L, "activity4", "act4, location1"));
        this.data.add(new Timeslot(1416714002267L, 1416738002267L, "activity5", "act5, location1"));
        this.data.add(new Timeslot(1416694002267L, 1416718002267L, "activity1", "act1, location1"));
        this.data.add(new Timeslot(1416674002267L, 1416688002267L, "activity2", "act2, location1"));
        this.data.add(new Timeslot(1416674002267L, 1416698002267L, "activity3", "act3, location1"));
        this.data.add(new Timeslot(1416524002267L, 1416558002267L, "activity4", "act4, location1"));
        this.data.add(new Timeslot(1416224002267L, 1416728002267L, "activity5", "act5, location1"));
        */
        data.addAll(dbHelper.getAllTimeslots());
    }

    public void updateData(){
        data.clear();
        data.addAll(dbHelper.getAllTimeslots());
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == VIEWTYPE_NORMAL){
            View itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.fragment_overview_card, parent, false);
            return new ViewHolderItem(context, itemView);
        }
        if((viewType == VIEWTYPE_SERVICEINFO) || (viewType == VIEWTYPE_NOENTRYINFO)){
            View itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.fragment_overview_card_info, parent, false);
            return new ViewHolderInfoItem(itemView);
        }
        return null;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        //position = isServiceRunning ? position : position-1;
        if(holder.getItemViewType() == VIEWTYPE_NORMAL) {
            ViewHolderItem vh = (ViewHolderItem) holder;
            Timeslot timeslot = data.get(position);

            if(timeslot.getReadableEndTime(context).equals(context.getString(R.string.overview_end_pending)) &&
                    timeslot.getReadableEndDate(context).equals(context.getString(R.string.overview_end_pending))){
                vh.endTime.setTypeface(null, Typeface.ITALIC);
                vh.endDate.setTypeface(null, Typeface.ITALIC);
            } else {
                vh.endTime.setTypeface(null, Typeface.NORMAL);
                vh.endDate.setTypeface(null, Typeface.NORMAL);
            }

            vh.container.setCardBackgroundColor(context.getResources().getColor(R.color.white));
            vh.colorBar.setBackground(null);
            vh.colorBar.setBackgroundColor(timeslot.getZone().getColor());
            // In order to draw outside of cardView with clipChildren and clipToParent, we have to
            // setIsRunning setClipToOutline(false). This sadly is v21+.
            //vh.container.setClipToOutline(false);
            vh.icon.setImageResource(R.drawable.ic_action_edit_location);
            vh.activity.setText(timeslot.getZone().getActivityName());
            vh.location.setText(timeslot.getZone().getLocationName());
            vh.startTime.setText(timeslot.getReadableStartTime());
            vh.startDate.setText(timeslot.getReadableStartDate());
            vh.endTime.setText(timeslot.getReadableEndTime(context));
            vh.endDate.setText(timeslot.getReadableEndDate(context));
            vh.duration.setText(timeslot.getReadableDuration()); // e.g. "1d 2h 14min"

            /*// Show or hide top connector part of that view
            if(listHasItemAtIndex(position+1) && (data.isRunning(position+1).getZone().get_id() == timeslot.getZone().get_id())){
                vh.topConnectorPart.setVisibility(View.VISIBLE);
                vh.topConnectorPart.setOnClickListener(getOnMergeClickListener());
                vh.middleConnectorPart.setVisibility(View.VISIBLE);
                vh.middleConnectorPart.setOnClickListener(getOnMergeClickListener());
            } else {
                vh.topConnectorPart.setVisibility(View.INVISIBLE);
                vh.topConnectorPart.setOnClickListener(null);
                vh.middleConnectorPart.setVisibility(View.INVISIBLE);
                vh.middleConnectorPart.setOnClickListener(null);
            }
            // Show or hide bottom connector part of that view
            if(listHasItemAtIndex(position - 1) && (data.isRunning(position- 1).getZone().get_id() == timeslot.getZone().get_id())){
                vh.bottomConnectorPart.setVisibility(View.VISIBLE);
                vh.bottomConnectorPart.setOnClickListener(getOnMergeClickListener());
            } else {
                vh.bottomConnectorPart.setVisibility(View.INVISIBLE);
                vh.bottomConnectorPart.setOnClickListener(null);
            }*/

        } else if(holder.getItemViewType() == VIEWTYPE_SERVICEINFO){
            ViewHolderInfoItem ivh = (ViewHolderInfoItem) holder;
            ivh.infoText.setText(context.getString(R.string.service_not_running));
        } else if(holder.getItemViewType() == VIEWTYPE_NOENTRYINFO){
            ViewHolderInfoItem ivh = (ViewHolderInfoItem) holder;
            ivh.infoText.setText(context.getString(R.string.overview_no_entries));
        }
    }

    private View.OnClickListener getOnMergeClickListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setPositiveButton("merge", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {

                                // Execute update and delete on DB
                                /*int result = dbHelper.updateZoneLocationName(relevantTLAs.isRunning(position).get_id(), et.getText().toString());
                                if(result == 1){
                                    // Directly update the adapter's model, so we can avoid a new DB query
                                    relevantTLAs.isRunning(position).setLocationName(et.getText().toString());
                                    innerAdapter.notifyDataSetChanged();
                                    Snackbar.make(parentActivity.coordinatorLayout, "Updated.", Snackbar.LENGTH_LONG)
                            .show();
                                    dialog.dismiss();
                                } else {
                                    Snackbar.make(parentActivity.coordinatorLayout, "Couldn't update Location.", Snackbar.LENGTH_LONG)
                            .show();
                                }*/

                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                // Cancel action
                                dialog.dismiss();
                            }
                        })
                        .setTitle("Merge Entries")
                        .setMessage("Do you really want to merge those two entries?")
                        .create();
                alertDialog.show();
            }
        };
    }

    @Override
    public int getItemViewType(int position) {
        if (showServiceNotRunningInfo() && position == getItemCount()-1) {
            return VIEWTYPE_SERVICEINFO;
        } else if (data.size() == 0 &&
                (position == getItemCount()-1 && !showServiceNotRunningInfo() ||
                position == getItemCount()-2 && showServiceNotRunningInfo())
                ) {
            return VIEWTYPE_NOENTRYINFO;
        } else {
            return VIEWTYPE_NORMAL;
        }
    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position < lastPosition){
            Animation animation = AnimationUtils.loadAnimation(this.context, R.anim.animation_bottom_top);
            // --> PROBLEM with following line: at position=100 it results into 10s offset. How to refresh offset after initial load?
            //animation.setStartOffset(position * 100);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        int extraInfoCards = 0;
        if(showServiceNotRunningInfo()){
            extraInfoCards++;
        }
        if(data.size() == 0){
            extraInfoCards++;
        }
        return data.size() + extraInfoCards;
    }

    private boolean showServiceNotRunningInfo(){
        return !((MainActivity)context).serviceStatus.isRunning();
    }

    private DatabaseHelper getDbHelper(Context context) {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }

}