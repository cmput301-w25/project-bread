package com.example.bread.fragment;

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
import android.widget.FrameLayout;
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
import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.example.bread.view.LoginPage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the settings page of the app, where users can edit their account details, and log out.
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

        // Add a button for changing username
        Button changeUsernameButton = view.findViewById(R.id.change_username_button);
        if (changeUsernameButton != null) {
            changeUsernameButton.setOnClickListener(v -> showChangeUsernameDialog());
        }

        // Load user's profile picture
        loadProfilePicture();

        // Set up profile picture change button
        if (profileChangeButton != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2) {
                profileChangeButton.setOnClickListener(v -> showImagePickerOptions());
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
     * Loads the user's profile picture from Firestore and sets it on the profile image button
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
     * Shows options for selecting an image from gallery
     */
    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    private void showImagePickerOptions() {
        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Registers the image picker launcher
     */
    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == getActivity().RESULT_OK) {
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
     * Updates the profile picture in Firestore
     *
     * @param base64Image the Base64 encoded image
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

    /**
     * Shows a dialog for changing the username
     */
    private void showChangeUsernameDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Username");

        // Create a simple EditText directly
        final EditText newUsernameEditText = new EditText(getContext());
        newUsernameEditText.setHint("Enter new username");
        newUsernameEditText.setText(currentUsername);
        newUsernameEditText.setPadding(50, 30, 50, 30);

        // Set padding around the EditText
        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        container.addView(newUsernameEditText, params);

        builder.setView(container);

        builder.setPositiveButton("Save", null); // Will set this properly later
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent auto-dismissal on validation errors
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String newUsername = newUsernameEditText.getText().toString().trim();

            // Validate the username
            if (newUsername.isEmpty()) {
                newUsernameEditText.setError("Username cannot be empty");
                return;
            }

            if (newUsername.equals(currentUsername)) {
                dialog.dismiss();
                return; // No change needed
            }

            // Validate format: only letters, numbers, and underscores
            if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
                newUsernameEditText.setError("Username can only contain letters, numbers, and underscores");
                return;
            }

            // Check if username already exists
            participantRepository.checkIfUsernameExists(newUsername, exists -> {
                if (exists) {
                    newUsernameEditText.setError("Username already exists");
                } else {
                    // Update the username in Firebase Auth and Firestore
                    updateUsername(newUsername, dialog);
                }
            }, e -> {
                Log.e(TAG, "Error checking if username exists", e);
                Toast.makeText(getContext(), "Error checking username availability", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /**
     * Updates the username in FirebaseAuth and Firestore
     *
     * @param newUsername the new username
     * @param dialog the dialog to dismiss on success
     */
    private void updateUsername(String newUsername, AlertDialog dialog) {
        if (getActivity() == null) return;

        // Show a progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(getContext())
                .setMessage("Updating username... This may take a moment.")
                .setCancelable(false)
                .show();

        // Get current user info
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            progressDialog.dismiss();
            return;
        }

        // First, get the current participant data
        participantRepository.fetchBaseParticipant(currentUsername, participant -> {
            if (participant == null) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Error fetching user data", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create the new participant with the updated username
            participant.setUsername(newUsername);

            // Add the participant with the new username
            participantRepository.addParticipant(participant, unused -> {
                // Migrate all relationships (followers, following, mood events)
                migrateUserRelationships(currentUsername, newUsername, () -> {
                    // Update the username in FirebaseAuth profile
                    auth.getCurrentUser().updateProfile(
                            new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(newUsername)
                                    .build()
                    ).addOnSuccessListener(aVoid -> {
                        // Delete the old document
                        participantRepository.getParticipantRef(currentUsername).delete()
                                .addOnSuccessListener(unused2 -> {
                                    SharedPreferences preferences = getActivity().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
                                    preferences.edit().putString("username", newUsername).apply();

                                    // Update the current username
                                    currentUsername = newUsername;

                                    progressDialog.dismiss();
                                    dialog.dismiss();

                                    Toast.makeText(getContext(), "Username updated successfully", Toast.LENGTH_SHORT).show();

                                    // Refresh the app by restarting HomePage
                                    Intent intent = new Intent(getActivity(), com.example.bread.view.HomePage.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    getActivity().finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Log.e(TAG, "Error deleting old participant document", e);
                                    Toast.makeText(getContext(), "Error updating username", Toast.LENGTH_SHORT).show();
                                });
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error updating Auth profile", e);
                        Toast.makeText(getContext(), "Error updating username", Toast.LENGTH_SHORT).show();
                    });
                }, error -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error migrating user relationships", error);
                    Toast.makeText(getContext(), "Error updating username relationships", Toast.LENGTH_SHORT).show();
                });
            }, e -> {
                progressDialog.dismiss();
                Log.e(TAG, "Error adding participant with new username", e);
                Toast.makeText(getContext(), "Error updating username", Toast.LENGTH_SHORT).show();
            });
        }, e -> {
            progressDialog.dismiss();
            Log.e(TAG, "Error fetching participant", e);
            Toast.makeText(getContext(), "Error updating username", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Migrates all user relationships between the old username and the new one
     *
     * @param oldUsername the old username
     * @param newUsername the new username
     * @param onComplete callback when migration is complete
     * @param onError callback when an error occurs
     */
    private void migrateUserRelationships(String oldUsername, String newUsername, Runnable onComplete, OnFailureListener onError) {
        DocumentReference oldUserRef = participantRepository.getParticipantRef(oldUsername);
        DocumentReference newUserRef = participantRepository.getParticipantRef(newUsername);

        // 1. Migrate followers: Move all entries in the followers collection
        migrateFollowersAndFollowing(oldUsername, newUsername, () -> {
            // 2. Migrate mood events: Update participant reference in all mood events
            migrateMoodEvents(oldUsername, newUsername, onComplete, onError);
        }, onError);
    }

    /**
     * Migrates followers and following relationships between old and new username
     */
    private void migrateFollowersAndFollowing(String oldUsername, String newUsername, Runnable onComplete, OnFailureListener onError) {
        // First, get the list of followers
        participantRepository.fetchFollowers(oldUsername, followers -> {
            // No followers to migrate
            if (followers.isEmpty()) {
                // Now get the list of users this person is following
                migrateFollowing(oldUsername, newUsername, onComplete, onError);
                return;
            }

            // Migrate each follower
            final int[] completedCount = {0};
            for (String followerUsername : followers) {
                // Update the following collection of each follower
                updateFollowingForUser(followerUsername, oldUsername, newUsername, () -> {
                    // Add this follower to the new user's followers
                    participantRepository.addFollower(newUsername, followerUsername, unused -> {
                        completedCount[0]++;
                        if (completedCount[0] >= followers.size()) {
                            // Now migrate following
                            migrateFollowing(oldUsername, newUsername, onComplete, onError);
                        }
                    }, e -> {
                        Log.e(TAG, "Error adding follower to new username", e);
                        completedCount[0]++;
                        if (completedCount[0] >= followers.size()) {
                            migrateFollowing(oldUsername, newUsername, onComplete, onError);
                        }
                    });
                }, e -> {
                    Log.e(TAG, "Error updating following for follower", e);
                    completedCount[0]++;
                    if (completedCount[0] >= followers.size()) {
                        migrateFollowing(oldUsername, newUsername, onComplete, onError);
                    }
                });
            }
        }, e -> {
            Log.e(TAG, "Error fetching followers", e);
            onError.onFailure(e);
        });
    }

    /**
     * Migrates following relationships
     */
    private void migrateFollowing(String oldUsername, String newUsername, Runnable onComplete, OnFailureListener onError) {
        participantRepository.fetchFollowing(oldUsername, following -> {
            // No users being followed
            if (following.isEmpty()) {
                onComplete.run();
                return;
            }

            final int[] completedCount = {0};
            for (String followedUsername : following) {
                // Add to new user's following
                participantRepository.addFollowing(newUsername, followedUsername, unused -> {
                    // Update the followers collection of each followed user
                    updateFollowerForUser(followedUsername, oldUsername, newUsername, () -> {
                        completedCount[0]++;
                        if (completedCount[0] >= following.size()) {
                            onComplete.run();
                        }
                    }, e -> {
                        Log.e(TAG, "Error updating follower for followed user", e);
                        completedCount[0]++;
                        if (completedCount[0] >= following.size()) {
                            onComplete.run();
                        }
                    });
                }, e -> {
                    Log.e(TAG, "Error adding following to new username", e);
                    completedCount[0]++;
                    if (completedCount[0] >= following.size()) {
                        onComplete.run();
                    }
                });
            }
        }, e -> {
            Log.e(TAG, "Error fetching following", e);
            onError.onFailure(e);
        });
    }

    /**
     * Update follower reference in a user's following collection
     */
    private void updateFollowingForUser(String username, String oldFollowedUsername, String newFollowedUsername, Runnable onComplete, OnFailureListener onError) {
        // First remove the old following reference
        participantRepository.getParticipantRef(username)
                .collection("following")
                .document(oldFollowedUsername)
                .delete()
                .addOnSuccessListener(unused -> {
                    // Then add the new following reference
                    Map<String, String> followingData = new HashMap<>();
                    followingData.put("username", newFollowedUsername);
                    participantRepository.getParticipantRef(username)
                            .collection("following")
                            .document(newFollowedUsername)
                            .set(followingData)
                            .addOnSuccessListener(unused2 -> onComplete.run())
                            .addOnFailureListener(onError);
                })
                .addOnFailureListener(onError);
    }

    /**
     * Update following reference in a user's followers collection
     */
    private void updateFollowerForUser(String username, String oldFollowerUsername, String newFollowerUsername, Runnable onComplete, OnFailureListener onError) {
        // First remove the old follower reference
        participantRepository.getParticipantRef(username)
                .collection("followers")
                .document(oldFollowerUsername)
                .delete()
                .addOnSuccessListener(unused -> {
                    // Then add the new follower reference
                    Map<String, String> followerData = new HashMap<>();
                    followerData.put("username", newFollowerUsername);
                    participantRepository.getParticipantRef(username)
                            .collection("followers")
                            .document(newFollowerUsername)
                            .set(followerData)
                            .addOnSuccessListener(unused2 -> onComplete.run())
                            .addOnFailureListener(onError);
                })
                .addOnFailureListener(onError);
    }

    /**
     * Migrates mood events to use the new username
     */
    private void migrateMoodEvents(String oldUsername, String newUsername, Runnable onComplete, OnFailureListener onError) {
        DocumentReference oldUserRef = participantRepository.getParticipantRef(oldUsername);
        DocumentReference newUserRef = participantRepository.getParticipantRef(newUsername);

        // Find all mood events with the old participant reference
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("moodEvents")
                .whereEqualTo("participantRef", oldUserRef)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    List<MoodEvent> moodEvents = queryDocumentSnapshots.toObjects(MoodEvent.class);
                    // Update document IDs
                    for (int i = 0; i < moodEvents.size(); i++) {
                        MoodEvent event = moodEvents.get(i);
                        event.setId(queryDocumentSnapshots.getDocuments().get(i).getId());
                    }

                    final int[] completedCount = {0};
                    MoodEventRepository moodEventRepo = new MoodEventRepository();

                    for (MoodEvent event : moodEvents) {
                        // Update participant reference
                        event.setParticipantRef(newUserRef);

                        // Update the mood event
                        moodEventRepo.updateMoodEvent(event, unused -> {
                            completedCount[0]++;
                            if (completedCount[0] >= moodEvents.size()) {
                                onComplete.run();
                            }
                        }, e -> {
                            Log.e(TAG, "Error updating mood event", e);
                            completedCount[0]++;
                            if (completedCount[0] >= moodEvents.size()) {
                                onComplete.run();
                            }
                        });
                    }
                })
                .addOnFailureListener(onError);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfilePicture();
    }
}