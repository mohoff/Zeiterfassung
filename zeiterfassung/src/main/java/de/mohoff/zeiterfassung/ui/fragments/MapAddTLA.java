package de.mohoff.zeiterfassung.ui.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
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

import java.io.IOException;
import java.util.ArrayList;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;

/**
 * Created by moo on 10/9/15.
 */
public class MapAddTLA extends MapManageTLAAbstract {
    String activityName, locationName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activityName = getArguments().getString("activityName");
        locationName = getArguments().getString("locationName");

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (radius < 50) {
                    GeneralHelper.showToast(getActivity(), "Input must be >= 50 meters.");
                    // cancel save process
                } else if (candidateMarker == null) {
                    GeneralHelper.showToast(getActivity(), "Please pin an area on the map first.");
                } else {
                    // TODO: Check if entered radius is not near other TLAs.
                    LatLng pos = candidateMarker.getPosition();
                    int result = dbHelper.createNewTLA(pos.latitude, pos.longitude, radius, activityName, locationName);
                    if (result != 1) {
                        GeneralHelper.showToast(getActivity(), "Couldn't add TLA. Does it already exist?");
                    } else {
                        GeneralHelper.showToast(getActivity(), "TLA successfully added.");
                        // Clear back stack because add-operation succeeded.
                        GeneralHelper.clearBackStack(getActivity());
                        // Go back to ManageTLAs fragment with clean back stack
                        Fragment nextFragment = new ManageTLAs();
                        parentActivity.replaceFragment(nextFragment, false);
                    }
                }
            }
        });
    }

    @Override
    public void drawExistingTLAs(){
        super.drawExistingTLAs();

        // TODO: provide appropriate colors: gray (uneditable) and greenish (editable)
        // TODO: we want gray markers (custom markers) and gray fill colors to show that. Via .icon() ?
        // TODO: rework camera center so viewport contains all markers.

        ArrayList<LatLng> latLngList = new ArrayList<>();

        for(TargetLocationArea tla : dbHelper.getAllTLAs()){
            LatLng latLng = new LatLng(tla.getLatitude(), tla.getLongitude());
            latLngList.add(latLng);
            Marker marker = map.addMarker(
                optionsFixMarker.position(latLng)
            );
            Circle circle = map.addCircle(
                optionsFixCircle.center(latLng).radius(tla.getRadius())
            );

            fixMarkers.add(marker);
            fixCircles.add(circle);
        }

        centerMapTo(Loc.getMapViewport(latLngList, 100));
    }
}
