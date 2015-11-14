package de.mohoff.zeiterfassung.ui.components.zones;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
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
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.ui.components.MapAbstract;

/**
 * Created by moo on 10/9/15.
 */
public class AddZoneMap extends ManageZonesMapAbstract {
    String activityName, locationName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activityName = getArguments().getString("activityName");
        locationName = getArguments().getString("locationName");
        candidateColor = getArguments().getInt("color");

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
                if (radius < Zone.MIN_RADIUS) {
                    GeneralHelper.showToast(getActivity(), "Input must be >= " + Zone.MIN_RADIUS + " meters.");
                    // Cancel save process
                } else if (candidateMarker == null) {
                    GeneralHelper.showToast(getActivity(), "Please pin area on map.");
                } else {
                    // TODO: Check if entered radius is not near other Zones.
                    LatLng pos = candidateMarker.getPosition();
                    int result = dbHelper.createNewZone(pos.latitude, pos.longitude, radius, activityName, locationName);
                    if (result != 1) {
                        GeneralHelper.showToast(getActivity(), "Couldn't add Zone. Does it already exist?");
                    } else {
                        GeneralHelper.showToast(getActivity(), "Zone successfully added.");
                        // Clear back stack because add-operation succeeded.
                        GeneralHelper.clearBackStack(getActivity());
                        // Go back to ManageZones fragment with clean back stack
                        Fragment nextFragment = new ManageZones();
                        parentActivity.replaceFragment(nextFragment, false);
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

        centerMapTo(MapAbstract.getMapViewport(latLngList, 100));
    }
}
