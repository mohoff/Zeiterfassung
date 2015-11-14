package de.mohoff.zeiterfassung.ui.components.zones;

import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;

import de.mohoff.zeiterfassung.helpers.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.ui.components.MapAbstract;

/**
 * Created by moo on 10/9/15.
 */
public abstract class ManageZonesMapAbstract extends MapAbstract {
    ArrayList<Marker> fixMarkers = new ArrayList<Marker>();
    ArrayList<Circle> fixCircles = new ArrayList<Circle>();
    LatLng lookupLatLng;

    Marker candidateMarker = null;
    Circle candidateCircle = null;
    int candidateColor;
    int radius = 50;

    EditText radiusValue, addressValue;
    ImageButton searchButton;
    // TODO: Replace button with appropriate SAVE icon

    int colorButtonEnabled, colorButtonDisabled;

    MarkerOptions optionsFixMarker, optionsCandidateMarker;
    CircleOptions optionsFixCircle, optionsCandidateCircle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // needed to indicate that the fragment would
        // like to add items to the Options Menu
        setHasOptionsMenu(true);

        // TODO: To animate the drawer when switching fragments: https://github.com/keklikhasan/LDrawer
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateViewWithLayout(inflater, container, savedInstanceState, R.layout.fragment_manage_zones_map);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MapsInitializer.initialize(parentActivity);

        // TODO: Do we have to add isAdded() here?
        //MainActivity main = (MainActivity) getActivity();
        parentActivity.getDrawerToggle().setDrawerIndicatorEnabled(false);
        parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Set FAB icon and click listener
        parentActivity.fab.show();
        parentActivity.fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_save_black_24dp));

        colorButtonDisabled = getResources().getColor(R.color.grey_25);
        colorButtonEnabled = getResources().getColor(R.color.greenish);

        radiusValue = (EditText) view.findViewById(R.id.radiusValue);
        radiusValue.setText(String.valueOf(radius));
        radiusValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String inputString = radiusValue.getText().toString();
                try {
                    radius = Integer.valueOf(inputString);
                } catch (Exception e) {
                    //radiusValue.setText(String.valueOf(radius));
                    GeneralHelper.showToast(parentActivity, "Input is not a number.");
                }
                if (candidateCircle != null) {
                    candidateCircle.setRadius(radius);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        searchButton = (ImageButton) view.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (addressValue.getText() == null) {
                    GeneralHelper.showToast(parentActivity, "Please fill in address or place.");
                } else {
                    try {
                        // Do the lookup
                        Address lookupResult = geocoder.getFromLocationName(addressValue.getText().toString(), 1).get(0); // isRunning(0) can cause IndexOutOfBoundException
                        lookupLatLng = new LatLng(lookupResult.getLatitude(), lookupResult.getLongitude());
                        // Add marker and circle for the lookup position to the map
                        candidateMarker = map.addMarker(
                                createMarkerOptions(true, "", "", BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).position(lookupLatLng)
                        );
                        candidateCircle = map.addCircle(
                                createCircleOptions(Color.argb(100, 81, 112, 226)).center(lookupLatLng).radius(radius)
                        );
                        // Move camera to lookup position
                        centerMapTo(lookupLatLng, 0);
                    } catch (IOException e) {
                        GeneralHelper.showToast(parentActivity, "Lookup failed. Is internet connection available?");
                    } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                        GeneralHelper.showToast(parentActivity, "Lookup failed. Please enter valid address or place.");
                    }
                }
            }
        });

        addressValue = (EditText) view.findViewById(R.id.addressValue);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Called when the up arrow/carat in actionbar is pressed
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        super.onMapReady(googleMap);
        // Set top padding to move the compass button below the input area so it becomes visible.
        // TODO: Check if absolute pixel padding works out on other devices/resolutions.
        googleMap.setPadding(0,350,0,0);
        // Make sure we start with an empty map. After supporting proper back/up-navigation,
        // the variable googleMap doesn't represent an empty map after navigating to this fragment
        // more than once.
        googleMap.clear();
        // Inside drawExistingZones(), map center will be setIsRunning and animated.
        drawExistingZones();

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
                        createMarkerOptions(true, "", "", BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).position(point)
                );
                candidateCircle = map.addCircle(
                        createCircleOptions(Color.argb(100, 81, 112, 226)).center(point).radius(radius)
                );
            }
        });
    }

    public void drawExistingZones(){
        // Implementations need to assign cameraCenter!


        // initialize MarkerOptions
        optionsFixMarker = createMarkerOptions(false, "", "", BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        optionsFixCircle = createCircleOptions(Color.HSVToColor(100, new float[]{BitmapDescriptorFactory.HUE_ROSE, 1, 1}));
        optionsCandidateMarker = createMarkerOptions(true, "", "", BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        optionsCandidateCircle = createCircleOptions(Color.HSVToColor(100, new float[]{BitmapDescriptorFactory.HUE_RED, 1, 1}));
    }
}
