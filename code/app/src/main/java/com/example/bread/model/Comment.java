package com.example.bread.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a comment, encapsulating the user's comment and the timestamp of the comment.
 * Implements {@link Serializable} and {@link Comparable} for sorting.
 */
public class Comment implements Serializable, Comparable<Comment> {

    private String id;
    private DocumentReference participantRef;
    @ServerTimestamp
    private Date timestamp;
    private String text;

    /**
     * Default constructor for Firestore serialization
     */
    public Comment() {
    }

    /**
     * Constructor for creating a new comment
     *
     * @param text the text of the comment
     */
    public Comment(@NonNull DocumentReference participantRef, String text) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.participantRef = participantRef;
    }

    /**
     * Returns the UUID of the comment
     *
     * @return the UUID of the comment
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the UUID of the comment
     *
     * @param id the UUID of the comment
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the timestamp of the comment
     *
     * @return the timestamp of the comment
     */
    public Date getTimestamp() {
        return this.timestamp;
    }

    /**
     * Sets the timestamp of the comment
     *
     * @param timestamp the timestamp of the comment
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the text of the comment
     *
     * @return the text of the comment
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text of the comment
     *
     * @param text the text of the comment
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the reference to the participant who made the comment
     *
     * @return the reference to the participant who made the comment
     */
    public DocumentReference getParticipantRef() {
        return participantRef;
    }

    /**
     * Sets the reference to the participant who made the comment
     *
     * @param participantRef reference to the participant who made the comment
     */
    public void setParticipantRef(DocumentReference participantRef) {
        this.participantRef = participantRef;
    }

    /**
     * Compares this comment to another comment based on the timestamp.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(Comment o) {
        if (this.timestamp == null && o.timestamp == null) {
            return 0;
        }
        if (this.timestamp == null) {
            return -1;
        }
        if (o.timestamp == null) {
            return 1;
        }
        return this.timestamp.compareTo(o.timestamp);
    }

    @NonNull
    @Override
    public String toString() {
        return "Comment{" +
                "id='" + id + '\'' +
                ", participantRef=" + participantRef +
                ", timestamp=" + timestamp +
                ", text='" + text + '\'' +
                '}';
    }
}
