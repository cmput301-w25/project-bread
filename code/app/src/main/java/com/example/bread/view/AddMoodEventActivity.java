package com.example.bread.view;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.CompoundButton;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.LocationHandler;
import com.google.firebase.firestore.DocumentReference;

import java.util.Map;

public class AddMoodEventActivity extends AppCompatActivity {
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
                if (isGranted) {
                    // If permission is granted, fetch location and keep checkbox checked
                    locationHandler.fetchUserLocation();
                } else {
                    // If permission is denied, uncheck the box
                    locationCheckbox.setChecked(false);
                    Toast.makeText(this, "Please enable location permissions.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood_event);

        // Tie UI elements to variables
        emotionalStateSpinner = findViewById(R.id.emotionalStateSpinner);
        reasonEditText = findViewById(R.id.reasonEditText);
        socialSituationSpinner = findViewById(R.id.socialSituationSpinner);
        saveButton = findViewById(R.id.saveButton);
        eventTitleEditText = findViewById(R.id.eventTitleEditText);
        triggerEditText = findViewById(R.id.triggerEditText);
        locationCheckbox = findViewById(R.id.locationCheckbox);

        // Set up repositories and location handler
        moodEventRepository = new MoodEventRepository();
        participantRepository = new ParticipantRepository();
        locationHandler = LocationHandler.getInstance(this);

        // Populate emotional state spinner
        ArrayAdapter<MoodEvent.EmotionalState> moodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MoodEvent.EmotionalState.values());
        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionalStateSpinner.setAdapter(moodAdapter);

        //emotionalStateSpinner.setSelection(getIndex(emotionalStateSpinner, MoodEvent.EmotionalState.NONE));

        // Populate social situation spinner
        ArrayAdapter<MoodEvent.SocialSituation> socialAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MoodEvent.SocialSituation.values());
        socialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(socialAdapter);

        // Set up location checkbox listener
        locationCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Request location permission when checkbox is checked
                locationHandler.requestLocationPermission(requestPermissionLauncher);
            }
        });

        // Save button logic
        saveButton.setOnClickListener(v -> saveMoodEvent());
    }

    private void saveMoodEvent() {
        // Get required information for mood event
        MoodEvent.EmotionalState emotionalState = (MoodEvent.EmotionalState) emotionalStateSpinner.getSelectedItem();
        if (emotionalState == null) {
            Toast.makeText(this, "Please select a mood!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get all the form data
        String eventTitle = eventTitleEditText.getText().toString().trim();
        //if (eventTitle.isEmpty()) eventTitle = null;

        String reason = reasonEditText.getText().toString().trim();
        //if (reason.isEmpty()) reason = null;

        String trigger = triggerEditText.getText().toString().trim();
        //if (trigger.isEmpty()) trigger = null;

        MoodEvent.SocialSituation socialSituation = (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem();


        boolean isValid = true;

        // Validate eventTitle (example: non-empty required)
        if (eventTitle.isEmpty()) {
            eventTitleEditText.setError("Event title cannot be empty");
            isValid = false;
        } else {
            eventTitleEditText.setError(null);
        }

        if (!reason.isEmpty()) { // Only validate if reason is provided
            int charCount = reason.length();
            int wordCount = reason.split("\\s+").length; // Split by whitespace to count words

            if (charCount > 20 || wordCount > 3) {
                reasonEditText.setError("Reason must be 20 characters or fewer and 3 words or fewer");
                isValid = false;
            } else {
                reasonEditText.setError(null); // Clear error if valid
            }
        } else {
            reasonEditText.setError(null); // Clear error if empty (optional field)
        }


        // Validate trigger (example: optional, but if provided, max length 100)
//        if (!trigger.isEmpty() && trigger.length() > 100) {
//            triggerEditText.setError("Trigger must be 100 characters or less");
//            isValid = false;
//        } else {
//            triggerEditText.setError(null); // Clear error if valid
//        }
        if (trigger.isEmpty()) {
            triggerEditText.setError(null); // Clear error, empty is valid since trigger is optional
        }

        // If any validation fails, stop here
        if (!isValid) {
            return;
        }

        // Get participant reference
        DocumentReference participantRef = participantRepository.getParticipantRef(getCurrentUsername());

        // Create the mood event
        MoodEvent moodEvent = new MoodEvent(eventTitle, reason, emotionalState, participantRef);
        moodEvent.setSocialSituation(socialSituation);
        moodEvent.setTrigger(trigger);
        moodEvent.setAttachedImage(null); // TODO: Implement image upload functionality

        // Handle location based on checkbox state
        if (locationCheckbox.isChecked() && locationHandler.getLastLocation() != null) {
            // If checkbox is checked and we have a location, add it to the mood event
            Map<String, Object> geoInfo = moodEvent.generateGeoInfo(locationHandler.getLastLocation());
            moodEvent.setGeoInfo(geoInfo);
        } else {
            // If checkbox is unchecked or no location is available, set geoInfo to null
            moodEvent.setGeoInfo(null);
        }

        // Save to Firebase
        moodEventRepository.addMoodEvent(
                moodEvent,
                aVoid -> {
                    Toast.makeText(this, "Mood saved!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the screen
                },
                e -> Toast.makeText(this, "Failed to save mood: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    // Helper method to get the current username (placeholder - replace with actual login logic)
    private String getCurrentUsername() {
        // This is a placeholder. In a real app, you'd get the username from Firebase Authentication,
        // SharedPreferences, or another authentication system.
        return getSharedPreferences("sharedPrefs", MODE_PRIVATE).getString("username", "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure to stop location updates when the activity is destroyed
        locationHandler.stopLocationUpdates();
    }
}
