package com.example.bread.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bread.R;
import com.example.bread.utils.LocationHandler;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginPage extends AppCompatActivity {

    private static final String TAG = "LoginPage";

    /**
     * These two fields are used to handle location permissions and fetching the user's location.
     * They are required in all the activities that need to fetch the user's location.
     * Always call stopLocationUpdates() in the onDestroy() / onStop() method of the activity.
     */
    private LocationHandler locationHandler;
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    locationHandler.fetchUserLocation();
                } else {
                    Log.e(TAG, "Location permission denied - cannot fetch location");
                }
            });

    private EditText emailEditText, passwordEditText;

    private FirebaseAuth mAuth;

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

        locationHandler = LocationHandler.getInstance(this);
        locationHandler.requestLocationPermission(locationPermissionLauncher);

        mAuth = FirebaseAuth.getInstance();

        Button loginButton = findViewById(R.id.login_button);
        Button signupButton = findViewById(R.id.login_signup_button);
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

    @Override
    protected void onStop() {
        super.onStop();
        locationHandler.stopLocationUpdates();
    }

    private void signInUser(@NonNull String email, @NonNull String password, @NonNull OnSuccessListener<AuthResult> onSuccessListener, OnFailureListener onFailureListener) {
        mAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to sign in user with email: " + email, e));
    }
}