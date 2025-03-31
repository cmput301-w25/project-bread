package com.example.bread.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.example.bread.utils.LocationHandler;
import com.example.bread.view.HomePage;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.util.Map;

/**
 * AddMoodEventFragment - Fragment
 * <p>
 * Role / Purpose
 * A DialogFragment that provides a form interface for users to create and submit a new MoodEvent.
 * It allows the user to input a title, select emotional state and social situation, optionally attach an image and/or location, and set the visibility of the mood event.
 * The form includes input validation and persists the mood event to Firestore using MoodEventRepository.
 * The fragment also navigates back to HomeFragment after a successful save.
 * <p>
 * Design Patterns
 * Observer Pattern: Uses listeners and callbacks for handling UI interactions and activity results.
 * Repository Pattern: Abstracts data operations via MoodEventRepository and ParticipantRepository.
 * Singleton Pattern: Accesses shared services like FirebaseAuth and LocationHandler.
 * MVC Pattern: Fragment acts as a controller managing view input and model updates.
 * <p>
 * Outstanding Issues
 * Updates UI before Firebase save confirms success, can lead to misleading UI.
 */

public class AddMoodEventFragment extends DialogFragment {
    private static final String TAG = "AddMoodEventFragment";
    private Spinner emotionalStateSpinner, socialSituationSpinner;
    private EditText eventTitleEditText, reasonEditText;
    private Chip locationChip, publicChip;
    private Button saveButton, cancelButton;
    private ImageButton imageAddButton, removeImageButton;
    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;
    private LocationHandler locationHandler;
    private String imageBase64; // To store the attached image
    private ActivityResultLauncher<Intent> imagePickerLauncher;
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

        // Register the image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getData() == null) {
                            Log.e(TAG, "No image selected.");
                            return;
                        }
                        try {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                imageAddButton.setImageURI(imageUri);
                                imageBase64 = ImageHandler.compressImageToBase64(requireContext(), imageUri);
                                Log.d(TAG, "Image selected and converted: " + imageBase64);
                                Toast.makeText(requireContext(), "Image attached", Toast.LENGTH_SHORT).show();
                                // Show the remove button
                                removeImageButton.setVisibility(View.VISIBLE);
                            } else {
                                Log.e(TAG, "No image selected.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "User canceled image selection.", e);
                            Toast.makeText(requireContext(), "No Image Selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.99);
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
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
        locationChip = view.findViewById(R.id.locationChip);
        publicChip = view.findViewById(R.id.publicChip);
        imageAddButton = view.findViewById(R.id.imageAdd);
        removeImageButton = view.findViewById(R.id.removeImageButton);
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

        // Set up image add button listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2) {
            imageAddButton.setOnClickListener(v -> {
                Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                imagePickerLauncher.launch(intent);
            });
        }

        // Set up remove image button listener
        removeImageButton.setOnClickListener(v -> {
            imageBase64 = null;
            imageAddButton.setImageResource(R.drawable.material_camera); // Reset to default icon
            removeImageButton.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Image removed", Toast.LENGTH_SHORT).show();
        });

        // Save button logic
        saveButton.setOnClickListener(v -> {
            Log.i(TAG, "Save button clicked");
            saveMoodEvent();
        });

        // Cancel button logic
        cancelButton.setOnClickListener(v -> {
            Log.i(TAG, "Cancel button clicked");
            dismiss();
        });

        return view;
    }

    /**
     *
     */
    private void saveMoodEvent() {
        Log.i(TAG, "Starting saveMoodEvent");

        // Get required information for mood event
        MoodEvent.EmotionalState emotionalState = (MoodEvent.EmotionalState) emotionalStateSpinner.getSelectedItem();
        MoodEvent.Visibility visibility = publicChip.isChecked() ? MoodEvent.Visibility.PRIVATE : MoodEvent.Visibility.PUBLIC;
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

        if (!reason.isEmpty()) {
            int charCount = reason.length();
            int wordCount = reason.split("\\s+").length;
            Log.d(TAG, "Reason validation - Char count: " + charCount + ", Word count: " + wordCount);
            if (reason.length() > 200) {
                Log.w(TAG, "Validation failed: Reason exceeds 200 chars");
                reasonEditText.setError("Reason must be 200 characters or fewer");
                isValid = false;
            } else {
                Log.d(TAG, "Reason validated: " + reason);
                reasonEditText.setError(null);
            }
        } else {
            Log.d(TAG, "Reason is empty (optional field)");
            reasonEditText.setError(null);
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
        moodEvent.setAttachedImage(imageBase64); // Set the attached image (may be null if removed)
        Log.d(TAG, "MoodEvent created: " + moodEvent);
        Log.d(TAG, "Timestamp (before save): " + (moodEvent.getTimestamp() != null ? moodEvent.getTimestamp().toString() : "null (to be set by server)"));

        // Handle location based on chip state
        Log.d(TAG, "Location chip checked: " + locationChip.isChecked());
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

    private String getCurrentUsername() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String username = currentUser.getDisplayName();
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
        requestPermissionLauncher = null;
        imagePickerLauncher = null; // Clean up
    }

}