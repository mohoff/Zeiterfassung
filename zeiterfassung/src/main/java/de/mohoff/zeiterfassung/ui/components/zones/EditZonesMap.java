package de.mohoff.zeiterfassung.ui.components.zones;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import de.mohoff.zeiterfassung.R;
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
        // TODO: Does parentActivity reference work in onCreateView? (usually only in onActivityCreated()...)
        candidateZoneId = getArguments().getInt(parentActivity.getString(R.string.arg_zone_id));

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        editZone = dbHelper.getZoneById(candidateZoneId);
        candidateColor = editZone.getColor();
        radius = editZone.getRadius();
        radiusValue.setText(String.valueOf(radius));

        parentActivity.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (radius < Zone.MIN_RADIUS) {
                    Snackbar.make(parentActivity.coordinatorLayout, getString(R.string.error_input_radius_min, Zone.MIN_RADIUS), Snackbar.LENGTH_LONG)
                            .show();
                } else if (candidateMarker == null) {
                    Snackbar.make(parentActivity.coordinatorLayout, getString(R.string.error_input_no_pin), Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    // TODO: check if oldPos == newPos here. If so, showSnack "Position already saved"
                    if (radius == editZone.getRadius()) {
                        Snackbar.make(parentActivity.coordinatorLayout, getString(R.string.error_input_radius_equal), Snackbar.LENGTH_LONG)
                                .show();
                    } else {
                        // TODO: Check if entered radius is valid (> 50m && not near other Zones).
                        // TODO: execute save action on DB
                        Snackbar.make(parentActivity.coordinatorLayout, getString(R.string.update_zone_success), Snackbar.LENGTH_LONG)
                                .show();
                        // TODO: Make use of:
                        Snackbar.make(parentActivity.coordinatorLayout, getString(R.string.update_zone_failure), Snackbar.LENGTH_LONG)
                                .show();
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
