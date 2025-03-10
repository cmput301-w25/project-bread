package com.example.bread.repository;

import android.util.Log;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.Notification;
import com.example.bread.utils.NotificationUtils;
import com.google.firebase.firestore.FirebaseFirestore;

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