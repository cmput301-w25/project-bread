package com.example.bread.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bread.R;
import com.example.bread.firebase.FirebaseService;

/**
 * MainActivity - Entry Point / Controller
 *
 * Role / Purpose:
 * Acts as the app's entry point. Determines whether the user is logged in by checking
 * SharedPreferences for a stored username and redirects accordingly to either the LoginPage or HomePage.
 * Also initializes Firebase context for app-wide service availability.
 *
 * Design Pattern:
 * -  Decides and redirects the user's flow based on stored state.
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    /**
     * Called when the activity is first created.
     * Sets up window insets, initializes Firebase services, and checks login status.
     *
     * @param savedInstanceState the saved state of the activity (if any)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize FirebaseService with context for network connectivity checks
        FirebaseService.initializeContext(getApplicationContext());

        SharedPreferences preferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        if (preferences.getString("username", "").isEmpty()) {
            Intent intent = new Intent(MainActivity.this, LoginPage.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(MainActivity.this, HomePage.class);
            startActivity(intent);
            finish();
        }
    }
}