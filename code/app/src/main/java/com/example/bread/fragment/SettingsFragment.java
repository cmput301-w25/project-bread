package com.example.bread.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresExtension;
import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.example.bread.view.LoginPage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.util.Objects;

/**
 * SettingsFragment - Fragment
 * <p>
 * Role / Purpose
 * Allows users to view and update their account settings including profile image and name.
 * Supports logout functionality and navigational dismissal back to previous screen.
 * <p>
 * Design Pattern
 * Fragment Pattern: Encapsulates account settings UI.
 * MVC Pattern: Connects UI to data using ParticipantRepository and Firebase interactions.
 * <p>
 * Outstanding Issues / Comments
 * Profile picture updates do not confirm upload completion before UI feedback.
 * Image handling requires API 30+ (R+); limited device support without fallback.
 */

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private ImageButton profileChangeButton;
    private ImageView sentioLogo;
    private ParticipantRepository participantRepository;
    private ImageView closeButton;
    private String currentUsername;

    // Image related variables
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private String imageBase64;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        participantRepository = new ParticipantRepository();

        // Get current username
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUsername = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        }

        // Register the image picker
        registerImagePicker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize views
        profileChangeButton = view.findViewById(R.id.profile_change_button);
        sentioLogo = view.findViewById(R.id.sentio_logo_settings);
        Button editAccountButton = view.findViewById(R.id.edit_account_button);
        Button logoutButton = view.findViewById(R.id.log_out_button);
        closeButton = view.findViewById(R.id.settings_close_button);

        closeButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().setCustomAnimations(
                    R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
            ).remove(SettingsFragment.this).commit();
        });

        // Find and hide the delete account button
        Button deleteAccountButton = view.findViewById(R.id.delete_account_button);
        if (deleteAccountButton != null) {
            deleteAccountButton.setVisibility(View.GONE);
        }

        // Load user's profile picture
        loadProfilePicture();

        // Set up profile picture change button
        if (profileChangeButton != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2) {
                profileChangeButton.setOnClickListener(v -> pickImage());
            } else {
                profileChangeButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "Photo picking not supported on this device", Toast.LENGTH_SHORT).show();
                });
            }
        }

        // Edit account button
        editAccountButton.setOnClickListener(v -> {
            editName();
        });

        // Logout button
        logoutButton.setOnClickListener(v -> {
            // Clear SharedPreferences
            SharedPreferences preferences = getActivity().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
            preferences.edit().clear().apply();

            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            // Go back to login page
            Intent intent = new Intent(getActivity(), LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    /**
     * Loads the user's profile picture from Firestore and sets it on the profile image button.
     * If a valid image exists, it is decoded from Base64 and displayed.
     */
    private void loadProfilePicture() {
        if (currentUsername == null) return;

        participantRepository.fetchBaseParticipant(currentUsername, participant -> {
            if (participant != null && participant.getProfilePicture() != null) {
                Bitmap bitmap = ImageHandler.base64ToBitmap(participant.getProfilePicture());
                if (bitmap != null && profileChangeButton != null) {
                    profileChangeButton.setImageBitmap(bitmap);
                }
            }
        }, e -> Log.e(TAG, "Error loading profile picture", e));
    }

    /**
     * Opens the system image picker to allow the user to select a new profile picture.
     * Requires Android 11 (API level 30) with extension version 2 or higher.
     */
    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    private void pickImage() {
        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Registers an ActivityResultLauncher to handle the result of image selection.
     * Converts the selected image to Base64 and triggers a profile update in Firestore.
     */
    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        getActivity();
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            if (result.getData() != null) {
                                try {
                                    Uri imageUri = result.getData().getData();
                                    if (imageUri != null) {
                                        // Set the image on the button
                                        profileChangeButton.setImageURI(imageUri);

                                        // Convert the image to Base64
                                        imageBase64 = ImageHandler.compressImageToBase64(requireContext(), imageUri);

                                        // Update the profile picture in Firestore
                                        updateProfilePicture(imageBase64);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing selected image", e);
                                    Toast.makeText(getContext(), "Error processing selected image", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Log.d(TAG, "Image selection cancelled");
                        }
                    }
                }
        );
    }

    /**
     * Updates the profile picture of the current user in Firestore.
     *
     * @param base64Image The new profile picture encoded as a Base64 string.
     */
    private void updateProfilePicture(String base64Image) {
        if (currentUsername == null || base64Image == null) return;

        DocumentReference userRef = participantRepository.getParticipantRef(currentUsername);
        userRef.update("profilePicture", base64Image)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Profile picture updated successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating profile picture", e);
                });
    }

    /**
     * Opens a dialog allowing the user to edit their first and last name.
     * Pre-fills the dialog with existing name data and updates Firestore upon confirmation.
     */
    public void editName() {
        if (getContext() == null) return;

        // Retrieving user information
        ParticipantRepository userRepo = new ParticipantRepository();
        String username = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getDisplayName();
        DocumentReference participantRef = userRepo.getParticipantRef(username);

        // Initializing dialog view, pre filling text fields, and displaying
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Account Details");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_account_details, null);

        EditText editFirstname = dialogView.findViewById(R.id.edit_firstname);
        EditText editLastname = dialogView.findViewById(R.id.edit_lastname);

        // Retrieving first and last name of user
        userRepo.fetchParticipantByRef(participantRef,
                participant -> {
                    if (participant != null) {

                        String firstname = participant.getFirstName();
                        String lastname = participant.getLastName();

                        editFirstname.setText(firstname != null ? firstname : "");
                        editLastname.setText(lastname != null ? lastname : "");

                        Log.d(TAG, "Fetched participant: " + participant.getFirstName() + " " + participant.getLastName());
                    } else {
                        Log.d(TAG, "Participant not found.");
                    }
                },
                e -> Log.e(TAG, "Failed to fetch participant", e)
        );
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Retrieving input for updated account details
            String newFirstName = editFirstname.getText().toString().trim();
            String newLastName = editLastname.getText().toString().trim();

            //update values in firebase
            participantRef
                    .update(
                            "firstName", newFirstName,
                            "lastName", newLastName)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                            Toast.makeText(getContext(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
                            Toast.makeText(getContext(), "Failed to update name", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        // Set up dialog view and buttons
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfilePicture();
    }
}