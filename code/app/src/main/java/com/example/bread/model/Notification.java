package com.example.bread.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a notification sent within the application.
 * A notification can be of various types such as follow requests, and contains metadata
 * including the sender, recipient, title, message, timestamp, and read status.
 */
public class Notification {
    private String type;
    private String senderUsername;
    private String recipientUsername;
    private String title;
    private String message;
    private long timestamp;
    private boolean read;

    /**
     * Default constructor required for Firestore serialization and deserialization.
     */
    public Notification() {
    }

    /**
     * Constructs a {@code Notification} with all specified fields.
     *
     * @param type              the type of notification (e.g., "follow_request")
     * @param senderUsername    the username of the sender
     * @param recipientUsername the username of the recipient
     * @param title             the title of the notification
     * @param message           the content/message of the notification
     * @param timestamp         the creation time of the notification in milliseconds
     * @param read              whether the notification has been read
     */
    public Notification(String type, String senderUsername, String recipientUsername,
                        String title, String message, long timestamp, boolean read) {
        this.type = type;
        this.senderUsername = senderUsername;
        this.recipientUsername = recipientUsername;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    /**
     * Creates a new follow request notification.
     *
     * @param senderUsername    the username of the sender
     * @param recipientUsername the username of the recipient
     * @return a {@code Notification} object representing a follow request
     */
    public static Notification createFollowRequestNotification(String senderUsername, String recipientUsername) {
        return new Notification(
                "follow_request",
                senderUsername,
                recipientUsername,
                "New Follow Request",
                senderUsername + " wants to follow you",
                System.currentTimeMillis(),
                false
        );
    }

    /**
     * Converts this {@code Notification} to a map representation for Firestore.
     *
     * @return a map containing all notification fields
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("senderUsername", senderUsername);
        map.put("recipientUsername", recipientUsername);
        map.put("title", title);
        map.put("message", message);
        map.put("timestamp", timestamp);
        map.put("read", read);
        return map;
    }

    /**
     * Returns the type of this notification.
     *
     * @return the type string
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of this notification.
     *
     * @param type the notification type to set (e.g., "follow_request")
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the sender's username.
     *
     * @return the sender's username string
     */
    public String getSenderUsername() {
        return senderUsername;
    }

    /**
     * Sets the sender's username.
     *
     * @param senderUsername the sender's username
     */
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    /**
     * Returns the recipient's username.
     *
     * @return the recipient's username string
     */
    public String getRecipientUsername() {
        return recipientUsername;
    }

    /**
     * Sets the recipient's username.
     *
     * @param recipientUsername the recipient's username
     */
    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    /**
     * Returns the title of the notification.
     *
     * @return the title string
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the notification.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the message body of the notification.
     *
     * @return the message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message body of the notification.
     *
     * @param message the message content
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the timestamp when the notification was created.
     *
     * @return the timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of the notification.
     *
     * @param timestamp the timestamp in milliseconds
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns whether the notification has been read.
     *
     * @return {@code true} if the notification has been read, otherwise {@code false}
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Sets the read status of the notification.
     *
     * @param read {@code true} if the notification has been read, otherwise {@code false}
     */
    public void setRead(boolean read) {
        this.read = read;
    }
}
