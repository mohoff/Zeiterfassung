package de.mohoff.zeiterfassung.ui;

import android.content.Context;
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
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.Timeslot;

public class CardAdapterOverview extends RecyclerView.Adapter<CardAdapterOverview.ViewHolder> {
    private DatabaseHelper dbHelper = null;
    private ArrayList<Timeslot> data = new ArrayList<Timeslot>();
    private int lastPosition = 99999;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public CardView container;
        public ImageView icon;
        public TextView activity;
        public TextView location;
        public TextView startTime;
        public TextView duration;
        public TextView endTime;

        public ViewHolder(View v){
            super(v);
            this.container = (CardView) v.findViewById(R.id.card_view);
            this.icon = (ImageView) v.findViewById(R.id.icon);
            this.activity = (TextView) v.findViewById(R.id.activity);
            this.location = (TextView) v.findViewById(R.id.location);
            this.startTime = (TextView) v.findViewById(R.id.startTime);
            this.duration = (TextView) v.findViewById(R.id.duration);
            this.endTime = (TextView) v.findViewById(R.id.endTime);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public CardAdapterOverview(Context context) {
        getDbHelper();
        this.context = context;
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

        this.data.addAll(dbHelper.getAllTimeslots());
    }

    // Create new views (invoked by the layout manager)
    @Override
    public CardAdapterOverview.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
        inflate(R.layout.activity_main_cards_additionalinfo, parent, false);
        return new ViewHolder(itemView);

        /*
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_view, parent, false);
            // set the view's size, margins, paddings and layout parameters
            ...
            ViewHolder vh = new ViewHolder(v);
            return vh;
        */
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Timeslot timeslot = data.get(position);
        if(position < 10){
            // set a background color for dummy data
            holder.container.setCardBackgroundColor(context.getResources().getColor(R.color.greenish_5));
        } else {
            holder.container.setCardBackgroundColor(context.getResources().getColor(R.color.white));
        }

        holder.icon.setImageResource(R.drawable.ic_action_edit_location);
        holder.activity.setText(timeslot.getActivity());
        holder.location.setText(timeslot.getLocation());
        holder.startTime.setText(timeslot.getReadableStartTime());
        holder.endTime.setText(timeslot.getReadableEndTime());
        holder.duration.setText(timeslot.getReadableDuration()); // e.g. "1d 2h 14min"

        setAnimation(holder.container, position);
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
        return data.size();
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }

}