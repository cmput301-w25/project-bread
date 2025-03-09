package com.example.bread.firebase;

import android.util.Log;

import com.example.bread.repository.ParticipantRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FCMTokenManager {
    private static final String TAG = "FCMTokenManager";
    private static final String COLLECTION_TOKENS = "fcm_tokens";

    /**
     * Save an FCM token to Firestore for a specific user
     */
    public static void saveTokenToFirestore(String username, String token) {
        if (username == null || username.isEmpty() || token == null || token.isEmpty()) {
            Log.e(TAG, "Invalid username or token");
            return;
        }

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", token);
        tokenData.put("username", username);
        tokenData.put("updatedAt", System.currentTimeMillis());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_TOKENS).document(username)
                .set(tokenData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved for user: " + username))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving token for user: " + username, e));
    }

    /**
     * Get an FCM token from Firestore for a specific user
     */
    public static void getTokenForUser(String username, OnTokenFetchListener listener) {
        if (username == null || username.isEmpty()) {
            listener.onTokenFetch(null);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_TOKENS).document(username)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String token = documentSnapshot.getString("token");
                        listener.onTokenFetch(token);
                    } else {
                        Log.d(TAG, "No token found for user: " + username);
                        listener.onTokenFetch(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching token for user: " + username, e);
                    listener.onTokenFetch(null);
                });
    }

    public interface OnTokenFetchListener {
        void onTokenFetch(String token);
    }
}