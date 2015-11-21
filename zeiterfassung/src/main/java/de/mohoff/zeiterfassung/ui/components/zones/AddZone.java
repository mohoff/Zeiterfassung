package de.mohoff.zeiterfassung.ui.components.zones;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.Snackbar;
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

import de.mohoff.zeiterfassung.helpers.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.helpers.DatabaseHelper;
import de.mohoff.zeiterfassung.ui.MainActivity;
import de.mohoff.zeiterfassung.ui.colorpicker.ColorPicker;

public class AddZone extends Fragment {
    private MainActivity parentActivity;
    private DatabaseHelper dbHelper = null;
    private ArrayAdapter adapter;
    private AutoCompleteTextView autoCompleteTextView;
    private EditText editText;
    private ColorPicker colorPicker;
    private CardView pinButton;
    private String activityNames[];

    private String inputActivityName = "";
    private String inputLocationName = "";

    public AddZone() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // needed to indicate that the fragment would
        // like to add items to the Options Menu
        setHasOptionsMenu(true);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_zone, container, false);

        autoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.inputActivity);
        editText = (EditText) view.findViewById(R.id.inputLocation);
        colorPicker = (ColorPicker) view.findViewById(R.id.colorPicker);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        parentActivity = (MainActivity) getActivity();
        parentActivity.getDrawerToggle().setDrawerIndicatorEnabled(false);
        parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = getDbHelper(parentActivity);
        // Get distinct Activity names to suggest them while typing in AutoCompleteTextView
        List<String> activityList = dbHelper.getDistinctActivityNames();
        activityNames = activityList.toArray(new String[activityList.size()]);

        adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, activityNames);

        // If activityName is passed, use it and disable corresponding AutoCompleteTextView.
        // If not passed, enable AutoCompleteTextView
        if(getArguments() != null && getArguments().containsKey(getString(R.string.arg_activity))){
            inputActivityName = getArguments().getString(getString(R.string.arg_activity));
            autoCompleteTextView.setText(inputActivityName);
            autoCompleteTextView.setEnabled(false);
        } else {
            autoCompleteTextView.setAdapter(adapter);
            autoCompleteTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    AutoCompleteTextView tv = (AutoCompleteTextView) v;
                    if(!hasFocus) {
                        inputActivityName = tv.getText().toString();
                        GeneralHelper.hideSoftKeyboardWithView(parentActivity, v);
                    }
                }
            });
        }

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                EditText et = (EditText) v;
                if(!hasFocus) {
                    inputLocationName = et.getText().toString();
                    GeneralHelper.hideSoftKeyboardWithView(parentActivity, v);
                }
            }
        });

        // Set FAB colorBarIcon and click listener
        parentActivity.fab.show();
        parentActivity.fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_chevron_right_white_24dp));
        parentActivity.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoCompleteTextView.clearFocus();
                editText.clearFocus();
                if (inputActivityName.equals("") || inputLocationName.equals("")) {
                    Snackbar.make(parentActivity.coordinatorLayout, getString(R.string.error_input_names), Snackbar.LENGTH_LONG)
                            .show();
                } else if(!colorPicker.isColorSelected()){
                    Snackbar.make(parentActivity.coordinatorLayout, getString(R.string.error_input_color), Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    Fragment nextFragment = new AddZoneMap();
                    // pass ZoneId to map fragment
                    Bundle args = new Bundle();
                    args.putString(getString(R.string.arg_activity), inputActivityName);
                    args.putString(getString(R.string.arg_location), inputLocationName);
                    args.putInt(getString(R.string.arg_color), colorPicker.getSelectedColor());
                    nextFragment.setArguments(args);

                    parentActivity.replaceFragment(nextFragment, true);
                }
            }
        });

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
