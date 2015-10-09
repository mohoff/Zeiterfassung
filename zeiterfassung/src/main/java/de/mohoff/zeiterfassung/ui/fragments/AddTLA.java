package de.mohoff.zeiterfassung.ui.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class AddTLA extends Fragment {
    private DatabaseHelper dbHelper = null;

    public AddTLA() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MainActivity main = (MainActivity) getActivity();
        main.getDrawerToggle().setDrawerIndicatorEnabled(false);
        main.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // needed to indicate that the fragment would
        // like to add items to the Options Menu
        setHasOptionsMenu(true);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_tla, container, false);
    }
}
