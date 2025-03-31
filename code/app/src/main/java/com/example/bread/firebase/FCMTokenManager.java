package com.example.bread.firebase;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Role / Purpose
 * Manages Firebase Cloud Messaging (FCM) tokens by saving them to Firestore.
 * This class provides methods to store and retrieve FCM tokens associated with user accounts.
 * <p>
 * Design Patterns:
 * - Singleton: Ensures a single instance of FirebaseService.
 * <p>
 * Outstanding Issues:
 * - No method to delete tokens when a user logs out or uninstalls the app.
 */
public class FCMTokenManager {
    private static final String TAG = "FCMTokenManager";
    private static final String COLLECTION_TOKENS = "fcm_tokens";
    private static FirebaseService firebaseService;

    /**
     * Interface to listen for token fetch results.
     */
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
<<<<<<< Updated upstream


}
=======
}
>>>>>>> Stashed changes
