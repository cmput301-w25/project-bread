package com.example.bread.firebase;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

public class FCMTokenManager {
    private static final String TAG = "FCMTokenManager";
    private static final String COLLECTION_TOKENS = "fcm_tokens";
    private static FirebaseService firebaseService;
    public interface OnTokenFetchListener {

        void onTokenFetch(String token);
    }

    /**
     * Get the FirebaseService instance
     */
    private static synchronized FirebaseService getFirebaseService() {
        if (firebaseService == null) {
            firebaseService = new FirebaseService();
        }
        return firebaseService;
    }


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

        FirebaseFirestore db = getFirebaseService().getDb();
        db.collection(COLLECTION_TOKENS).document(username)
                .set(tokenData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved for user: " + username))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving token for user: " + username, e));
    }



}
