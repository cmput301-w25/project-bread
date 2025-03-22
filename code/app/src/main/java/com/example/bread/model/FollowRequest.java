package com.example.bread.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;

/**
 * Model class representing a follow request between two participants.
 * Contains the username of the requestor, the status of the request, and the timestamp of when it was created.
 */
public class FollowRequest implements Serializable {
    /**
     * Represents the possible states of a follow request
     */
    public enum RequestStatus {
        PENDING("pending"),
        ACCEPTED("accepted"),
        DECLINED("declined");

        private final String value;

        RequestStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static RequestStatus fromString(String value) {
            for (RequestStatus status : RequestStatus.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return PENDING;
        }
    }

    private String fromUsername;
    private String status;
    private Timestamp timestamp;

    /**
     * Required empty constructor for Firestore
     */
    public FollowRequest() {
        // Required empty constructor for Firestore
    }

    /**
     * Create a new follow request with pending status
     *
     * @param fromUsername The username of the participant sending the request
     */
    public FollowRequest(String fromUsername) {
        this.fromUsername = fromUsername;
        this.status = RequestStatus.PENDING.getValue();
        this.timestamp = Timestamp.now();
    }

    /**
     * Get the username of the participant who sent the request
     *
     * @return The username of the requestor
     */
    public String getFromUsername() {
        return fromUsername;
    }

    /**
     * Set the username of the participant sending the request
     *
     * @param fromUsername The username to set
     */
    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    /**
     * Get the status string of the request
     *
     * @return The status as a string: "pending", "accepted", or "declined"
     */
    public String getStatus() {
        return status;
    }

    /**
     * Get the status as an enum value
     *
     * @return The RequestStatus enum value
     */
    public RequestStatus getStatusEnum() {
        return RequestStatus.fromString(status);
    }

    /**
     * Set the status of the request using a string
     *
     * @param status The status string to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Set the status of the request using an enum value
     *
     * @param status The RequestStatus enum value to set
     */
    public void setStatus(RequestStatus status) {
        this.status = status.getValue();
    }

    /**
     * Get the timestamp when the request was created
     *
     * @return The timestamp
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp of the request
     *
     * @param timestamp The timestamp to set
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Converts this object to a Firestore data map
     *
     * @return A map representation for Firestore
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("fromUsername", fromUsername);
        map.put("status", status);
        map.put("timestamp", timestamp);
        return map;
    }
}