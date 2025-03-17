package com.example.bread.fragment;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.LocationHandler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Represents the map page of the app, where users can view a map of their location and nearby
 * mood events.
 */
public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private FirebaseAuth mAuth;
    private MoodEventRepository moodEventRepo;
    private ParticipantRepository participantRepository;

    /**
     * These two fields are used to handle location permissions and fetching the user's location.
     * They are required in all the activities that need to fetch the user's location.
     * Always call stopLocationUpdates() in the onDestroy() / onStop() method of the activity.
     */
    private LocationHandler locationHandler;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        moodEventRepo = new MoodEventRepository();
        participantRepository = new ParticipantRepository();
        locationHandler = LocationHandler.getInstance(requireContext());
        locationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                locationHandler.fetchUserLocation();
            } else {
                Log.e(TAG, "Location permission denied - cannot fetch location");
            }
        });
        locationHandler.requestLocationPermission(locationPermissionLauncher);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // TODO: alternate between these two methods to display events on the map based on user selected filter
        fetchSelfMoodEvents();
        fetchInRadiusMoodEventsFromFollowing();

        return view;
    }

    private void fetchSelfMoodEvents() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot fetch mood events without a signed-in user");
            return;
        }
        String username = user.getDisplayName();
        if (username == null) {
            Log.e(TAG, "Cannot fetch mood events without a username");
            return;
        }
        moodEventRepo.fetchEventsWithParticipantRef(participantRepository.getParticipantRef(username), moodEvents -> {
            for (MoodEvent event : moodEvents) {
                if (event.getGeoInfo() != null) {
                    // TODO: collect these events here in a combined list to display on the map
                    Log.d(TAG, "Fetched mood event: " + event);
                }
            }
        }, e -> {
            Log.e(TAG, "Failed to fetch mood events", e);
        });
    }

    private void fetchInRadiusMoodEventsFromFollowing() {
        Location currentLocation = locationHandler.getLastLocation();
        if (currentLocation == null) {
            Log.i(TAG, "Location not available yet, waiting for location callback");
            locationHandler.fetchUserLocation(location -> {
                Log.i(TAG, "Location callback received, fetching mood events");
                doFetchInRadiusMoodEvents(location);
            });
        } else {
            Log.i(TAG, "Location already available, fetching mood events");
            doFetchInRadiusMoodEvents(currentLocation);
        }
    }

    private void doFetchInRadiusMoodEvents(@NonNull Location currentLocation) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot fetch mood events without a signed-in user");
            return;
        }
        String username = user.getDisplayName();
        if (username == null) {
            Log.e(TAG, "Cannot fetch mood events without a username");
            return;
        }
        moodEventRepo.fetchForInRadiusEventsFromFollowing(username, currentLocation, 5.0, moodEvents -> {
            // TODO: collect these events here in a combined list to display on the map
            for (MoodEvent moodEvent : moodEvents) {
                Log.d(TAG, "Fetched mood event: " + moodEvent.toString());
            }
        }, e -> {
            Log.e(TAG, "Failed to fetch mood events", e);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "Fragment view destroyed, stopping location updates");
        locationHandler.stopLocationUpdates();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        locationPermissionLauncher = null;
    }
}