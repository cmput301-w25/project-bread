package com.example.bread.fragment;

import static android.app.PendingIntent.getActivity;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.view.LoginPage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.Objects;

public class SettingsFragment extends Fragment {

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
        //https://firebase.google.com/docs/firestore/query-data/get-data
        participantRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();

                    String firstname = document.getString("firstName");
                    String lastname = document.getString("lastName");

                    editFirstname.setText(firstname != null ? firstname : "test");
                    editLastname.setText(lastname != null ? lastname : "test");

                    if (document.exists()) {
                        Log.d("Settings Fragment", "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d("Settings Fragment", "No such document");
                    }
                } else {
                    Log.d("Settings Fragment", "get failed with ", task.getException());
                }
            }
        });

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Retrieving input for updated account details
            String newFirstName = editFirstname.getText().toString().trim();
            String newLastName = editLastname.getText().toString().trim();

            //update values in firebase
            https://firebase.google.com/docs/firestore/manage-data/add-data
            participantRef
                    .update(
                            "firstName", newFirstName,
                            "lastName", newLastName)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Settings Fragment", "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("Settings Fragment", "Error updating document", e);
                        }
                    });
        });
        // Set up dialog view and buttons
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
