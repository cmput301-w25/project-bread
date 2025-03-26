package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.bread.R;
import com.example.bread.model.Participant;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.example.bread.view.LoginPage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import java.util.Objects;

/**
 * Represents the settings page of the app, where users can edit their account details, and log out.
 */
public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private ImageButton profileChangeButton;
    private ImageView sentioLogo;
    private ParticipantRepository participantRepository;
    private String currentUsername;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        participantRepository = new ParticipantRepository();

        // Get current username
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUsername = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        }
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

        // Find and hide the delete account button
        Button deleteAccountButton = view.findViewById(R.id.delete_account_button);
        if (deleteAccountButton != null) {
            deleteAccountButton.setVisibility(View.GONE);
        }

        // Load user's profile picture
        loadProfilePicture();

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
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
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