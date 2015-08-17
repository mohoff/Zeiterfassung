package de.mohoff.zeiterfassung.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.GeneralHelper;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.ui.MainActivity;

/**
 * Created by moo on 8/16/15.
 */
public class MapAbstract extends Fragment implements OnMapReadyCallback {
    protected MainActivity parentActivity;
    protected static View view;
    protected ProgressBar progressBar;

    protected MapFragment mapFragment;
    protected GoogleMap map;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = (RelativeLayout) inflater.inflate(R.layout.fragment_map, container, false);
        } catch (InflateException e) {
            /* map is already there, just return view as it is */
        }

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        parentActivity = (MainActivity) getActivity();

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        //googleMap.setMyLocationEnabled(true); // displays current user location with bearing on the map
        progressBar.setVisibility(View.GONE);
    }

    protected void addMarkerToMap(List<Marker> markers, GoogleMap map, LatLng latLng){
        markers.add(map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .draggable(false)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                //.title("acc " + String.valueOf(loc.getAccuracy()) + ", speed " + String.valueOf(loc.getSpeed()) + ", alt " + String.valueOf(loc.getAltitude()))
        ));
        if(markers.size() > 1){
            // change color for old marker
            markers.get(markers.size()-2).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
            // move center of map to new marker ... in some cases not wanted --> TODO: checkbox on UI asking "follow location updates on the map"
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        } else {
            // zoom map in to marker, if the marker is the first one on the map
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }
    

}


