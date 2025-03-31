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

/**
 * A singleton class that handles location-related tasks.
 * It fetches the user's location and sets up continuous updates to handle changes (such as emulator location changes).
 */
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

    public interface OnLocationAvailableCallback {
        void onLocationAvailable(Location location);
    }

    /**
     * Check if we already have FINE_LOCATION permission; if not, request it.
     * If granted, immediately fetch the user location.
     */
    public void requestLocationPermission(ActivityResultLauncher<String> permissionLauncher) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            fetchUserLocation();
        }
    }

    /**
     * Fetch the user's location.
     * Even if a valid last-known location is returned, we subscribe to continuous updates so that any changes are captured.
     */
    @SuppressLint("MissingPermission")
    public void fetchUserLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "fetchUserLocation called without location permission!");
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLocation = location;
                        Log.d(TAG, "fetchUserLocation Success: "
                                + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                    }
                    // Always subscribe to updates if not already subscribed
                    if (locationCallback == null) {
                        requestLocationUpdates();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error trying to get last location: ", e);
                    if (locationCallback == null) {
                        requestLocationUpdates();
                    }
                });
    }

    /**
     * Fetch the user's location and call the given callback with the result.
     * Also subscribes to continuous updates.
     */
    @SuppressLint("MissingPermission")
    public void fetchUserLocation(@NonNull OnLocationAvailableCallback callback) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "fetchUserLocation called without location permission!");
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLocation = location;
                        Log.d(TAG, "fetchUserLocation Success: "
                                + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                        callback.onLocationAvailable(lastLocation);
                    }
                    if (locationCallback == null) {
                        requestLocationUpdates();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error trying to get last location: ", e);
                    if (locationCallback == null) {
                        requestLocationUpdates();
                    }
                });
    }

    /**
     * Continuously request location updates.
     * This method will only set up updates if they aren’t already active.
     */
    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "requestLocationUpdates called without permission!");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(5000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        lastLocation = location;
                        Log.d(TAG, "requestLocationUpdates onLocationResult: "
                                + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                        // Exit after processing the first valid location
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
