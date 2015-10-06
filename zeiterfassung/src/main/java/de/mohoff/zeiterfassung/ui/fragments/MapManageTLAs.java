package de.mohoff.zeiterfassung.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;
import de.mohoff.zeiterfassung.ui.MainActivity;

/**
 * Created by moo on 8/16/15.
 */
public class MapManageTLAs extends MapAbstract {
    View v;

    ArrayList<TargetLocationArea> areas = new ArrayList<TargetLocationArea>();
    ArrayList<Marker> fixMarkers = new ArrayList<Marker>();
    ArrayList<Circle> fixCircles = new ArrayList<Circle>();
    private LatLng cameraCenter;

    int candidateTLAId;
    TargetLocationArea candidateTLA;
    Marker candidateMarker = null;
    Circle candidateCircle = null;
    int radius;
    EditText radiusValue;
    ImageButton saveButton;
    // TODO: Replace button with appropriate SAVE icon

    int colorButtonDisabled;
    int colorButtonEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MainActivity main = (MainActivity) getActivity();
        main.getDrawerToggle().setDrawerIndicatorEnabled(false);
        main.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // needed to indicate that the fragment would
        // like to add items to the Options Menu
        setHasOptionsMenu(true);

        // TODO: To animate the drawer when switching fragments: https://github.com/keklikhasan/LDrawer

        // work around for bug "fragment not attached to activity anymore"
        // Appears when navigation to this fragment the 2nd time.
        // The call of getResources() can't complete because of the bug.
        // TODO: Are there other ways to stay attached to activity?
        if(isAdded()){
            // getResources() only works when fragment is attached to activity. That is what we check
            // with isAdded().
            colorButtonDisabled = getResources().getColor(R.color.grey_25);
            colorButtonEnabled = getResources().getColor(R.color.greenish);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = super.onCreateViewWithLayout(inflater, container, savedInstanceState, R.layout.fragment_map_edittext);
        super.getDbHelper(getActivity());

        candidateTLAId = getArguments().getInt("TLAId");
        candidateTLA = dbHelper.getTLAById(candidateTLAId);
        radius = candidateTLA.getRadius();

        radiusValue = (EditText) v.findViewById(R.id.radiusValue);
        radiusValue.setText(String.valueOf(radius));
        radiusValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String inputString = radiusValue.getText().toString();
                try {
                    radius = Integer.valueOf(inputString);
                } catch (Exception e) {
                    radius = candidateTLA.getRadius();
                    // TODO: show Toast: "Input is not a number."
                }
                if (candidateCircle != null) {
                    candidateCircle.setRadius(radius);
                }
                updateButtonColor();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        saveButton = (ImageButton) v.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (radius == candidateTLA.getRadius()) {
                    GeneralHelper.showToast(getActivity(), "Input is already saved.");
                    // cancel save process
                } else if (radius < 50){
                    GeneralHelper.showToast(getActivity(), "Input must be >= 50 meters.");
                    // cancel save process
                } else {
                    GeneralHelper.showToast(getActivity(), "Successfully saved.");
                    // TODO: Check if entered radius is valid (> 50m && not near other TLAs).
                    // TODO: Save new radius into DB.
                }

            }
        });
        updateButtonColor();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get item selected and deal with it
        switch (item.getItemId()) {
            case android.R.id.home:
                //called when the up affordance/carat in actionbar is pressed
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateButtonColor(){
        // Provide color feedback. Disable button if radius hasn't changed.
        if (radius == candidateTLA.getRadius()) {
            saveButton.setColorFilter(colorButtonDisabled);
            //saveButton.setClickable(false);
        } else {
            saveButton.setColorFilter(colorButtonEnabled);
            //saveButton.setClickable(true);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);
        // Make sure we start with an empty map. After supporting proper back/up-navigation,
        // the variable googleMap doesn't represent an empty map after navigating to this fragment
        // more than once.
        googleMap.clear();
        // Inside drawExistingTLAs(), cameraCenter will be assigned.
        // Therefore it's necessary to call both methods in this order.
        drawExistingTLAs();
        centerMapTo(cameraCenter);

        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
                candidateCircle.setVisible(false);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                if (arg0.equals(candidateMarker)) {
                    candidateCircle.setCenter(arg0.getPosition());
                    candidateCircle.setVisible(true);
                }
            }

            @Override
            public void onMarkerDrag(Marker arg0) {
            }
        });

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                // Remove focus from EditText. Resets focus to first focusable element in layout
                // which would be this EditText again. By using android:focusable="true" and
                // android:focusableInTouchMode="true" for the parent layout, it absorbs the focus.
                radiusValue.clearFocus();
                // hide keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(radiusValue.getWindowToken(), 0);
            }
        });
    }

    public void drawExistingTLAs(){
        for(TargetLocationArea tla : dbHelper.getAllTLAs()){
            // TODO: provide appropriate colors: gray (uneditable) and greenish (editable)

            // Make marker and circle uneditable by default.
            // When TLA is the one to edit, make it editable (see below)
            boolean isDraggable = false;
            float markerHue = BitmapDescriptorFactory.HUE_ROSE;
            int circleColor = Color.HSVToColor(100, new float[]{BitmapDescriptorFactory.HUE_ROSE, 1, 1});

            if (candidateTLAId == tla.get_id()) {
                // make marker and circle editable
                isDraggable = true;
                markerHue = BitmapDescriptorFactory.HUE_RED;
                circleColor = Color.HSVToColor(100, new float[]{BitmapDescriptorFactory.HUE_RED, 1, 1});
                // assign camera center
                cameraCenter = new LatLng(tla.getLatitude(), tla.getLongitude());
            }

            LatLng latLng = new LatLng(tla.getLatitude(), tla.getLongitude());
            Marker marker = map.addMarker(new MarkerOptions()
                            .position(latLng)
                            .draggable(isDraggable)
                            .title(tla.getLocationName() + " @" + tla.getActivityName())
                            .snippet("latitude: " + tla.getLatitude() +
                                            "\nlongitude: " + tla.getLongitude() +
                                            "\nradius: " + tla.getRadius() +
                                            "\ncolorScheme: " + tla.getColorScheme() +
                                            "\n_id: " + tla.get_id()
                            )
                            .icon(BitmapDescriptorFactory.defaultMarker(markerHue))
            );
            CircleOptions circleOptions = new CircleOptions()
                    .center(latLng)
                    .radius(tla.getRadius())
                    .fillColor(circleColor)
                    //.fillColor(Color.argb(100, 81, 112, 226))
                    .strokeWidth(0)
                    .strokeColor(Color.TRANSPARENT)
                    ;
            Circle circle = map.addCircle(circleOptions);

            if (candidateTLAId == tla.get_id()) {
                candidateMarker = marker;
                candidateCircle = circle;
            } else {
                fixMarkers.add(marker);
                fixCircles.add(circle);
            }

        }
    }
}
