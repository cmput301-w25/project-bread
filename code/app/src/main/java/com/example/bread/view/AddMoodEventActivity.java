package com.example.bread.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class AddMoodEventActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Spinner emotionalStateSpinner, socialSituationSpinner;
    private EditText eventTitleEditText, reasonEditText, triggerEditText;
    private CheckBox locationCheckbox;
    private Button saveButton;
    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood_event);

        // Tie UI elements to variables
        emotionalStateSpinner = findViewById(R.id.emotionalStateSpinner);
        reasonEditText = findViewById(R.id.reasonEditText);
        socialSituationSpinner = findViewById(R.id.socialSituationSpinner);
        saveButton = findViewById(R.id.saveButton);
        eventTitleEditText = findViewById(R.id.eventTitleEditText);  // New field
        triggerEditText = findViewById(R.id.triggerEditText);        // New field
        locationCheckbox = findViewById(R.id.locationCheckbox);      // New field

        // Set up repositories and location client
        moodEventRepository = new MoodEventRepository();
        participantRepository = new ParticipantRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Populate emotional state spinner
        ArrayAdapter<MoodEvent.EmotionalState> moodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MoodEvent.EmotionalState.values());
        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionalStateSpinner.setAdapter(moodAdapter);

        // Populate social situation spinner
        ArrayAdapter<MoodEvent.SocialSituation> socialAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MoodEvent.SocialSituation.values());
        socialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(socialAdapter);

        // Save button logic
        saveButton.setOnClickListener(v -> saveMoodEvent());
    }

    private void saveMoodEvent() {
        // Get event title (optional)
        String eventTitle = eventTitleEditText.getText().toString().trim();
        if (eventTitle.isEmpty()) eventTitle = null;

        // Get reason (optional)
        String reason = reasonEditText.getText().toString().trim();
        if (reason.isEmpty()) reason = null;

        // Get trigger (optional)
        String trigger = triggerEditText.getText().toString().trim();
        if (trigger.isEmpty()) trigger = null;

        // Get selected emotional state (required)
        MoodEvent.EmotionalState emotionalState = (MoodEvent.EmotionalState) emotionalStateSpinner.getSelectedItem();
        if (emotionalState == null) {
            Toast.makeText(this, "Please select a mood!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get social situation (optional)
        MoodEvent.SocialSituation socialSituation = (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem();

        // Handle location (optional, based on checkbox)
        AtomicReference<Map<String, Object>> geoInfo = new AtomicReference<>();
        if (locationCheckbox.isChecked()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                Toast.makeText(this, "Location permission required. Please grant permission and try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            String finalEventTitle = eventTitle;
            String finalReason = reason;
            String finalTrigger = trigger;
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    geoInfo.set(new MoodEvent().generateGeoInfo(location)); // Reuse MoodEvent's method
                    saveMoodEventWithLocation(geoInfo.get(), finalEventTitle, finalReason, emotionalState, socialSituation, finalTrigger);
                } else {
                    Toast.makeText(this, "Unable to get location. Please enable GPS and try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveMoodEventWithLocation(null, eventTitle, reason, emotionalState, socialSituation, trigger);
        }
    }

    private void saveMoodEventWithLocation(Map<String, Object> geoInfo, String eventTitle, String reason,
                                           MoodEvent.EmotionalState emotionalState, MoodEvent.SocialSituation socialSituation,
                                           String trigger) {
        // Fetch the current participant from Firebase using ParticipantRepository
        participantRepository.fetchParticipant(getCurrentUsername(), participant -> {
            if (participant == null) {
                Toast.makeText(this, "User not found. Please log in or register!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the participant's username or use a unique identifier (e.g., document ID)
            String username = participant.getUsername();
            DocumentReference participantRef = FirebaseFirestore.getInstance().collection("participants").document(username);

            // Create and save the mood event inside the lambda
            MoodEvent moodEvent = new MoodEvent(eventTitle, reason, emotionalState, participantRef);
            moodEvent.setSocialSituation(socialSituation);  // Optional
            moodEvent.setGeoInfo(geoInfo);                 // Optional location
            moodEvent.setTrigger(trigger);                 // Optional trigger

            // TODO: Implement image upload functionality for mood events (US 02.02.01)
            moodEvent.setImageUrl(null);  // Placeholder for now

            // Save to Firebase
            moodEventRepository.addMoodEvent(
                    moodEvent,
                    aVoid -> {
                        Toast.makeText(this, "Mood saved!", Toast.LENGTH_SHORT).show();
                        finish(); // Close the screen
                    },
                    e -> Toast.makeText(this, "Failed to save mood: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }, e -> Toast.makeText(this, "Failed to fetch user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Helper method to get the current username (placeholder - replace with actual login logic)
    private String getCurrentUsername() {
        // This is a placeholder. In a real app, you’d get the username from Firebase Authentication,
        // SharedPreferences, or another authentication system.
        return getSharedPreferences("sharedPrefs", MODE_PRIVATE).getString("username", "");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveMoodEvent(); // Retry saving with location
            } else {
                Toast.makeText(this, "Location permission denied. Location will not be saved.", Toast.LENGTH_SHORT).show();
                saveMoodEventWithLocation(null, eventTitleEditText.getText().toString().trim().isEmpty() ? null : eventTitleEditText.getText().toString().trim(),
                        reasonEditText.getText().toString().trim().isEmpty() ? null : reasonEditText.getText().toString().trim(),
                        (MoodEvent.EmotionalState) emotionalStateSpinner.getSelectedItem(),
                        (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem(),
                        triggerEditText.getText().toString().trim().isEmpty() ? null : triggerEditText.getText().toString().trim());
            }
        }
    }
}
