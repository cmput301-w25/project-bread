package com.example.bread.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.LocationHandler;
import com.google.firebase.firestore.DocumentReference;

import java.util.Map;

public class AddMoodEventActivity extends AppCompatActivity {
    private static final String TAG = "AddMoodEventActivity"; // Log tag
    private Spinner emotionalStateSpinner, socialSituationSpinner;
    private EditText eventTitleEditText, reasonEditText, triggerEditText;
    private CheckBox locationCheckbox;
    private Button saveButton;
    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;
    private LocationHandler locationHandler;

    // Activity result launcher for location permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Log.d(TAG, "Location permission result: " + isGranted);
                if (isGranted) {
                    Log.i(TAG, "Permission granted, fetching user location");
                    locationHandler.fetchUserLocation();
                } else {
                    Log.w(TAG, "Permission denied, unchecking location checkbox");
                    locationCheckbox.setChecked(false);
                    Toast.makeText(this, "Please enable location permissions.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood_event);
        Log.i(TAG, "Activity created");

        // Tie UI elements to variables
        emotionalStateSpinner = findViewById(R.id.emotionalStateSpinner);
        reasonEditText = findViewById(R.id.reasonEditText);
        socialSituationSpinner = findViewById(R.id.socialSituationSpinner);
        saveButton = findViewById(R.id.saveButton);
        eventTitleEditText = findViewById(R.id.eventTitleEditText);
        triggerEditText = findViewById(R.id.triggerEditText);
        locationCheckbox = findViewById(R.id.locationCheckbox);
        Log.d(TAG, "UI elements initialized");

        // Set up repositories and location handler
        moodEventRepository = new MoodEventRepository();
        participantRepository = new ParticipantRepository();
        locationHandler = LocationHandler.getInstance(this);
        Log.d(TAG, "Repositories and location handler initialized");

        // Populate emotional state spinner
        ArrayAdapter<MoodEvent.EmotionalState> moodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MoodEvent.EmotionalState.values());
        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionalStateSpinner.setAdapter(moodAdapter);
        Log.v(TAG, "Emotional state spinner populated");

        // Populate social situation spinner
        ArrayAdapter<MoodEvent.SocialSituation> socialAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MoodEvent.SocialSituation.values());
        socialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(socialAdapter);
        Log.v(TAG, "Social situation spinner populated");

        // Set up location checkbox listener
        locationCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Location checkbox changed: " + isChecked);
            if (isChecked) {
                Log.i(TAG, "Checkbox checked, requesting location permission");
                locationHandler.requestLocationPermission(requestPermissionLauncher);
            }
        });

        // Save button logic
        saveButton.setOnClickListener(v -> {
            Log.i(TAG, "Save button clicked");
            saveMoodEvent();
        });
    }

    private void saveMoodEvent() {
        Log.i(TAG, "Starting saveMoodEvent");

        // Get required information for mood event
        MoodEvent.EmotionalState emotionalState = (MoodEvent.EmotionalState) emotionalStateSpinner.getSelectedItem();
        if (emotionalState == null) {
            Log.w(TAG, "No emotional state selected");
            return;
        }
        Log.d(TAG, "Emotional state selected: " + emotionalState);

        // Get all the form data
        String eventTitle = eventTitleEditText.getText().toString().trim();
        String reason = reasonEditText.getText().toString().trim();
        String trigger = triggerEditText.getText().toString().trim();
        MoodEvent.SocialSituation socialSituation = (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem();
        Log.v(TAG, "Form data - Title: " + eventTitle + ", Reason: " + reason + ", Trigger: " + trigger + ", SocialSituation: " + socialSituation);

        boolean isValid = true;

        // Validate eventTitle (example: non-empty required)
        if (eventTitle.isEmpty()) {
            Log.w(TAG, "Validation failed: Event title is empty");
            eventTitleEditText.setError("Event title cannot be empty");
            isValid = false;
        } else {
            Log.d(TAG, "Event title validated: " + eventTitle);
            //eventTitleEditText.setError(null);
        }

        if (!reason.isEmpty()) { // Only validate if reason is provided
            int charCount = reason.length();
            int wordCount = reason.split("\\s+").length; // Split by whitespace to count words
            Log.d(TAG, "Reason validation - Char count: " + charCount + ", Word count: " + wordCount);
            if (charCount > 20 || wordCount > 3) {
                Log.w(TAG, "Validation failed: Reason exceeds 20 chars or 3 words");
                reasonEditText.setError("Reason must be 20 characters or fewer and 3 words or fewer");
                isValid = false;
            } else {
                Log.d(TAG, "Reason validated: " + reason);
                reasonEditText.setError(null); // Clear error if valid
            }
        } else {
            Log.d(TAG, "Reason is empty (optional field)");
            reasonEditText.setError(null); // Clear error if empty (optional field)
        }

        if (trigger.isEmpty()) {
            Log.d(TAG, "Trigger is empty (optional field)");
            triggerEditText.setError(null); // Clear error, empty is valid since trigger is optional
        } else {
            Log.d(TAG, "Trigger validated: " + trigger);
        }

        // If any validation fails, stop here
        if (!isValid) {
            Log.w(TAG, "Validation failed, aborting save");
            return;
        }

        // Get participant reference
        String username = getCurrentUsername();
        Log.d(TAG, "Retrieved username: " + username);
        DocumentReference participantRef = participantRepository.getParticipantRef(username);
        Log.d(TAG, "Participant reference: " + (participantRef != null ? participantRef.getPath() : "null"));

        // Create the mood event
        MoodEvent moodEvent = new MoodEvent(eventTitle, reason, emotionalState, participantRef);
        moodEvent.setSocialSituation(socialSituation);
        moodEvent.setTrigger(trigger);
        moodEvent.setAttachedImage(null); // TODO: Implement image upload functionality
        Log.d(TAG, "MoodEvent created: " + moodEvent.toString());
        Log.d(TAG, "Timestamp (before save): " + (moodEvent.getTimestamp() != null ? moodEvent.getTimestamp().toString() : "null (to be set by server)"));

        // Handle location based on checkbox state
        Log.d(TAG, "Location checkbox checked: " + locationCheckbox.isChecked());
        Log.d(TAG, "Last location: " + (locationHandler.getLastLocation() != null ? locationHandler.getLastLocation().toString() : "null"));
        if (locationCheckbox.isChecked() && locationHandler.getLastLocation() != null) {
            try {
                Map<String, Object> geoInfo = moodEvent.generateGeoInfo(locationHandler.getLastLocation());
                moodEvent.setGeoInfo(geoInfo);
                Log.i(TAG, "Location attached to mood event: " + geoInfo);
            } catch (Exception e) {
                Log.e(TAG, "Error generating geo info: " + e.getMessage(), e);
                //Toast.makeText(this, "Error attaching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            moodEvent.setGeoInfo(null);
            Log.d(TAG, "No location attached (checkbox unchecked or location null)");
        }

        // Save to Firebase
        Log.i(TAG, "Saving mood event to Firebase");
        moodEventRepository.addMoodEvent(
                moodEvent,
                aVoid -> {
                    Log.i(TAG, "Mood event saved successfully");
                    Toast.makeText(this, "Mood saved!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the screen
                },
                e -> {
                    Log.e(TAG, "Failed to save mood event: " + e.getMessage(), e);
                    //Toast.makeText(this, "Failed to save mood: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    // Helper method to get the current username (placeholder - replace with actual login logic)
    private String getCurrentUsername() {
        String username = getSharedPreferences("sharedPrefs", MODE_PRIVATE).getString("username", "");
        Log.v(TAG, "Retrieved username from SharedPreferences: " + username);
        return username;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity destroyed, stopping location updates");
        locationHandler.stopLocationUpdates();
    }
}
