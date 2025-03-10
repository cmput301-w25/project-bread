package com.example.bread.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class to represent notifications in the application
 */
public class Notification {
    private String type;
    private String senderUsername;
    private String recipientUsername;
    private String title;
    private String message;
    private long timestamp;
    private boolean read;

    // Empty constructor required for Firestore
    public Notification() {
    }

    // Constructor with all fields
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

    // Static factory method for follow request notification
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

    // Convert to Map for Firestore
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

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}