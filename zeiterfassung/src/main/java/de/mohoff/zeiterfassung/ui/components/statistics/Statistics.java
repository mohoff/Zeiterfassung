package de.mohoff.zeiterfassung.ui.components.statistics;

import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.List;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.Stat;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class Statistics extends Fragment {
    MainActivity parentActivity;
    DatabaseHelper dbHelper;
    View v;
    RecyclerView recList;
    RecyclerView.LayoutManager layoutManager;
    RecyclerView.Adapter adapter;

    List<Stat> stats;

    // TODO: add listeners for timeslotStarted, ZoneAdded/Deleted, so we can call notifyDataSetChanged() somewhere in here.

    public Statistics() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_statistics, container, false);

        recList = (RecyclerView) v.findViewById(R.id.recList);
        recList.setHasFixedSize(true);
        recList.setItemAnimator(new DefaultItemAnimator());

        return v;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        parentActivity = (MainActivity) getActivity();
        getDbHelper();
        parentActivity.fab.hide();

        layoutManager = new LinearLayoutManager(getActivity());
        recList.setLayoutManager(layoutManager);
        recList.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        dbHelper.initStatistics();
        dbHelper.updateStat("numberOfTimeslots");
        dbHelper.updateStat("numberOfZones");

        stats = dbHelper.getAllStats();
        adapter = new AdapterStatistics(parentActivity, stats);

        recList.setAdapter(adapter);
        recList.setLayoutManager(layoutManager);
    }

    @Override
    public void onResume() {
        super.onResume();
        //adapter.notifyDataSetChanged();
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(getActivity(), DatabaseHelper.class);
        }
        return dbHelper;
    }
}
