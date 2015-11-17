package de.mohoff.zeiterfassung.ui.components.zones;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.Zone;
import de.mohoff.zeiterfassung.helpers.GeneralHelper;
import de.mohoff.zeiterfassung.ui.components.MapAbstract;

/**
 * Created by moo on 10/9/15.
 */
public class AddZoneMap extends ManageZonesMapAbstract {
    String activityName, locationName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activityName = getArguments().getString(getString(R.string.arg_activity));
        locationName = getArguments().getString(getString(R.string.arg_location));
        candidateColor = getArguments().getInt(getString(R.string.arg_color));

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) parentActivity.fab.getLayoutParams();
        //params.setAnchorId(R.id.topView);
        //parentActivity.fab.setLayoutParams(params);

        parentActivity.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newRadius < Zone.MIN_RADIUS) {
                    Snackbar.make(
                            parentActivity.coordinatorLayout,
                            getString(R.string.error_input_radius_min, Zone.MIN_RADIUS),
                            Snackbar.LENGTH_LONG)
                    .show();
                } else if (candidateMarker == null) {
                    Snackbar.make(
                            parentActivity.coordinatorLayout,
                            getString(R.string.error_input_no_pin),
                            Snackbar.LENGTH_LONG)
                    .show();
                } else {
                    // TODO: Check if entered newRadius is not near other Zones.
                    LatLng pos = candidateMarker.getPosition();
                    if (1 == dbHelper.createNewZone(
                            pos.latitude,
                            pos.longitude,
                            newRadius,
                            activityName,
                            locationName,
                            candidateColor)) {
                        Snackbar.make(
                                parentActivity.coordinatorLayout,
                                getString(R.string.create_zone_success),
                                Snackbar.LENGTH_LONG)
                        .show();
                        goBackToManageZones();
                    } else {
                        Snackbar.make(
                                parentActivity.coordinatorLayout,
                                getString(R.string.create_zone_failure),
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

        // TODO: provide appropriate colors: gray (uneditable) and greenish (editable)
        // TODO: we want gray markers (custom markers) and gray fill colors to show that. Via .icon() ?

        ArrayList<LatLng> latLngList = new ArrayList<>();

        for(Zone zone : dbHelper.getAllZones()){
            LatLng latLng = new LatLng(zone.getLatitude(), zone.getLongitude());
            latLngList.add(latLng);
            Marker marker = map.addMarker(
                optionsFixMarker.position(latLng)
            );
            Circle circle = map.addCircle(
                optionsFixCircle.center(latLng).radius(zone.getRadius())
            );

            fixMarkers.add(marker);
            fixCircles.add(circle);
        }
        centerMapTo(MapAbstract.getMapViewport(latLngList));
    }
}
