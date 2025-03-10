package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.bread.R;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.view.LoginPage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import java.util.Objects;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button editAccountButton = view.findViewById(R.id.edit_account_button);
        editAccountButton.setOnClickListener(v -> {
            editName();
        });

        Button logoutButton = view.findViewById(R.id.log_out_button);
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

        Button deleteAccountButton = view.findViewById(R.id.delete_account_button );
        deleteAccountButton.setOnClickListener(v -> {
            // Delete user account and all associated mood events
            // TODO: implement delete account functionality (?)
            //https://firebase.google.com/docs/auth/android/manage-users#delete_a_user
        });
        return view;
    }

    public void editName(){
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

                        editFirstname.setText(firstname != null ? firstname : "test");
                        editLastname.setText(lastname != null ? lastname : "test");

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
}
