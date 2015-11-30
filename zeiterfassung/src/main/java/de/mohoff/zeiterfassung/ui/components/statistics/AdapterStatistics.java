package de.mohoff.zeiterfassung.ui.components.statistics;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.Stat;
import de.mohoff.zeiterfassung.datamodel.Timeslot;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.locationservice.LocationService;
import de.mohoff.zeiterfassung.locationservice.ServiceChangeListener;
import de.mohoff.zeiterfassung.ui.MainActivity;
import de.mohoff.zeiterfassung.ui.colorpicker.ColorPalette;

/**
 * Created by moo on 11/1/15.
 */
public class AdapterStatistics extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ServiceChangeListener, OnChartValueSelectedListener{
    private static int VIEWTYPE_BASIC = 0;
    private static int VIEWTYPE_CHART = 1;

    private DatabaseHelper dbHelper;
    private MainActivity context;
    private List<Stat> data;

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

    public static class StatBasic extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView headline;
        public TextView content;
        public StatBasic(View v) {
            super(v);
            headline = (TextView) v.findViewById(R.id.headline);
            content = (TextView) v.findViewById(R.id.content);
        }
    }

    public static class StatChart extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView headline;
        public PieChart chart;
        public StatChart(View v) {
            super(v);
            headline = (TextView) v.findViewById(R.id.headline);
            chart = (PieChart) v.findViewById(R.id.pieChart);
        }
    }

    public AdapterStatistics(Context context, List<Stat> data) {
        getDbHelper();
        this.context = (MainActivity)context;
        this.data = data;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        if(viewType == VIEWTYPE_BASIC){
            View itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.fragment_statistics_element_basic, parent, false);
            return new StatBasic(itemView);
        }
        if(viewType == VIEWTYPE_CHART){
            View itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.fragment_statistics_element_chart, parent, false);
            return new StatChart(itemView);
        }
        return null;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        // TODO: Refactor here. Too dirty
        if(position >= 3) position -= 1;
        final Stat stat = data.get(position);

        if(holder.getItemViewType() == VIEWTYPE_BASIC) {
            StatBasic statHolder = (StatBasic) holder;
            statHolder.headline.setText(stat.getDisplayString());
            statHolder.content.setText(stat.getDisplayValue());

            if(stat.getDisplayValue().equals("n.a.")){
                statHolder.content.setTypeface(null, Typeface.ITALIC);
            } else {
                statHolder.content.setTypeface(null, Typeface.NORMAL);
            }

            // Update serviceUptime entry every second if background service is running
            if(stat.getIdentifier().equals("serviceUptime")){
                setupTextViewUpdater(statHolder.content, stat);
            }
            if(stat.getIdentifier().equals("distanceTravelled")){
                statHolder.content.setText(stat.getDisplayValueWithExtraTime(LocationService.SESSION_DISTANCE));
            }
        }

        if(holder.getItemViewType() == VIEWTYPE_CHART) {
            StatChart statHolder = (StatChart) holder;
            statHolder.headline.setText(stat.getDisplayString());

            statHolder.chart.setData(getInitialChartData());
            statHolder.chart.setNoDataTextDescription("No tracking information available yet.");
            statHolder.chart.setUsePercentValues(true); // true
            statHolder.chart.setDescription("");
            statHolder.chart.setExtraOffsets(10, 20, 10, 5); // left, top, right, bottom
            statHolder.chart.setDragDecelerationFrictionCoef(0.95f);
            statHolder.chart.setDrawHoleEnabled(true);
            statHolder.chart.setHoleColorTransparent(true);
            statHolder.chart.setTransparentCircleRadius(0f); // 61f
            //statHolder.chart.setTransparentCircleColor(Color.BLUE);
            statHolder.chart.setTransparentCircleAlpha(0); // 110
            statHolder.chart.setHoleRadius(0f);
            statHolder.chart.setCenterTextSize(14f);
            statHolder.chart.setDrawCenterText(false); // true
            statHolder.chart.setRotationAngle(-90);

            statHolder.chart.setRotationEnabled(false); // Enables rotation of the chart by touch
            statHolder.chart.setHighlightPerTapEnabled(true);
            statHolder.chart.animateY(1400, Easing.EasingOption.EaseInOutQuad); // Shows loading animation
            statHolder.chart.getLegend().setEnabled(false);
            statHolder.chart.highlightValues(null);
            statHolder.chart.setOnChartValueSelectedListener(this);

            statHolder.chart.invalidate();
        }
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        // Timeslot.getReadableDuration takes millis as argument. Thus we first need to convert
        // Entry's minutes to milliseconds.
        String readableDuration = Timeslot.getReadableDuration(((long) e.getVal()) * 60 * 1000, true, false);
        String activity = e.getData().toString();
        Snackbar.make(
                context.coordinatorLayout,
                activity + ": " + readableDuration,
                Snackbar.LENGTH_LONG)
        .show();
    }

    @Override
    public void onNothingSelected() {

    }

    private PieData getInitialChartData(){
        HashMap<String, HashMap<String,Long>> map = dbHelper.getTimeSpentForEachZone();
        HashMap<String, Long> mapSum = new HashMap<>();

        for (Map.Entry<String, HashMap<String,Long>> entry : map.entrySet()) {
            String activity = entry.getKey();
            HashMap<String, Long> inner = entry.getValue();

            Long sum = Long.valueOf(0);
            for (Long locTime : inner.values()){
                sum += locTime;
            }
            mapSum.put(activity, sum);
        }

        List<Entry> timesData = new ArrayList<Entry>();
        List<String> activities = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Long> entry : mapSum.entrySet()) {
            // yValues (time duration)
            // Divide by (1000*60), so Entry only needs to stores 'minutes' instead of milliseconds.
            timesData.add(new Entry(
                    (float) entry.getValue()/(1000*60),
                    i++,
                    entry.getKey()
            ));
            // xValues (Activity name)
            activities.add(entry.getKey());
        }

        PieDataSet dataSet = new PieDataSet(timesData, "");

        // Add colors from ColorPalette.GREENISH. If more colors need than there are in the array,
        // start repeating colors.
        int[] colors = new int[timesData.size()];
        for(int j=0; j<timesData.size(); j++){
            colors[j] = ColorPalette.GREENISH[j%(ColorPalette.GREENISH.length)];
        }
        dataSet.setColors(colors);
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(6f);
        dataSet.setValueFormatter(new PercentFormatter());

        PieData data = new PieData(activities, dataSet);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(12f);
        return data;
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
        // +1 because of the additional chart which is not stored in List data.
        return data.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 2) {
            return VIEWTYPE_CHART;
        } else {
            return VIEWTYPE_BASIC;
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
