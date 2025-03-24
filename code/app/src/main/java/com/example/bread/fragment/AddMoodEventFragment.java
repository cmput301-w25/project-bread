package com.example.bread.fragment;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.LocationHandler;
import com.example.bread.view.HomePage;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.util.Map;

/**
 * Represents a dialog where users can add a mood event to their history.
 */
public class AddMoodEventFragment extends DialogFragment {
    private static final String TAG = "AddMoodEventFragment";
    private Spinner emotionalStateSpinner, socialSituationSpinner;
    private EditText eventTitleEditText, reasonEditText;
    private Chip locationChip, publicChip; // Replaced CheckBox with Chip
    private Button saveButton, cancelButton;
    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;
    private LocationHandler locationHandler;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            Log.d(TAG, "Location permission result: " + isGranted);
            if (isGranted) {
                Log.i(TAG, "Permission granted, fetching user location");
                locationHandler.fetchUserLocation();
            } else {
                Log.w(TAG, "Permission denied, unchecking location chip");
                locationChip.setChecked(false);
                Toast.makeText(context, "Please enable location permissions.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            // Set the dialog width to 99% of the screen width
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.99);
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
            // Set a fully transparent background for the dialog window
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_mood_event, container, false);
        Log.i(TAG, "Dialog view created");

        emotionalStateSpinner = view.findViewById(R.id.emotionalStateSpinner);
        reasonEditText = view.findViewById(R.id.reasonEditText);
        socialSituationSpinner = view.findViewById(R.id.socialSituationSpinner);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        eventTitleEditText = view.findViewById(R.id.eventTitleEditText);
        locationChip = view.findViewById(R.id.locationChip); // Updated to Chip
        publicChip = view.findViewById(R.id.publicChip); // Updated to Chip
        Log.d(TAG, "UI elements initialized");

        moodEventRepository = new MoodEventRepository();
        participantRepository = new ParticipantRepository();
        locationHandler = LocationHandler.getInstance(requireContext());
        Log.d(TAG, "Repositories and location handler initialized");

        // Populate emotional state spinner
        ArrayAdapter<MoodEvent.EmotionalState> moodAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, MoodEvent.EmotionalState.values());
        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionalStateSpinner.setAdapter(moodAdapter);
        emotionalStateSpinner.setSelection(MoodEvent.EmotionalState.NONE.ordinal());
        Log.v(TAG, "Emotional state spinner populated");

        // Populate social situation spinner
        ArrayAdapter<MoodEvent.SocialSituation> socialAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, MoodEvent.SocialSituation.values());
        socialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(socialAdapter);
        Log.v(TAG, "Social situation spinner populated");

        // Set up location chip listener
        locationChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Location chip changed: " + isChecked);
            if (isChecked) {
                Log.i(TAG, "Chip checked, requesting location permission");
                locationHandler.requestLocationPermission(requestPermissionLauncher);
            }
        });

        // Save button logic
        saveButton.setOnClickListener(v -> {
            Log.i(TAG, "Save button clicked");
            saveMoodEvent();
        });

        // Cancel button logic
        cancelButton.setOnClickListener(v -> {
            Log.i(TAG, "Cancel button clicked");
            dismiss(); // Dismiss the dialog
        });

        return view;
    }

    private void saveMoodEvent() {
        Log.i(TAG, "Starting saveMoodEvent");

        // Get required information for mood event
        MoodEvent.EmotionalState emotionalState = (MoodEvent.EmotionalState) emotionalStateSpinner.getSelectedItem();
        MoodEvent.Visibility visibility = publicChip.isChecked() ? MoodEvent.Visibility.PUBLIC : MoodEvent.Visibility.PRIVATE; // Updated to use Chip
        if (emotionalState == null) {
            Log.w(TAG, "No emotional state selected");
            return;
        }
        Log.d(TAG, "Emotional state selected: " + emotionalState);

        // Get all the form data
        String eventTitle = eventTitleEditText.getText().toString().trim();
        String reason = reasonEditText.getText().toString().trim();

        MoodEvent.SocialSituation socialSituation = (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem();
        Log.v(TAG, "Form data - Title: " + eventTitle + ", Reason: " + reason + ", SocialSituation: " + socialSituation);

        boolean isValid = true;

        // Validate eventTitle (example: non-empty required)
        if (eventTitle.isEmpty()) {
            Log.w(TAG, "Validation failed: Event title is empty");
            eventTitleEditText.setError("Event title cannot be empty");
            isValid = false;
        } else {
            Log.d(TAG, "Event title validated: " + eventTitle);
            eventTitleEditText.setError(null);
        }

        // Validate emotionalState (must not be NONE)
        if (emotionalState == MoodEvent.EmotionalState.NONE) {
            Log.w(TAG, "Validation failed: Emotional state cannot be NONE");
            Toast.makeText(requireContext(), "Please select an emotional state!", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
            Log.d(TAG, "Emotional state validated: " + emotionalState);
        }

        if (!reason.isEmpty()) { // Only validate if reason is provided
            int charCount = reason.length();
            int wordCount = reason.split("\\s+").length; // Split by whitespace to count words
            Log.d(TAG, "Reason validation - Char count: " + charCount + ", Word count: " + wordCount);
            if (reason.length() > 200) {
                Log.w(TAG, "Validation failed: Reason exceeds 200 chars");
                reasonEditText.setError("Reason must be 200 characters or fewer");
                isValid = false;
            } else {
                Log.d(TAG, "Reason validated: " + reason);
                reasonEditText.setError(null); // Clear error if valid
            }
        } else {
            Log.d(TAG, "Reason is empty (optional field)");
            reasonEditText.setError(null); // Clear error if empty (optional field)
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
        moodEvent.setVisibility(visibility);

        moodEvent.setAttachedImage(null); // TODO: Implement image upload functionality
        Log.d(TAG, "MoodEvent created: " + moodEvent.toString());
        Log.d(TAG, "Timestamp (before save): " + (moodEvent.getTimestamp() != null ? moodEvent.getTimestamp().toString() : "null (to be set by server)"));

        // Handle location based on chip state
        Log.d(TAG, "Location chip checked: " + locationChip.isChecked()); // Updated to use Chip
        Log.d(TAG, "Last location: " + (locationHandler.getLastLocation() != null ? locationHandler.getLastLocation().toString() : "null"));
        if (locationChip.isChecked() && locationHandler.getLastLocation() != null) {
            try {
                Map<String, Object> geoInfo = moodEvent.generateGeoInfo(locationHandler.getLastLocation());
                moodEvent.setGeoInfo(geoInfo);
                Log.i(TAG, "Location attached to mood event: " + geoInfo);
            } catch (Exception e) {
                Log.e(TAG, "Error generating geo info: " + e.getMessage(), e);
                return;
            }
        } else {
            moodEvent.setGeoInfo(null);
            Log.d(TAG, "No location attached (chip unchecked or location null)");
        }

        // Save to Firebase
        Log.i(TAG, "Saving mood event to Firebase");
        moodEventRepository.addMoodEvent(
                moodEvent,
                aVoid -> {
                    Log.i(TAG, "Mood event saved successfully");
                    Toast.makeText(requireContext(), "Mood saved!", Toast.LENGTH_SHORT).show();

                    // Dismiss the dialog
                    dismiss();
                    // Ensure the HomeFragment is displayed

                    // Navigate back to HomeFragment
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction().setCustomAnimations(
                                    R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
                            )
                            .replace(R.id.frame_layout, new HomeFragment())
                            .commit();


                    if (getActivity() instanceof HomePage) {
                        ((HomePage) getActivity()).selectHomeNavigation();
                    }
                },
                e -> {
                    Log.e(TAG, "Failed to save mood event: " + e.getMessage(), e);
                }
        );
    }

    // Helper method to get the current username
    private String getCurrentUsername() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String username = currentUser.getDisplayName(); // Use getEmail() if username is email
        Log.v(TAG, "Retrieved username from FirebaseAuth: " + username);
        return username;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "Dialog view destroyed, stopping location updates");
        locationHandler.stopLocationUpdates();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        requestPermissionLauncher = null; // Clean up
    }
}