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
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.Zone;

/**
 * Created by moo on 8/16/15.
 */
public class EditZonesMap extends ManageZonesMapAbstract {
    int candidateZoneId;
    Zone editZone;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        candidateZoneId = getArguments().getInt(context.getString(R.string.arg_zone_id));
        editZone = dbHelper.getZoneById(candidateZoneId);
        candidateColor = editZone.getColor();
        // Overwrite newRadius which was previously assigned in super.onActivityCreated with value
        // from SharedPreferences.
        newRadius = editZone.getRadius();
        radiusValue.setText(String.valueOf(newRadius));

        context.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (candidateMarker == null) {
                    Snackbar.make(
                            context.coordinatorLayout,
                            getString(R.string.error_input_no_pin),
                            Snackbar.LENGTH_LONG)
                    .show();
                    return;
                }
                Loc newLoc = Loc.convertLatLngToLoc(candidateMarker.getPosition());
                if (newRadius < Zone.MIN_RADIUS) {
                    Snackbar.make(
                            context.coordinatorLayout,
                            getString(R.string.error_input_radius_min, Zone.MIN_RADIUS),
                            Snackbar.LENGTH_LONG)
                    .show();
                } else if (candidateMarker.getPosition().equals(editZone.getLatLng()) &&
                        newRadius == editZone.getRadius()) {
                    Snackbar.make(
                            context.coordinatorLayout,
                            getString(R.string.error_input_pin_equal),
                            Snackbar.LENGTH_LONG)
                        .show();
                } else if (dbHelper.isIntersectingOtherZone(newLoc, newRadius, candidateZoneId)) {
                    Snackbar.make(
                            context.coordinatorLayout,
                            getString(R.string.error_input_pin_intersect),
                            Snackbar.LENGTH_LONG)
                    .show();
                } else {
                    editZone.setRadius(newRadius);
                    editZone.setRadius((int) candidateCircle.getRadius());
                    if (dbHelper.updateZone(editZone) == 1) {
                        Snackbar.make(
                                context.coordinatorLayout,
                                context.getString(R.string.update_zone_success),
                                Snackbar.LENGTH_LONG)
                        .show();
                        goBackToManageZones();
                    } else {
                        Snackbar.make(
                                context.coordinatorLayout,
                                context.getString(R.string.update_zone_failure),
                                Snackbar.LENGTH_LONG)
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
                centerMapTo(latLng, 0, SHOW_MAP_ANIMATIONS);
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

        // TODO: rework this for FAB color

        // Provide color feedback. Disable button if newRadius hasn't changed.
        /*if (newRadius == editZone.getRadius()) {
            saveButton.setColorFilter(colorButtonDisabled);
            //saveButton.setClickable(false);
        } else {
            saveButton.setColorFilter(colorButtonEnabled);
            //saveButton.setClickable(true);
        }*/
    }
}
