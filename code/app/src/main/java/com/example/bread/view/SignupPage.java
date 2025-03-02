package com.example.bread.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bread.R;
import com.example.bread.model.Participant;
import com.example.bread.repository.ParticipantRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignupPage extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, passwordEditText, firstNameEditText, lastNameEditText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        usernameEditText = findViewById(R.id.signup_username_text);
        emailEditText = findViewById(R.id.signup_email_text);
        passwordEditText = findViewById(R.id.signup_password_text);
        firstNameEditText = findViewById(R.id.signup_firstname_text);
        lastNameEditText = findViewById(R.id.signup_lastname_text);
        Button signupButton = findViewById(R.id.signup_button);

        ParticipantRepository participantRepository = new ParticipantRepository();

        signupButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String firstName = firstNameEditText.getText().toString();
            String lastName = lastNameEditText.getText().toString();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                if (username.isEmpty()) {
                    usernameEditText.setError("Username is required");
                }
                if (email.isEmpty()) {
                    emailEditText.setError("Email is required");
                }
                if (password.isEmpty()) {
                    passwordEditText.setError("Password is required");
                }
                if (firstName.isEmpty()) {
                    firstNameEditText.setError("First Name is required");
                }
                if (lastName.isEmpty()) {
                    lastNameEditText.setError("Last Name is required");
                }
                return;
            }

            // TODO: Check if the password is valid

            participantRepository.checkIfUsernameExists(username, exists -> {
                if (exists) {
                    usernameEditText.setError("Username already exists");
                } else {
                    signUpUser(email, password, authResult -> {
                        FirebaseUser currentUser = authResult.getUser();
                        if (currentUser != null) {
                            updateUserDisplayName(username, currentUser, aVoid -> {
                                Log.d("SignupPage", "User signed up successfully");
                                Toast.makeText(SignupPage.this, "Sign Up successful!", Toast.LENGTH_SHORT).show();

                                SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("username", username);
                                editor.apply();
                            }, null);
                            participantRepository.addParticipant(new Participant(username, email, firstName, lastName), aVoid -> {
                                Log.d("SignupPage", "Participant added successfully");
                                finish();
                            }, e -> {
                                Log.e("SignupPage", "Failed to add participant", e);
                                Toast.makeText(SignupPage.this, "Sign Up failed. Try again!", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            Log.e("SignupPage", "Failed to sign up user");
                            Toast.makeText(SignupPage.this, "Sign Up failed. Try again!", Toast.LENGTH_SHORT).show();
                        }
                    }, e -> {
                        Log.e("SignupPage", "Failed to sign up user", e);
                        Toast.makeText(SignupPage.this, "Sign Up failed. Try again!", Toast.LENGTH_SHORT).show();
                    });
                }
            }, e -> {
                Log.e("SignupPage", "Failed to check if username exists", e);
                Toast.makeText(SignupPage.this, "Sign Up failed. Try again!", Toast.LENGTH_SHORT).show();
            });

        });
    }

    private void signUpUser(@NonNull String email, @NonNull String password, @NonNull OnSuccessListener<AuthResult> onSuccessListener, OnFailureListener onFailureListener) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> {
                    Log.e("SignupPage", "Failed to sign up user", e);
                    Toast.makeText(SignupPage.this, "Sign Up failed. Try again!", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserDisplayName(@NonNull String username, @NonNull FirebaseUser currentUser, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        currentUser.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(username).build())
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> {
                    Log.e("SignupPage", "Failed to update user display name", e);
                    Toast.makeText(SignupPage.this, "Sign Up failed. Try again!", Toast.LENGTH_SHORT).show();
                });
    }
}