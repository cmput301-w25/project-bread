package com.example.bread.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

import java.util.regex.Pattern;

public class SignupPage extends AppCompatActivity {

    private static final String TAG = "SignupPage";
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]+$";

    private EditText usernameEditText, emailEditText, passwordEditText, firstNameEditText, lastNameEditText;
    private ProgressBar progressBar;
    private Button signupButton;

    private FirebaseAuth mAuth;
    private ParticipantRepository participantRepository;

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
        participantRepository = new ParticipantRepository();

        initializeUI();
        setupListeners();
    }

    private void initializeUI() {
        usernameEditText = findViewById(R.id.signup_username_text);
        emailEditText = findViewById(R.id.signup_email_text);
        passwordEditText = findViewById(R.id.signup_password_text);
        firstNameEditText = findViewById(R.id.signup_firstname_text);
        lastNameEditText = findViewById(R.id.signup_lastname_text);
        signupButton = findViewById(R.id.signup_button);

        progressBar = findViewById(R.id.signup_progress_bar);
        if (progressBar == null) {
            Log.w(TAG, "ProgressBar not found in layout. Loading indicator will not be shown.");
        }
    }

    private void setupListeners() {
        signupButton.setOnClickListener(v -> attemptSignup());
    }

    private void attemptSignup() {
        // Reset errors
        usernameEditText.setError(null);
        emailEditText.setError(null);
        passwordEditText.setError(null);
        firstNameEditText.setError(null);
        lastNameEditText.setError(null);

        // Get input values
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        if (!validateInputFields(username, email, password, firstName, lastName)) {
            return;
        }
        showLoading(true);
        checkUsernameAvailability(username, email, password, firstName, lastName);
    }

    private boolean validateInputFields(String username, String email, String password, String firstName, String lastName) {
        boolean isValid = true;

        // Check for empty fields
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Username is required");
            isValid = false;
        }
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            isValid = false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            isValid = false;
        }
        if (TextUtils.isEmpty(firstName)) {
            firstNameEditText.setError("First Name is required");
            isValid = false;
        }
        if (TextUtils.isEmpty(lastName)) {
            lastNameEditText.setError("Last Name is required");
            isValid = false;
        }

        if (!TextUtils.isEmpty(username) && username.length() < MIN_USERNAME_LENGTH) {
            usernameEditText.setError("Username must be at least " + MIN_USERNAME_LENGTH + " characters");
            isValid = false;
        }

        if (!TextUtils.isEmpty(username) && !Pattern.matches(USERNAME_PATTERN, username)) {
            usernameEditText.setError("Username can only contain letters, numbers, and underscores");
            isValid = false;
        }

        if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            isValid = false;
        }

        if (!TextUtils.isEmpty(password)) {
            String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$";

            if (!Pattern.matches(passwordPattern, password)) {
                passwordEditText.setError("Password must be at least 6 characters and include at least one digit, " +
                        "one uppercase letter, one lowercase letter, and one special character");
                isValid = false;
            }
        }

        return isValid;
    }

    private void checkUsernameAvailability(String username, String email, String password, String firstName, String lastName) {
        participantRepository.checkIfUsernameExists(username, exists -> {
            if (exists) {
                showLoading(false);
                usernameEditText.setError("Username already exists");
                usernameEditText.requestFocus();
            } else {
                createUserAccount(email, password, username, firstName, lastName);
            }
        }, e -> {
            showLoading(false);
            Log.e(TAG, "Failed to check if username exists", e);
            Toast.makeText(SignupPage.this, "Network error", Toast.LENGTH_SHORT).show();
        });
    }

    private void createUserAccount(String email, String password, String username, String firstName, String lastName) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(this, authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        updateUserProfile(user, username, email, firstName, lastName);
                    } else {
                        showLoading(false);
                        Toast.makeText(SignupPage.this, "Account creation failed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to create user", e);
                    Toast.makeText(SignupPage.this, "Account creation failed.", Toast.LENGTH_SHORT).show();

                });
    }

    private void updateUserProfile(FirebaseUser user, String username, String email, String firstName, String lastName) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build();

        user.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile updated.");
                    saveUserToFirestore(username, email, firstName, lastName);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to update user profile", e);
                    Toast.makeText(SignupPage.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserToFirestore(String username, String email, String firstName, String lastName) {
        Participant participant = new Participant(username, email, firstName, lastName);

        participantRepository.addParticipant(participant, aVoid -> {
            Log.d(TAG, "Participant added successfully");

            saveToSharedPreferences(username);
            showLoading(false);
            goToHomePage();

        }, e -> {
            showLoading(false);
            Log.e(TAG, "Failed to add participant", e);
            Toast.makeText(SignupPage.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveToSharedPreferences(String username) {
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.apply();
    }

    private void goToHomePage() {
        Intent intent = new Intent(SignupPage.this, HomePage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        signupButton.setEnabled(!isLoading);
    }
}