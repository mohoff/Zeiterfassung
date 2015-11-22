package de.mohoff.zeiterfassung.ui.components.zones;

import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class ManageZones extends Fragment {
    private MainActivity context;
    private DatabaseHelper dbHelper = null;
    private RecyclerView recyclerView;
    private LinearLayoutManager linLayoutManager;
    private ManageZonesAdapter adapter;

    // TODO: add kind of tutorial: alertDialog which explains that user has to longclick Zones in order to edit them. With checkbox "Don't show tipp again"


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_zones, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(false);
        recyclerView.setClickable(false); // taken from old listView. Can use here?

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = (MainActivity) getActivity();

        // use a linear layout manager
        linLayoutManager = new LinearLayoutManager(context);
        linLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linLayoutManager);

        // specify an adapter (see also next example)
        adapter = new ManageZonesAdapter(context);
        recyclerView.setAdapter(adapter);

        // Set FAB colorBarIcon and click listener
        context.fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_add_white_24dp));
        context.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment nextFragment = new AddZone();
                context.replaceFragment(nextFragment, true);
            }
        });
        context.fab.show();
    }

    @Override
    public void onResume() {
        // TODO: What's up with that snippet? Still needed in NavigationView? -- Current status: NEEDED!
        if(getFragmentManager().getBackStackEntryCount() > 0){
            ((MainActivity)getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(false);
        } else {
            ((MainActivity)getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(true);
        }
        super.onResume();
    }
}
