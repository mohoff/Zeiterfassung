package de.mohoff.zeiterfassung.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;
import de.mohoff.zeiterfassung.ui.MainActivity;

/**
 * Created by moo on 10/9/15.
 */
public class MapAddTLA extends MapAbstract {
    View v;

    ArrayList<TargetLocationArea> areas = new ArrayList<TargetLocationArea>();
    ArrayList<Marker> fixMarkers = new ArrayList<Marker>();
    ArrayList<Circle> fixCircles = new ArrayList<Circle>();
    private LatLng cameraCenter;
    LatLng lookupLatLng;

    TargetLocationArea candidateTLA;
    Marker candidateMarker = null;
    Circle candidateCircle = null;
    int radius = 100;
    EditText radiusValue;
    EditText addressValue;
    ImageButton searchButton;
    FloatingActionButton saveButton;
    // TODO: Replace button with appropriate SAVE icon

    int colorButtonDisabled;
    int colorButtonEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Do we have to add isAdded() here?
        //MainActivity main = (MainActivity) getActivity();
        parentActivity.getDrawerToggle().setDrawerIndicatorEnabled(false);
        parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = super.onCreateViewWithLayout(inflater, container, savedInstanceState, R.layout.fragment_map_add_tla);
        super.getDbHelper(getActivity());

        radiusValue = (EditText) v.findViewById(R.id.radiusValue);
        radiusValue.setText(String.valueOf(radius));
        radiusValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String inputString = radiusValue.getText().toString();
                try {
                    radius = Integer.valueOf(inputString);
                } catch (Exception e) {
                    // TODO: show Toast: "Input is not a number."
                }
                if (candidateCircle != null) {
                    candidateCircle.setRadius(radius);
                }
                //updateButtonColor();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        addressValue = (EditText) v.findViewById(R.id.addressValue);

        searchButton = (ImageButton) v.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(addressValue.getText() == null){
                    GeneralHelper.showToast(parentActivity, "Please enter address or place first.");
                } else {
                    try {
                        // Do the lookup
                        Address lookupResult = geocoder.getFromLocationName(addressValue.getText().toString(), 1).get(0);
                        lookupLatLng = new LatLng(lookupResult.getLatitude(), lookupResult.getLongitude());
                        // Add marker and circle for the lookup position to the map
                        candidateMarker = map.addMarker(
                                createMarkerOptions(lookupLatLng, true, "", "", BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        );
                        candidateCircle = map.addCircle(
                                createCircleOptions(lookupLatLng, radius, Color.argb(100, 81, 112, 226))
                        );
                        // Move camera to lookup position
                        centerMapTo(lookupLatLng);
                    } catch (IOException e){
                        GeneralHelper.showToast(parentActivity, "Lookup failed. Do you have internet connection?");
                    } catch (IllegalArgumentException e) {
                        GeneralHelper.showToast(parentActivity, "Lookup failed. Please enter a valid address or place.");
                    }
                }
            }
        });

        saveButton = (FloatingActionButton) v.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (radius < 50) {
                    GeneralHelper.showToast(getActivity(), "Input must be >= 50 meters.");
                    // cancel save process
                } else {
                    GeneralHelper.showToast(getActivity(), "Successfully saved.");
                    // TODO: Check if entered radius is valid (> 50m && not near other TLAs).
                    // TODO: Save new radius into DB.
                }

            }
        });
        //updateButtonColor();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
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

    private void updateButtonColor(){

        // TODO: rework this

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
                addressValue.clearFocus();
                GeneralHelper.hideSoftKeyboard(getActivity());

                // In case there is a marker and circle on the map already, remove them both.
                if(candidateMarker != null && candidateCircle != null){
                    candidateMarker.remove();
                    candidateCircle.remove();
                }

                candidateMarker = map.addMarker(
                        createMarkerOptions(point, true, "", "", BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                );
                candidateCircle = map.addCircle(
                        createCircleOptions(point, radius, Color.argb(100, 81, 112, 226))
                );
            }
        });
    }

    public void drawExistingTLAs(){
         for(TargetLocationArea tla : dbHelper.getAllTLAs()){

              int circleColor = Color.HSVToColor(100, new float[]{BitmapDescriptorFactory.HUE_ROSE, 1, 1});
              LatLng latLng = new LatLng(tla.getLatitude(), tla.getLongitude());

              // TODO: we want gray markers (custom markers) and gray fill colors to show that. Via .icon() ?

              Marker marker = map.addMarker(
                    createMarkerOptions(latLng, false, "", "", BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
              );
              Circle circle = map.addCircle(
                    createCircleOptions(latLng, tla.getRadius(), circleColor)
              );
              fixMarkers.add(marker);
              fixCircles.add(circle);

             // TODO: rework cameraCenter so the map viewport is wrapping all TLAs
             cameraCenter = new LatLng(tla.getLatitude(), tla.getLongitude());
        }

    }
}
