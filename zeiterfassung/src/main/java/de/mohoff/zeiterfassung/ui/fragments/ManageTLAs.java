package de.mohoff.zeiterfassung.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mohoff.zeiterfassung.ui.AdapterManageTLA;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class ManageTLAs extends Fragment {
    private DatabaseHelper dbHelper = null;
    private RecyclerView recyclerView;
    private LinearLayoutManager linLayoutManager;
    private AdapterManageTLA adapter;

    // TODO: add kind of tutorial: alertDialog which explains that user has to longclick Zones in order to edit them. With checkbox "Don't show tipp again"


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_tlas, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        // used when size of recyclerView doesn't change (can we use it here?)
        recyclerView.setHasFixedSize(false);

        // use a linear layout manager
        linLayoutManager = new LinearLayoutManager(getActivity());
        linLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linLayoutManager);

        // specify an adapter (see also next example)
        adapter = new AdapterManageTLA(getActivity());
        recyclerView.setAdapter(adapter);

        recyclerView.setClickable(false); // taken from old listView. Can use here?

        return view;
    }

    @Override
    public void onResume() {
        if(getFragmentManager().getBackStackEntryCount() > 0){
            ((MainActivity)getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(false);
        } else {
            ((MainActivity)getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(true);
        }
        super.onResume();
    }
}
