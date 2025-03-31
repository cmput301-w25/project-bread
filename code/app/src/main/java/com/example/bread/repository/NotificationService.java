package com.example.bread.repository;

import android.util.Log;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * NotificationService - Repository
 * <p>
 * Role / Purpose
 * Provides a utility for sending follow request notifications between users by writing to the Firestore "notifications" collection.
 * Encapsulates logic for constructing and storing notification data using the Notification model.
 * <p>
 * Design Patterns
 * Singleton Pattern: Lazily initializes a shared FirebaseService instance for database access.
 * Service Pattern: Acts as a standalone utility for managing notifications separate from UI logic.
 * <p>
 * Outstanding Issues
 * - Currently limited to sending follow request notifications; other notification types may need to be added later.
 * - No support for real-time listening, reading, or removing notifications.
 */

public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static FirebaseService firebaseService;

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
     * Send a follow request notification by saving it to Firestore
     *
     * @param senderUsername
     * @param recipientUsername
     */
    public static void sendFollowRequestNotification(String senderUsername, String recipientUsername) {
        if (senderUsername == null || senderUsername.isEmpty() || recipientUsername == null || recipientUsername.isEmpty()) {
            Log.e(TAG, "Invalid sender or recipient username");
            return;
        }

        Log.d(TAG, "Sending follow request notification from " + senderUsername + " to " + recipientUsername);

        // Create notification using model class
        Notification notification = Notification.createFollowRequestNotification(
                senderUsername, recipientUsername);

        // Save notification to Firestore using FirebaseService
        FirebaseFirestore db = getFirebaseService().getDb();
        db.collection(COLLECTION_NOTIFICATIONS)
                .add(notification.toMap())
                .addOnSuccessListener(documentReference ->
                        Log.d(TAG, "Follow request notification saved with ID: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error saving follow request notification: " + e.getMessage(), e));
    }
}