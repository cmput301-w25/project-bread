package com.example.bread;

import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseEmulatorRule {
    private static final String ANDROID_LOCALHOST = "10.0.2.2";
    private static boolean isInitialized = false;

    public static synchronized void initializeEmulators() {
        if (!isInitialized) {
            try {
                // Initialize Firebase App if not already initialized
//                if (FirebaseApp.().isEmpty()) {
//                    // You might need to pass the application context here
//                    // FirebaseApp.initializeApp(context);
//                }

                // Set up Firestore Emulator
                FirebaseFirestore.getInstance().useEmulator(ANDROID_LOCALHOST, 8080);

                // Set up Auth Emulator
                FirebaseAuth.getInstance().useEmulator(ANDROID_LOCALHOST, 9099);

                isInitialized = true;
                Log.i("FirebaseEmulatorRule", "Emulators initialized successfully");
            } catch (Exception e) {
                Log.e("FirebaseEmulatorRule", "Error initializing emulators", e);
                throw new RuntimeException("Failed to initialize Firebase emulators", e);
            }
        }
    }
}