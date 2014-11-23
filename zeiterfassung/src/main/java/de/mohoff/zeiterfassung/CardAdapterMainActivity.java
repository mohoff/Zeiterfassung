package de.mohoff.zeiterfassung;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import de.mohoff.zeiterfassung.datamodel.Timeslot;

public class CardAdapterMainActivity extends RecyclerView.Adapter<CardAdapterMainActivity.ViewHolder> {
    private ArrayList<Timeslot> dummyData = new ArrayList<Timeslot>();

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView icon;
        public TextView activity;
        public TextView location;
        public TextView startTime;
        public TextView duration;
        public TextView endTime;

        public ViewHolder(View v){
            super(v);
            this.icon = (ImageView) v.findViewById(R.id.icon);
            this.activity = (TextView) v.findViewById(R.id.activity);
            this.location = (TextView) v.findViewById(R.id.location);
            this.startTime = (TextView) v.findViewById(R.id.startTime);
            this.duration = (TextView) v.findViewById(R.id.duration);
            this.endTime = (TextView) v.findViewById(R.id.endTime);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public CardAdapterMainActivity() {
        //this.dummyData = dummyData;
        this.dummyData.add(new Timeslot(1416759002267L, 1416760002267L, "activity1", "act1, location1"));
        this.dummyData.add(new Timeslot(1416754002267L, 1416758002267L, "activity2", "act2, location1"));
    }

    // Create new views (invoked by the layout manager)
    @Override
    public CardAdapterMainActivity.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
        inflate(R.layout.activity_main_cards, parent, false);
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
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.icon.setImageResource(R.drawable.ic_action_edit_location);
        holder.activity.setText(dummyData.get(position).getActivity());
        holder.location.setText(dummyData.get(position).getLocation());
        holder.startTime.setText(Timeslot.getTimeReadableTime(dummyData.get(position).getStarttime()));
        holder.endTime.setText(Timeslot.getTimeReadableTime(dummyData.get(position).getEndtime()));
        holder.duration.setText(Timeslot.getDurationReadable(dummyData.get(position).getStarttime(), dummyData.get(position).getEndtime())); // e.g. "1d 2h 14min"
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dummyData.size();
    }
}