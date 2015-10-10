package de.mohoff.zeiterfassung.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.List;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;

public class AddTLA extends Fragment {
    private MainActivity context;
    private DatabaseHelper dbHelper = null;
    private String activityNames[];

    private String inputActivityName = "";
    private String inputLocationName = "";

    public AddTLA() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        context = (MainActivity) getActivity();
        context.getDrawerToggle().setDrawerIndicatorEnabled(false);
        context.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // needed to indicate that the fragment would
        // like to add items to the Options Menu
        setHasOptionsMenu(true);

        dbHelper = getDbHelper(context);
        // Get distinct Activity names to suggest them while typing in AutoCompleteTextView
        List<String> activityList = dbHelper.getDistinctActivityNames();
        activityNames = activityList.toArray(new String[activityList.size()]);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_tla, container, false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, activityNames);
        final AutoCompleteTextView autoCompleteView = (AutoCompleteTextView) view.findViewById(R.id.inputActivity);
        autoCompleteView.setAdapter(adapter);
        autoCompleteView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                AutoCompleteTextView tv = (AutoCompleteTextView) v;
                if (!hasFocus) {
                    inputActivityName = tv.getText().toString();
                    GeneralHelper.hideSoftKeyboardWithView(context, v);
                }
            }
        });

        final EditText editText = (EditText) view.findViewById(R.id.inputLocation);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                EditText et = (EditText) v;
                if (!hasFocus) {
                    inputLocationName = et.getText().toString();
                    GeneralHelper.hideSoftKeyboardWithView(context, v);
                }
            }
        });

        CardView pinButton = (CardView) view.findViewById(R.id.buttonRight);
        pinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoCompleteView.clearFocus();
                editText.clearFocus();
                if (inputActivityName == "" || inputLocationName == "") {
                    GeneralHelper.showToast(context, "Please enter both names first.");
                } else {
                    Fragment nextFragment = new MapAddTLA();
                    // pass TLAId to map fragment
                    Bundle args = new Bundle();
                    args.putString("activityName", inputActivityName);
                    args.putString("locationName", inputLocationName);
                    nextFragment.setArguments(args);

                    context.replaceFragment(nextFragment, true);
                }
            }
        });

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Called when the up affordance/carat in actionbar is pressed
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private DatabaseHelper getDbHelper(Context context) {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return dbHelper;
    }
}
