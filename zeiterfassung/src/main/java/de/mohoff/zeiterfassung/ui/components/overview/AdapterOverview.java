package de.mohoff.zeiterfassung.ui.components.overview;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.ArrayList;
import java.util.HashMap;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.Timeslot;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;
import de.mohoff.zeiterfassung.ui.components.settings.Settings;

public class AdapterOverview extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private DatabaseHelper dbHelper = null;
    private ArrayList<Timeslot> data = new ArrayList<Timeslot>();
    private int lastPosition = 99999;
    private MainActivity context;
    private HashMap<String, String> summedTime; // Activity --> readableTime

    SharedPreferences sp;
    private boolean showActivityFirst, showColorIndicator, showTimeContextInfo;
    private String timeContextInfoInterval;

    private final static int VIEWTYPE_NORMAL = 1;
    private final static int VIEWTYPE_SERVICEINFO = 2;
    private final static int VIEWTYPE_NOENTRYINFO = 3;


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolderItem extends RecyclerView.ViewHolder {
        public Context context;
        // each data item is just a string in this case
        public CardView container;

        public View colorBar;
        public RelativeLayout colorBarContainer;
        public ImageView colorBarIcon;

        public TextView timeContextInfo;
        public LinearLayout timeContextInfoWrapper;

        public TextView firstLine, secondLine;
        public TextView startTime, startDate;
        public TextView endTime, endDate;
        public TextView duration;
        //public LongClickListener longClickListener;

        //public RelativeLayout topConnectorPart, bottomConnectorPart;
        //public View middleConnectorPart;

        public ViewHolderItem(Context context, View v) {
            super(v);
            this.context = context;
            this.container = (CardView) v.findViewById(R.id.card_view);

            this.colorBar = v.findViewById(R.id.colorBar);
            this.colorBarContainer = (RelativeLayout) v.findViewById(R.id.colorBarContainer);
            this.colorBarIcon = (ImageView) v.findViewById(R.id.colorBarIcon);

            this.timeContextInfo = (TextView) v.findViewById(R.id.timeContextInfo);
            this.timeContextInfoWrapper = (LinearLayout) v.findViewById(R.id.timeContextInfoWrapper);

            this.firstLine = (TextView) v.findViewById(R.id.firstLine);
            this.secondLine = (TextView) v.findViewById(R.id.secondLine);
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

        public ViewHolderInfoItem(View v) {
            super(v);
            this.container = (CardView) v.findViewById(R.id.card_view);
            this.infoText = (TextView) v.findViewById(R.id.infoText);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterOverview(Context ctx) {
        getDbHelper(context);
        context = (MainActivity) ctx;
        data.addAll(dbHelper.getAllTimeslots());
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        showActivityFirst = sp.getBoolean(
                context.getString(R.string.setting_appearance_label_order),
                Boolean.valueOf(context.getString(R.string.setting_appearance_label_order_default_value))
        );
        showColorIndicator = sp.getBoolean(
                context.getString(R.string.setting_appearance_color_indicator),
                Boolean.valueOf(context.getString(R.string.setting_appearance_color_indicator_default_value))
        );
        showTimeContextInfo = sp.getBoolean(
                context.getString(R.string.setting_appearance_extra_info),
                Boolean.valueOf(context.getString(R.string.setting_appearance_extra_info_default_value))
        );
        if (showTimeContextInfo) {
            int index = Integer.parseInt(sp.getString(
                    context.getString(R.string.setting_appearance_extra_info_detail),
                    String.valueOf(context.getString(R.string.setting_appearance_extra_info_detail_default_value))
            ));
            timeContextInfoInterval = context.getResources().getStringArray(R.array.extraInfoEntries)[index];
            long starttime = Settings.getTimeInPastForArrayIndex(context, index);
            // When user selected 'Yesterday', endtime is not System.currentTimeMillis() and we need
            // to assign an appropriate endtime.
            long endtime = index == 1 ? Settings.getTimeInPastForArrayIndex(context, 0) : System.currentTimeMillis();
            summedTime = dbHelper.getSummedTimeForActivities(data, starttime, endtime);
        }

    }

    public void updateData() {
        data.clear();
        data.addAll(dbHelper.getAllTimeslots());
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEWTYPE_NORMAL) {
            View itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.fragment_overview_card, parent, false);
            return new ViewHolderItem(context, itemView);
        }
        if ((viewType == VIEWTYPE_SERVICEINFO) || (viewType == VIEWTYPE_NOENTRYINFO)) {
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
        if (holder.getItemViewType() == VIEWTYPE_NORMAL) {
            ViewHolderItem vh = (ViewHolderItem) holder;
            Timeslot timeslot = data.get(position);

            vh.container.setCardBackgroundColor(context.getResources().getColor(R.color.white));
            if (timeslot.getReadableEndTime(context).equals(context.getString(R.string.overview_end_pending)) &&
                    timeslot.getReadableEndDate(context).equals(context.getString(R.string.overview_end_pending))) {
                vh.endTime.setTypeface(null, Typeface.ITALIC);
                vh.endDate.setTypeface(null, Typeface.ITALIC);
            } else {
                vh.endTime.setTypeface(null, Typeface.NORMAL);
                vh.endDate.setTypeface(null, Typeface.NORMAL);
            }

            // Handle setting 'Color Indicator'
            if (showColorIndicator) {
                vh.colorBarContainer.setVisibility(View.VISIBLE);
                vh.colorBar.setBackground(null);
                vh.colorBar.setBackgroundColor(timeslot.getZone().getColor());
                vh.colorBarIcon.setImageResource(R.drawable.ic_action_edit_location);
            } else {
                // Set View.INVISIBLE here so layout remains the same withouth the color indicator.
                vh.colorBarContainer.setVisibility(View.INVISIBLE);
            }

            // Handle setting 'Timeslot Context Info'
            if (showTimeContextInfo) {
                vh.timeContextInfoWrapper.setVisibility(View.VISIBLE);
                String extraInfo = timeContextInfoInterval + ": " + summedTime.get(timeslot.getZone().getActivityName());
                vh.timeContextInfo.setText(extraInfo);
            } else {
                // Set View.GONE here so space can be used by other views.
                vh.timeContextInfoWrapper.setVisibility(View.GONE);
            }

            // Handle setting 'Label Order'
            if (showActivityFirst) {
                vh.firstLine.setText(timeslot.getZone().getActivityName());
                vh.secondLine.setText(timeslot.getZone().getLocationName());
            } else {
                vh.firstLine.setText(timeslot.getZone().getLocationName());
                vh.secondLine.setText(timeslot.getZone().getActivityName());
            }

            // In order to draw outside of cardView with clipChildren and clipToParent, we have to
            // setIsRunning setClipToOutline(false). This sadly is v21+.
            //vh.container.setClipToOutline(false);

            vh.startTime.setText(timeslot.getReadableStartTime());
            vh.startDate.setText(timeslot.getReadableStartDate());
            vh.endTime.setText(timeslot.getReadableEndTime(context));
            vh.endDate.setText(timeslot.getReadableEndDate(context));
            vh.duration.setText(timeslot.getReadableDuration(true, true)); // e.g. "1d 2h 14min"

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

        } else if (holder.getItemViewType() == VIEWTYPE_SERVICEINFO) {
            ViewHolderInfoItem ivh = (ViewHolderInfoItem) holder;
            ivh.infoText.setText(context.getString(R.string.service_not_running));
        } else if (holder.getItemViewType() == VIEWTYPE_NOENTRYINFO) {
            ViewHolderInfoItem ivh = (ViewHolderInfoItem) holder;
            ivh.infoText.setText(context.getString(R.string.overview_no_entries));
        }
    }

    private View.OnClickListener getOnMergeClickListener() {
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
                                    Snackbar.make(context.coordinatorLayout, "Updated.", Snackbar.LENGTH_LONG)
                            .show();
                                    dialog.dismiss();
                                } else {
                                    Snackbar.make(context.coordinatorLayout, "Couldn't update Location.", Snackbar.LENGTH_LONG)
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
        if (showServiceNotRunningInfo() && position == getItemCount() - 1) {
            return VIEWTYPE_SERVICEINFO;
        } else if (data.size() == 0 &&
                (position == getItemCount() - 1 && !showServiceNotRunningInfo() ||
                        position == getItemCount() - 2 && showServiceNotRunningInfo())
                ) {
            return VIEWTYPE_NOENTRYINFO;
        } else {
            return VIEWTYPE_NORMAL;
        }
    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position < lastPosition) {
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
        if (showServiceNotRunningInfo()) {
            extraInfoCards++;
        }
        if (data.size() == 0) {
            extraInfoCards++;
        }
        return data.size() + extraInfoCards;
    }

    private boolean showServiceNotRunningInfo() {
        return !((MainActivity) context).serviceStatus.isRunning();
    }

    private DatabaseHelper getDbHelper(Context context) {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }

}