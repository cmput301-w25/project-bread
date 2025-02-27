package com.example.bread.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bread.R;
import com.example.bread.utils.LocationHandler;

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

        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            Log.d("LoginPage", "Login Fetched User Location: " + LocationHandler.getInstance(this).getLastLocation().toString());
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationHandler.stopLocationUpdates();
    }
}