package de.mohoff.zeiterfassung.ui.components.zones;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import de.mohoff.zeiterfassung.datamodel.Zone;
import de.mohoff.zeiterfassung.helpers.GeneralHelper;

/**
 * Created by moo on 8/16/15.
 */
public class EditZonesMap extends ManageZonesMapAbstract {
    int candidateZoneId;
    Zone editZone;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        candidateZoneId = getArguments().getInt("ZoneId");

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        editZone = dbHelper.getZoneById(candidateZoneId);
        radius = editZone.getRadius();
        radiusValue.setText(String.valueOf(radius));

        parentActivity.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (radius < 50) {
                    GeneralHelper.showToast(getActivity(), "Input must be >= 50 meters.");
                    // cancel save process
                } else if (candidateMarker == null) {
                    GeneralHelper.showToast(getActivity(), "Please pin an area on the map first.");
                } else {
                    if (radius == editZone.getRadius()) {
                        GeneralHelper.showToast(getActivity(), "Input is already saved.");
                        // cancel save process
                    } else {
                        // TODO: Check if entered radius is valid (> 50m && not near other Zones).
                        // TODO: execute save action on DB
                        GeneralHelper.showToast(getActivity(), "Successfully saved.");
                    }
                }
            }
        });
    }

    @Override
    public void drawExistingZones(){
        super.drawExistingZones();
        for(Zone zone : dbHelper.getAllZones()) {
            Marker marker;
            Circle circle;
            LatLng latLng = new LatLng(zone.getLatitude(), zone.getLongitude());

            if (candidateZoneId == zone.get_id()) {
                marker = map.addMarker(
                        optionsCandidateMarker.position(latLng)
                );
                circle = map.addCircle(
                        optionsCandidateCircle.center(latLng).radius(zone.getRadius())
                );
                candidateMarker = marker;
                candidateCircle = circle;
                centerMapTo(latLng, 0);
            } else {
                marker = map.addMarker(
                        optionsFixMarker.position(latLng)
                );
                circle = map.addCircle(
                        optionsFixCircle.center(latLng).radius(zone.getRadius())
                );
                fixMarkers.add(marker);
                fixCircles.add(circle);
            }
        }
    }

    private void updateButtonColor(){

        // TODO: rework this

        // Provide color feedback. Disable button if radius hasn't changed.
        /*if (radius == editZone.getRadius()) {
            saveButton.setColorFilter(colorButtonDisabled);
            //saveButton.setClickable(false);
        } else {
            saveButton.setColorFilter(colorButtonEnabled);
            //saveButton.setClickable(true);
        }*/
    }
}
