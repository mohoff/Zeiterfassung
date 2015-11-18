package de.mohoff.zeiterfassung.ui.components.statistics;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
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
public class AdapterStatistics extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ServiceChangeListener{
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
            statHolder.chart.setNoDataTextDescription("No tracking entries available yet.");
            statHolder.chart.setData(getInitialChartData());



            statHolder.chart.setUsePercentValues(false); // true
            statHolder.chart.setDescription("");
            statHolder.chart.setExtraOffsets(10, 20, 10, 10);
            statHolder.chart.setDragDecelerationFrictionCoef(0.95f);
            statHolder.chart.setDrawHoleEnabled(true);
            statHolder.chart.setHoleColorTransparent(true);
            statHolder.chart.setTransparentCircleRadius(0f); // 61f
            //statHolder.chart.setTransparentCircleColor(Color.BLUE);
            statHolder.chart.setTransparentCircleAlpha(0); // 110
            statHolder.chart.setHoleRadius(0f);
            statHolder.chart.setCenterTextSize(14f);

            statHolder.chart.setDrawCenterText(true);
            statHolder.chart.setRotationAngle(-90);
            // enable rotation of the chart by touch
            statHolder.chart.setRotationEnabled(false); // true
            statHolder.chart.setHighlightPerTapEnabled(true);
            statHolder.chart.animateY(1400, Easing.EasingOption.EaseInOutQuad);

            Legend l = statHolder.chart.getLegend();
            l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART);

            //Paint info = statHolder.chart.getPaint(Chart.PAINT_LEGEND_LABEL);
            //info.setColor(0xFFFFFFFF);
            //info.setTextSize(16);


            /*XAxis xAxis = statHolder.chart.ge
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextSize(10f);
            xAxis.setTextColor(Color.RED);*/

            statHolder.chart.highlightValues(null);

            statHolder.chart.invalidate();


            /*// Some PieChart methods:
            invalidate()                        Refresh/redraw
            notifyDataSetChanged()              Underlying data changed. Needed when adding data dynamically
            setLogEnabled(boolean)              Enables logging
            setMaxVisibleValueCount(int)        Sets the number of maximum visible drawn value-labels on the chart. This only takes affect when setDrawValues() is enabled.

            /* // PieDataSet methods:
            setSliceSpace(float degrees): Sets the space that is left out between the piechart-slices, default: 0° --> no space, maximum 45, minimum 0 (no space)
            setSelectionShift(float shift): Sets the distance the highlighted piechart-slice of this DataSet is "shifted" away from the center of the chart, default 12f

             */



        }
    }

    private PieData getInitialChartData(){
        HashMap<String, HashMap<String,Long>> map = dbHelper.getTimeSpentForEachZone();
        HashMap<String, Long> mapSum = new HashMap<>();

        for (Map.Entry<String, HashMap<String,Long>> entry : map.entrySet()) {
            String key = entry.getKey();
            HashMap<String, Long> inner = entry.getValue();

            Long sum = Long.valueOf(0);
            for (Long locTime : inner.values()){
                sum += locTime;
            }
            mapSum.put(key, sum);
        }

        List<Entry> timesData = new ArrayList<Entry>();
        int i = 0;
        for (Long timePerActivity : mapSum.values()) {
            // Divide by (1000*60), so Entry only needs to stores 'minutes' instead of milliseconds.
            timesData.add(new Entry((float) timePerActivity/(1000*60), i++));
        }
        //timesData.addAll(timesData);

        List<String> activities = new ArrayList<>(map.keySet());
        PieDataSet dataSet = new PieDataSet(timesData, "");
        dataSet.setColors(ColorPalette.NORMAL);
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(6f);

        return new PieData(activities, dataSet);
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
