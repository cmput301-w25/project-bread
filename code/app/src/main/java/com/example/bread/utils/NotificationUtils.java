package com.example.bread.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.bread.R;

/**
 * NotificationUtils - Utility
 * <p>
 * Role / Purpose:
 * Provides helper methods for managing and displaying local notifications related to app events
 * such as follow requests. Handles notification channel creation and message display logic.
 * <p>
 * Design Pattern:
 * - Utility Pattern: Contains only static methods and constants; not meant to be instantiated.
 * - Singleton-like static access: Ensures notification channels are initialized only once.
 * <p>
 * Outstanding Issues / Comments:
 * - Currently supports only one notification channel (follow requests); future expansion could
 * modularize different types (e.g., comments).
 */
public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    public static final String CHANNEL_ID = "follow_requests";
    public static final String CHANNEL_NAME = "Follow Requests";
    public static final String CHANNEL_DESCRIPTION = "Notifications for new follow requests";

    public static final String EXTRA_NOTIFICATION_TYPE = "notification_type";
    public static final String EXTRA_SENDER_USERNAME = "sender_username";
    public static final String TYPE_FOLLOW_REQUEST = "follow_request";

    /**
     * Creates notification channels for Android O and above.
     * Should be called once during app launch or Activity creation.
     *
     * @param context context used to access system notification service
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
        }
    }


    /**
     * Displays a local notification to the user using the provided information.
     *
     * @param context        the context to use for system services
     * @param title          the title of the notification
     * @param message        the content/message of the notification
     * @param pendingIntent  intent to trigger when the notification is clicked
     * @param notificationId unique identifier for this notification (used to update or cancel it)
     */
    public static void showNotification(Context context, String title, String message,
                                        PendingIntent pendingIntent, int notificationId) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification) // Create this resource
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            notificationManager.notify(notificationId, notificationBuilder.build());
            Log.d(TAG, "Successfully displayed notification ID: " + notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Error displaying notification: " + e.getMessage(), e);
        }
    }


}