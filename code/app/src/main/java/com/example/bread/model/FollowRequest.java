package com.example.bread.model;

/**
 * Represents a follow request within the application. This class stores details about the follow request,
 * including the identifiers and usernames of the sender and the receiver of the request.
 */
public class FollowRequest {
    /**
     * The Firestore document ID of the follow request.
     */
    private String id;

    /**
     * The unique identifier for the sender of the follow request.
     */
    private String senderId;

    /**
     * The username of the sender of the follow request.
     */
    private String senderUsername;

    /**
     * The unique identifier for the receiver of the follow request.
     */
    private String receiverId;

    /**
     * Default constructor for creating an instance of FollowRequest without setting any initial properties.
     */
    public FollowRequest() {
    }

    /**
     * Constructs a new FollowRequest with the specified sender and receiver details.
     *
     * @param senderId The unique identifier of the sender of the follow request.
     * @param senderUsername The username of the sender of the follow request.
     * @param receiverId The unique identifier of the receiver of the follow request.
     */
    public FollowRequest(String senderId, String senderUsername, String receiverId) {
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.receiverId = receiverId;
    }

    /**
     * Retrieves the Firestore document ID of the follow request.
     *
     * @return The document ID of the follow request.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the Firestore document ID for the follow request.
     *
     * @param id The document ID to be set for the follow request.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Retrieves the unique identifier of the sender of the follow request.
     *
     * @return The unique identifier of the sender.
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Sets the unique identifier for the sender of the follow request.
     *
     * @param senderId The unique identifier to be set for the sender.
     */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * Retrieves the username of the sender of the follow request.
     *
     * @return The username of the sender.
     */
    public String getSenderUsername() {
        return senderUsername;
    }

    /**
     * Sets the username for the sender of the follow request.
     *
     * @param senderUsername The username to be set for the sender.
     */
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    /**
     * Retrieves the unique identifier of the receiver of the follow request.
     *
     * @return The unique identifier of the receiver.
     */
    public String getReceiverId() {
        return receiverId;
    }

    /**
     * Sets the unique identifier for the receiver of the follow request.
     *
     * @param receiverId The unique identifier to be set for the receiver.
     */
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
}