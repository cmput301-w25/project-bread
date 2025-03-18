package com.example.bread.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

public class MapDisplayFragment extends Fragment implements OnMapReadyCallback {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_display, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MapError", "SupportMapFragment is null");
        }
        return view;
    }

    //adding mood event icons to the map https://www.youtube.com/watch?v=4fExmOFKgQY
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        LatLng uofa = new LatLng(53.5232, -113.5263);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uofa, 15));

        // adding map UI controls https://www.youtube.com/watch?v=y84o2kyi_eo
        // Get UI settings
        UiSettings uiSettings = googleMap.getUiSettings();

        // Enable various map gestures and controls
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setRotateGesturesEnabled(true);
        uiSettings.setTiltGesturesEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);
        uiSettings.setScrollGesturesEnabledDuringRotateOrZoom(false);
        uiSettings.setIndoorLevelPickerEnabled(true);
    }
}
