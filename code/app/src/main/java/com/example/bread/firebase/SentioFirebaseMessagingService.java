package com.example.bread.firebase;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bread.utils.NotificationUtils;
import com.example.bread.view.HomePage;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Role / Purpose:
 * - Represents a service class for handling Firebase Cloud Messaging (FCM) messages.
 * - Receives and processes FCM messages.
 * - Handles token management and updates.
 * - Manages different types of data payloads from FCM.
 * <p>
 * - Design Patterns:
 * - Observer: The `FirebaseMessagingService` is an observer of FCM messages.
 * <p>
 * Outstanding Issues:
 * - Error handling for token saving and message processing could be made more comprehensive.
 */
public class SentioFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseMsgService";

    /**
     * Called when a message is received.
     *
     * @param remoteMessage The remote message.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();

            // Create a default intent (to HomePage)
            Intent intent = new Intent(this, HomePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationUtils.showNotification(this, title, message, pendingIntent, 0);
        }
    }

    /**
     * Handles data messages received from FCM.
     *
     * @param data The data payload of the message.
     */
    private void handleDataMessage(Map<String, String> data) {
        String notificationType = data.get("type");
        String title = data.get("title");
        String message = data.get("message");
        String senderUsername = data.get("senderUsername");

        if (notificationType == null) {
            return;
        }

        // Create an intent based on notification type
        Intent intent = new Intent(this, HomePage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add data to the intent
        intent.putExtra(NotificationUtils.EXTRA_NOTIFICATION_TYPE, notificationType);

        if (NotificationUtils.TYPE_FOLLOW_REQUEST.equals(notificationType)) {
            // For follow requests, navigate to follow requests fragment
            intent.putExtra(NotificationUtils.EXTRA_SENDER_USERNAME, senderUsername);
            intent.putExtra("navigate_to", "follow_requests");
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationUtils.showNotification(
                this,
                title != null ? title : "New Notification",
                message != null ? message : "",
                pendingIntent,
                0);
    }

    /**
     * Called when a new token is generated
     * We need to send this token to our server
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Send the token to your server
        saveFCMTokenToFirestore(token);
    }

    /**
     * Saves the FCM token to Firestore.
     *
     * @param token The FCM token to save.
     */
    private void saveFCMTokenToFirestore(String token) {
        // Get current user
        String username = getCurrentUsername();
        if (username != null && !username.isEmpty()) {
            FCMTokenManager.saveTokenToFirestore(username, token);
        }
    }

    /**
     * Retrieves the current username from SharedPreferences.
     *
     * @return The current username.
     */
    private String getCurrentUsername() {
        // Get username from SharedPreferences
        return getSharedPreferences("sharedPrefs", MODE_PRIVATE)
                .getString("username", "");
    }
}
