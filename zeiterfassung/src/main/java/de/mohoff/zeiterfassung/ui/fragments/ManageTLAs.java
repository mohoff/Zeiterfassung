package de.mohoff.zeiterfassung.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import de.mohoff.zeiterfassung.ui.ListAdapterTLA;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;

public class ManageTLAs extends Fragment {
    private DatabaseHelper dbHelper = null;
    ListAdapterTLA adapter;

    public ManageTLAs() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_tlas, container, false);

        adapter = new ListAdapterTLA(getActivity());
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        lv.setItemsCanFocus(true);
        lv.setClickable(false);
        lv.setAdapter(adapter);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
