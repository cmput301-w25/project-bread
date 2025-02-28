package com.example.bread.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHandler {
    private static final String TAG = "LocationHandler";
    private static LocationHandler instance;
    private final Context context;
    private final FusedLocationProviderClient fusedLocationProviderClient;

    private Location lastLocation;

    private LocationCallback locationCallback;

    private LocationHandler(Context context) {
        this.context = context.getApplicationContext();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public static synchronized LocationHandler getInstance(Context context) {
        if (instance == null) {
            instance = new LocationHandler(context);
        }
        return instance;
    }

    /**
     * Check if we already have FINE_LOCATION permission; if not, request it via the given launcher.
     * If we do have it, immediately fetch the user location.
     */
    public void requestLocationPermission(ActivityResultLauncher<String> permissionLauncher) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Launch the permission request dialog
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            // Permission is already granted, so we can get location immediately
            fetchUserLocation();
        }
    }

    /**
     * Called after permission is granted (or from requestLocationPermission if already granted).
     */
    @SuppressLint("MissingPermission")
    public void fetchUserLocation() {
        // We can only call this if permission is already granted
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "fetchUserLocation called without location permission!");
            return;
        }

        // Try to get a quick last-known location
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLocation = location;
                        Log.d(TAG, "fetchUserLocation Success: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                    } else {
                        Log.d(TAG, "Last location is null. Requesting updates...");
                        requestLocationUpdates();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error trying to get last location: ", e);
                    requestLocationUpdates();
                });
    }

    /**
     * Continuously request location updates if we don’t have a last location or if you need updates.
     */
    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        // Again, check for permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "requestLocationUpdates called without permission!");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(5000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(2000)
                .build();

        // Keep a single reference so we can stop later
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        lastLocation = location;
                        Log.d(TAG, "requestLocationUpdates onLocationResult: "
                                + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                        break;
                    }
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    /**
     * Stop receiving continuous location updates.
     */
    public void stopLocationUpdates() {
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }
}
