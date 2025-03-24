package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.utils.LocationHandler;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the map page of the app, where users can view a map of their location and nearby
 * mood events.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback{

    private static final String TAG = "MapFragment";
    private FirebaseAuth mAuth;
    private MoodEventRepository moodEventRepo;
    private ParticipantRepository participantRepository;
    private String username;
    private GoogleMap mMap;
    private Location userLocation;
    private LatLng currentLocation;

    // Filter-related variables
    private FloatingActionButton filterButton;
    private boolean isFilteringByHistory = false;
    private boolean isFilteringByFollow = false;
    private boolean isFilteringByWeek = false;
    private MoodEvent.EmotionalState selectedEmotionalState = null;
    private String searchKeyword = "";

    // Arrays
    ArrayList<MoodEvent> moodEventsHistory = new ArrayList<>();

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
                Log.d(TAG, "permission is granted");
                locationHandler.fetchUserLocation();
            } else {
                Log.e(TAG, "Location permission denied - cannot fetch location");
                Toast.makeText(getContext(), "Please enable location permissions.", Toast.LENGTH_SHORT).show();
            }
        });
        locationHandler.requestLocationPermission(locationPermissionLauncher);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Show filter button upon opening the app
        showFilterDialog();

        // Add filter button click listener
        filterButton = view.findViewById(R.id.filter_button);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "SupportMapFragment is null");
        }

        return view;
    }

    /**
     * Fetch user mood events
     */
    private void fetchSelfMoodEvents(@NonNull OnSuccessListener<ArrayList<MoodEvent>> onSuccessListener) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.e(TAG, "Cannot fetch mood events without a signed-in user");
            return;
        }
        username = user.getDisplayName();
        if (username == null) {
            Log.e(TAG, "Cannot fetch mood events without a username");
            return;
        }

        moodEventRepo.fetchEventsWithParticipantRef(participantRepository.getParticipantRef(username), moodEvents -> {
            moodEventsHistory.clear();
            for (MoodEvent event : moodEvents) {
                if (event.getGeoInfo() != null) {
                    Log.d(TAG, "Fetched mood event: " + event);
                    moodEventsHistory.add(event);
                }
            }
            onSuccessListener.onSuccess(moodEventsHistory);
        }, e -> {
            Log.e(TAG, "Failed to fetch mood events", e);
        });
    }

    private void fetchInRadiusMoodEventsFromFollowing(@NonNull OnSuccessListener<Map<String, MoodEvent>> onSuccessListener) {
        Location currentLocation = locationHandler.getLastLocation();
        if (currentLocation == null) {
            Log.i(TAG, "Location not available yet, waiting for location callback");
            locationHandler.fetchUserLocation(location -> {
                Log.i(TAG, "Location callback received, fetching mood events");
                doFetchInRadiusMoodEvents(location, onSuccessListener);
            });
        } else {
            Log.i(TAG, "Location already available, fetching mood events");
            doFetchInRadiusMoodEvents(currentLocation, onSuccessListener);
        }
    }

    private void doFetchInRadiusMoodEvents(@NonNull Location currentLocation, @NonNull OnSuccessListener<Map<String, MoodEvent>> onSuccessListener) {
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

        moodEventRepo.fetchForInRadiusEventsFromFollowing(username, currentLocation, 5.0, moodEventMaps -> {
            onSuccessListener.onSuccess(moodEventMaps);
        }, e -> {
            Log.e(TAG, "Failed to fetch mood events", e);
        });
    }

    //adding mood event icons to the map https://www.youtube.com/watch?v=4fExmOFKgQY
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

        // Getting user current location to set the map to this
        Location userCurrentLocation = locationHandler.getLastLocation();
        if (userCurrentLocation == null) {
            Log.i(TAG, "Location not available yet, waiting for location callback");
            locationHandler.fetchUserLocation(location -> {
                Log.i(TAG, "Location callback received");
                userLocation = location;

                Log.d(TAG, "Current location: " + userLocation);

                currentLocation = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            });
        } else {
            Log.i(TAG, "Location already available");
            userLocation = userCurrentLocation;
            currentLocation = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        }

        // adding map UI controls https://www.youtube.com/watch?v=y84o2kyi_eo
        // Get UI settings
        UiSettings uiSettings = googleMap.getUiSettings();
        // Enable map gestures and controls
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setRotateGesturesEnabled(true);
        uiSettings.setTiltGesturesEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);
        uiSettings.setScrollGesturesEnabledDuringRotateOrZoom(false);
        uiSettings.setIndoorLevelPickerEnabled(true);
    }

    //https://stackoverflow.com/questions/8799290/convert-string-text-to-bitmap
    /**
     * Converts text into a bitmap that we can display on the map
     * @param text
     * @return
     */
    public Bitmap textAsBitmap(String text) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(75);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
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

    /**
     * Primary filtering logic
     */
    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_moods_map, null);
        builder.setView(dialogView);

        // Defining inputs
        SwitchMaterial moodHistorySwitch = dialogView.findViewById(R.id.history_switch);
        SwitchMaterial followerMoodSwitch = dialogView.findViewById(R.id.follower_switch);
        SwitchMaterial recentWeekSwitch = dialogView.findViewById(R.id.recent_week_switch);
        Spinner moodSpinner = dialogView.findViewById(R.id.mood_spinner);
        EditText keywordEditText = dialogView.findViewById(R.id.keyword_edit_text);
        Button applyButton = dialogView.findViewById(R.id.apply_button);
        Button resetButton = dialogView.findViewById(R.id.reset_button);

        // Filling in inputs
        List<String> moodOptions = new ArrayList<>();
        moodOptions.add("All Moods");
        for (MoodEvent.EmotionalState state : MoodEvent.EmotionalState.values()) {
            if (state != MoodEvent.EmotionalState.NONE) {
                moodOptions.add(state.toString());
            }
        }

        ArrayAdapter<String> moodAdapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_item,
                moodOptions
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                text.setPadding(16, 16, 16, 16);
                return view;
            }
        };

        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(moodAdapter);

        moodHistorySwitch.setChecked(isFilteringByHistory);
        if (selectedEmotionalState != null) {
            int position = moodOptions.indexOf(selectedEmotionalState.toString());
            if (position >= 0) {
                moodSpinner.setSelection(position);
            }
        }

        followerMoodSwitch.setChecked(isFilteringByFollow);
        if (selectedEmotionalState != null) {
            int position = moodOptions.indexOf(selectedEmotionalState.toString());
            if (position >= 0) {
                moodSpinner.setSelection(position);
            }
        }

        recentWeekSwitch.setChecked(isFilteringByWeek);
        if (selectedEmotionalState != null) {
            int position = moodOptions.indexOf(selectedEmotionalState.toString());
            if (position >= 0) {
                moodSpinner.setSelection(position);
            }
        }

        keywordEditText.setText(searchKeyword);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        applyButton.setOnClickListener(v -> {
            isFilteringByHistory = moodHistorySwitch.isChecked();
            isFilteringByFollow = followerMoodSwitch.isChecked();
            isFilteringByWeek = recentWeekSwitch.isChecked();

            int moodPosition = moodSpinner.getSelectedItemPosition();
            if (moodPosition > 0) {
                String selectedMood = moodOptions.get(moodPosition);
                selectedEmotionalState = MoodEvent.EmotionalState.valueOf(selectedMood);
            } else {
                selectedEmotionalState = null;
            }

            searchKeyword = keywordEditText.getText().toString().trim().toLowerCase();

            mMap.clear();

            if (isFilteringByHistory) {
                fetchSelfMoodEvents(filteredMoods -> {
                    ArrayList<MoodEvent> finalFilteredList = applyFilters(filteredMoods);
                    Map<String, MoodEvent> filteredMap = new HashMap<>();

                    for (MoodEvent moodEvent : finalFilteredList){
                        filteredMap.put(username, moodEvent);
                    }

                    putMarkersOnMap(filteredMap);
                });
            }

            if (isFilteringByFollow) {
                fetchInRadiusMoodEventsFromFollowing(filteredMoods -> {
                    Map<String, MoodEvent> finalFilteredList = filteredMoods;
                    Map<String, MoodEvent> filteredMap = new HashMap<>();

                    for (Map.Entry<String, MoodEvent> entry : finalFilteredList.entrySet()) {
                        String userId = entry.getKey();
                        MoodEvent event = entry.getValue();

                        Log.d("MoodEvent", "User ID: " + userId + ", Mood: " + event.getTitle());

                        filteredMap.put(userId, event);  // Preserve User ID in map
                    }

                    // Convert Map values to List for filtering
                    ArrayList<MoodEvent> moodArray = new ArrayList<>(filteredMap.values());

                    // Apply additional filters (if necessary)
                    ArrayList<MoodEvent> filteredArray = applyFilters(moodArray);

                    // Use filteredMap to ensure markers have correct User ID → MoodEvent mapping
                    putMarkersOnMap(filteredMap);
                });
            }

            dialog.dismiss();
        });

        resetButton.setOnClickListener(v -> {
            mMap.clear();

            moodHistorySwitch.setChecked(false);
            followerMoodSwitch.setChecked(false);
            recentWeekSwitch.setChecked(false);
            moodSpinner.setSelection(0);
            keywordEditText.setText("");

            isFilteringByHistory = false;
            isFilteringByFollow = false;
            isFilteringByWeek = false;
            selectedEmotionalState = null;
            searchKeyword = "";

            dialog.dismiss();
        });
    }

    private ArrayList<MoodEvent> applyFilters(ArrayList<MoodEvent> moodEvents) {
        ArrayList<MoodEvent> filteredList = new ArrayList<>(moodEvents);

        if (isFilteringByWeek) {
            filteredList = filterByRecentWeek(filteredList);
        }

        if (selectedEmotionalState != null) {
            filteredList = filterByEmotionalState(filteredList, selectedEmotionalState);
        }

        if (!searchKeyword.isEmpty()) {
            filteredList = filterByKeyword(filteredList, searchKeyword);
        }

        if (filteredList.isEmpty() && (isFilteringByWeek || selectedEmotionalState != null || !searchKeyword.isEmpty())) {
            Toast.makeText(getContext(), "No mood events match the applied filters", Toast.LENGTH_SHORT).show();
            return new ArrayList<>(); // Return an empty list
        }

        return filteredList;
    }

    private ArrayList<MoodEvent> filterByRecentWeek(ArrayList<MoodEvent> events) {
        ArrayList<MoodEvent> filteredList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date oneWeekAgo = calendar.getTime();

        for (MoodEvent event : events) {
            if (event.getTimestamp().after(oneWeekAgo)) {
                filteredList.add(event);
            }
        }

        return filteredList;
    }

    private ArrayList<MoodEvent> filterByEmotionalState(ArrayList<MoodEvent> events, MoodEvent.EmotionalState state) {
        ArrayList<MoodEvent> filteredList = new ArrayList<>();

        for (MoodEvent event : events) {
            if (event.getEmotionalState() == state) {
                filteredList.add(event);
            }
        }

        return filteredList;
    }

    private ArrayList<MoodEvent> filterByKeyword(ArrayList<MoodEvent> events, String keyword) {
        ArrayList<MoodEvent> filteredList = new ArrayList<>();

        for (MoodEvent event : events) {
            if (event.getReason() != null && event.getReason().toLowerCase().contains(keyword)) {
                filteredList.add(event);
            }
        }

        return filteredList;
    }

    private void putMarkersOnMap(Map<String, MoodEvent> eventsMap) {
        for (Map.Entry<String, MoodEvent> moodEvent : eventsMap.entrySet()){
            String userId = moodEvent.getKey();
            MoodEvent event = moodEvent.getValue();

            Map<String, Object> geo = event.getGeoInfo();
            if (geo != null) {
                double lat = (double) geo.get("latitude");
                double lon = (double) geo.get("longitude");

                LatLng eventPos = new LatLng(lat, lon);
                MoodEvent.EmotionalState emotion = event.getEmotionalState();
                String moodText = emotion.toString();
                String emoji = EmotionUtils.getEmoticon(emotion);
                String finalString = "@".concat(userId).concat(": ").concat(moodText).concat(emoji);

                MarkerOptions testMark = new MarkerOptions().position(eventPos).title(finalString).icon(BitmapDescriptorFactory
                        .fromBitmap(textAsBitmap(emoji)));

                mMap.addMarker(testMark);
            }
        }
    }
}