package com.example.bread.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bread.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * LoginPage - view
 *
 * Role / Purpose:
 * Handles user login functionality. Accepts email and password input, validates the fields,
 * communicates with Firebase Authentication, and redirects users to the HomePage upon success.
 * Also allows navigation to the SignUp page for new users.
 *
 * Design Pattern:
 * - View Pattern: Manages the logic and user input for login.
 * - Delegation Pattern: Uses Firebase callbacks to handle success and failure in authentication.
 *
 * Outstanding Issues / Comments:
 * - UI/UX could benefit from animations or loading indicators during login attempts.
 * -
 */
public class LoginPage extends AppCompatActivity {

    private static final String TAG = "LoginPage";

    private EditText emailEditText, passwordEditText;

    private FirebaseAuth mAuth;
    /**
     * Called when the activity is starting.
     * Sets up the login form, initializes Firebase Auth, and sets button listeners.
     *
     * @param savedInstanceState the saved instance state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        Button loginButton = findViewById(R.id.login_button);

        TextView signupButton = findViewById(R.id.login_signup_button);
        signupButton.setPaintFlags(signupButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        emailEditText = findViewById(R.id.login_email_text);
        passwordEditText = findViewById(R.id.login_password_text);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                if (email.isEmpty()) {
                    emailEditText.setError("Email is required");
                }
                if (password.isEmpty()) {
                    passwordEditText.setError("Password is required");
                }
                return;
            }

            signInUser(email, password, authResult -> {
                FirebaseUser user = authResult.getUser();
                SharedPreferences preferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                if (user != null) {
                    editor.putString("username", user.getDisplayName());
                } else {
                    Log.e(TAG, "User is null after signing in");
                    Toast.makeText(LoginPage.this, "Failed to sign in.", Toast.LENGTH_SHORT).show();
                    return;
                }
                editor.apply();
                Intent intent = new Intent(LoginPage.this, HomePage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }, e -> {
                Log.e(TAG, "Failed to sign in user with email: " + email, e);
                Toast.makeText(LoginPage.this, "Failed to sign in.", Toast.LENGTH_SHORT).show();
            });
        });

        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, SignupPage.class);
            startActivity(intent);
        });
    }
    /**
     * Called when the activity becomes visible to the user.
     * Automatically redirects to HomePage if the user is already signed in.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Save the user's username in SharedPreferences
            SharedPreferences preferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("username", currentUser.getDisplayName());
            editor.apply();
            Intent intent = new Intent(LoginPage.this, HomePage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Log.d(TAG, "User is not signed in");
        }
    }
    /**
     * Signs in the user using Firebase Authentication.
     *
     * @param email             user's email
     * @param password          user's password
     * @param onSuccessListener callback for success
     * @param onFailureListener callback for failure
     */
    private void signInUser(@NonNull String email, @NonNull String password, @NonNull OnSuccessListener<AuthResult> onSuccessListener, OnFailureListener onFailureListener) {
        mAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to sign in user with email: " + email, e));
    }
}