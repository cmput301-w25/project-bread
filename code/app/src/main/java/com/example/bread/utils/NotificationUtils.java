package com.example.bread.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.bread.R;
import com.example.bread.view.HomePage;
import com.google.firebase.messaging.FirebaseMessaging;

public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    public static final String CHANNEL_ID = "follow_requests";
    public static final String CHANNEL_NAME = "Follow Requests";
    public static final String CHANNEL_DESCRIPTION = "Notifications for new follow requests";

    public static final String EXTRA_NOTIFICATION_TYPE = "notification_type";
    public static final String EXTRA_SENDER_USERNAME = "sender_username";
    public static final String TYPE_FOLLOW_REQUEST = "follow_request";


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
     * Shows a notification
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