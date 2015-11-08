package de.mohoff.zeiterfassung.ui.components.statistics;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.List;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.Stat;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.locationservice.LocationService;
import de.mohoff.zeiterfassung.locationservice.ServiceChangeListener;
import de.mohoff.zeiterfassung.ui.MainActivity;

/**
 * Created by moo on 11/1/15.
 */
public class AdapterStatistics extends RecyclerView.Adapter<AdapterStatistics.ViewHolder> implements ServiceChangeListener{
    private DatabaseHelper dbHelper;
    private MainActivity context;
    private List<Stat> data;
    Thread t;

    private Handler uptimeHandler;
    private Runnable uptimeRunnable;

    @Override
    public void onServiceStatusEvent(boolean isRunning) {
        if(!isRunning) {
            cancelTextViewUpdater();
            data = dbHelper.getAllStats();
            notifyDataSetChanged();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView headline;
        public TextView content;
        public ViewHolder(View v) {
            super(v);
            headline = (TextView) v.findViewById(R.id.headline);
            content = (TextView) v.findViewById(R.id.content);
        }
    }

    public AdapterStatistics(Context context, List<Stat> data) {
        getDbHelper();
        this.context = (MainActivity)context;
        this.data = data;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AdapterStatistics.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_statistics_element, parent, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Stat stat = data.get(position);
        holder.headline.setText(stat.getDisplayString());
        holder.content.setText(stat.getDisplayValue());

        if(stat.getDisplayValue().equals("n.a.")){
            holder.content.setTypeface(null, Typeface.ITALIC);
        } else {
            holder.content.setTypeface(null, Typeface.NORMAL);
        }

        // Update serviceUptime entry every second if background service is running
        if(stat.getIdentifier().equals("serviceUptime")){
            setupTextViewUpdater(holder.content, stat);
        }
        if(stat.getIdentifier().equals("distanceTravelled")){
            holder.content.setText(stat.getDisplayValueWithExtraTime(LocationService.SESSION_DISTANCE));
        }
    }

    private void cancelTextViewUpdater(){
        // Cancel old thread
        if(uptimeHandler != null && uptimeRunnable != null){
            uptimeHandler.removeCallbacks(uptimeRunnable);
        }
    }

    private void setupTextViewUpdater(final TextView tv, final Stat uptimeStat){
        cancelTextViewUpdater();

        // Create new thread
        uptimeHandler = new Handler();
        uptimeRunnable = new Runnable() {
            @Override
            public void run() {
                if(LocationService.IS_SERVICE_RUNNING){
                    String computedValue = uptimeStat.getDisplayValueWithExtraTime(LocationService.getServiceSessionUptime());
                    tv.setText(computedValue);
                }
                uptimeHandler.postDelayed(this, 1000);
            }
        };
        uptimeHandler.post(uptimeRunnable);
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
