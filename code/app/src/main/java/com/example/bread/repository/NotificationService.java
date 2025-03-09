package com.example.bread.repository;

import android.util.Log;

import com.example.bread.utils.NotificationUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NotificationService {
    private static final String TAG = "NotificationService";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    /**
     * Send a follow request notification by saving it to Firestore
     */
    public static void sendFollowRequestNotification(String senderUsername, String recipientUsername) {
        if (senderUsername == null || senderUsername.isEmpty() ||
                recipientUsername == null || recipientUsername.isEmpty()) {
            Log.e(TAG, "Invalid sender or recipient username");
            return;
        }

        Log.d(TAG, "Sending follow request notification from " + senderUsername + " to " + recipientUsername);

        // Create notification data
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "follow_request");
        notification.put("senderUsername", senderUsername);
        notification.put("recipientUsername", recipientUsername);
        notification.put("title", "New Follow Request");
        notification.put("message", senderUsername + " wants to follow you");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        // Save notification to Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION_NOTIFICATIONS)
                .add(notification)
                .addOnSuccessListener(documentReference ->
                        Log.d(TAG, "Follow request notification saved with ID: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error saving follow request notification: " + e.getMessage(), e));
    }
}