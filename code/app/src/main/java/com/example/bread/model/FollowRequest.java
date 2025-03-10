package com.example.bread.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;

/**
 * Model class representing a follow request between two participants.
 */
@IgnoreExtraProperties
public class FollowRequest implements Serializable {
    private String fromUsername;
    private String status; // "pending", "accepted", "declined"
    private Timestamp timestamp;

    public FollowRequest() {
        // Required empty constructor for Firestore
    }

    public FollowRequest(String fromUsername) {
        this.fromUsername = fromUsername;
        this.status = "pending";
        this.timestamp = Timestamp.now();
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Converts this object to a Firestore data map
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("fromUsername", fromUsername);
        map.put("status", status);
        map.put("timestamp", timestamp);
        return map;
    }
}