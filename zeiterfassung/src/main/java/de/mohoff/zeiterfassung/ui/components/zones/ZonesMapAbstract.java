package de.mohoff.zeiterfassung.ui.components.zones;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.location.Address;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.helpers.GeneralHelper;
import de.mohoff.zeiterfassung.ui.components.MapAbstract;

/**
 * Created by moo on 10/9/15.
 */
public abstract class ZonesMapAbstract extends MapAbstract {
    protected static boolean SHOW_MAP_ANIMATIONS;

    SharedPreferences sp;

    LatLng lookupLatLng;

    Marker candidateMarker = null;
    Circle candidateCircle = null;
    int candidateColor;
    int newRadius;

    EditText radiusValue, addressValue;
    ImageButton searchButton;
    // TODO: Replace button with appropriate SAVE colorBarIcon


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

        // For some reason, we also need to asign the PreferenceManager here as well. That is also done in superclass MapAbstract
        sp = PreferenceManager.getDefaultSharedPreferences(context);

        // Handle setting 'Default Zone Radius'
        newRadius = Integer.parseInt(sp.getString(
                context.getString(R.string.setting_zones_default_radius),
                String.valueOf(context.getString(R.string.setting_zones_default_radius_default_value))
        ));

        context.getDrawerToggle().setDrawerIndicatorEnabled(false);
        context.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set FAB colorBarIcon and click listener
        context.fab.show();
        context.fab.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_save_black_24dp));

        //colorButtonDisabled = getResources().getColor(R.color.grey_25);
        //colorButtonEnabled = getResources().getColor(R.color.greenish);

        radiusValue = (EditText) view.findViewById(R.id.radiusValue);
        radiusValue.setText(String.valueOf(newRadius));
        radiusValue.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String inputString = radiusValue.getText().toString();
                try {
                    newRadius = Integer.valueOf(inputString);
                } catch (Exception e) {
                    Snackbar.make(
                            context.coordinatorLayout,
                            context.getString(R.string.error_input_no_number),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
                if (candidateCircle != null) {
                    candidateCircle.setRadius(newRadius);
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
                    Snackbar.make(
                            context.coordinatorLayout,
                            context.getString(R.string.error_input_geolookup_empty),
                            Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    try {
                        // Do the lookup
                        Address lookupResult = geocoder.getFromLocationName(addressValue.getText().toString(), 1)
                                .get(0); // isRunning(0) can cause IndexOutOfBoundException
                        lookupLatLng = new LatLng(lookupResult.getLatitude(), lookupResult.getLongitude());
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(lookupLatLng)
                                .draggable(true)
                                .title(context.getString(R.string.map_new_zone))
                                .icon(BitmapDescriptorFactory.fromBitmap(markerCandidateLocation));

                        // Add marker and circle for the lookup position to the map
                        candidateMarker = map.addMarker(markerOptions);

                        candidateCircle = map.addCircle(
                                createCircleOptions(colorCandidateLocationCircle)
                                        .center(lookupLatLng)
                                        .radius(newRadius)
                        );

                        // Move camera to lookup position
                        centerMapTo(lookupLatLng, 0, SHOW_MAP_ANIMATIONS);
                    } catch (IOException e) {
                        Snackbar.make(
                                context.coordinatorLayout,
                                context.getString(R.string.error_geolookup),
                                Snackbar.LENGTH_LONG)
                                .show();
                    } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                        Snackbar.make(
                                context.coordinatorLayout,
                                context.getString(R.string.error_input_geolookup_invalid),
                                Snackbar.LENGTH_LONG)
                                .show();
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
        googleMap.setPadding(0, 350, 0, 0);
        // Make sure we start with an empty map. After supporting proper back/up-navigation,
        // the variable googleMap doesn't represent an empty map after navigating to this fragment
        // more than once.
        googleMap.clear();
        // Inside drawExistingZones(), map center will be setIsRunning and animated. TODO: What ?! setIsRunning?
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
                if (candidateMarker != null && candidateCircle != null) {
                    candidateMarker.remove();
                    candidateCircle.remove();
                }

                candidateMarker = map.addMarker(new MarkerOptions()
                        .position(point)
                        .draggable(true)
                        .title(context.getString(R.string.map_new_zone))
                        .icon(BitmapDescriptorFactory.fromBitmap(markerCandidateLocation))
                );
                candidateCircle = map.addCircle(
                        createCircleOptions(colorCandidateLocationCircle).center(point).radius(newRadius)
                );
            }
        });
    }

    // If LocationService is running, it should retrieve updated list of Zones.
    protected void updateLocationServiceZones() {
        if (context.mService != null) {
            context.mService.updateAllZones();
        }
    }

    protected void goBackToManageZones() {
        // Clear back stack because we're done with adding or editing a Zone.
        GeneralHelper.clearBackStack(context);
        // Go back to Zones fragment with clean back stack
        Fragment nextFragment = new Zones();
        context.replaceFragment(nextFragment, false);
    }
}
