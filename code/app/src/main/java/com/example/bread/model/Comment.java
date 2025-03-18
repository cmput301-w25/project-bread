package com.example.bread.model;

import androidx.annotation.NonNull;

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
    public Comment(String text) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
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

    @Override
    public int compareTo(Comment o) {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }
}
