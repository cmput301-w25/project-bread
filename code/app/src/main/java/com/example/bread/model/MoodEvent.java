package com.example.bread.model;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.io.Serializable;
import java.util.UUID;

public class MoodEvent implements Serializable, Comparable<MoodEvent> {
    /**
     * Enum representing the different emotional states a user can have
     */
    public enum EmotionalState {
        HAPPY,
        SAD,
        ANGRY,
        ANXIOUS,
        NEUTRAL
    }

    private String id;
    private Timestamp timestamp;
    private String reason;
    private DocumentReference participantRef;

    private EmotionalState emotionalState;

    public MoodEvent() {}

    public MoodEvent(Timestamp timestamp, String reason, EmotionalState emotionalState, DocumentReference participantRef) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = timestamp;
        this.reason = reason;
        this.emotionalState = emotionalState;
        this.participantRef = participantRef;
    }


    @NonNull
    @Override
    public String toString() {
        return "MoodEvent{" +
                "id='" + id + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", reason='" + reason + '\'' +
                ", participantRef=" + participantRef +
                ", emotionalState=" + emotionalState +
                '}';
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public EmotionalState getEmotionalState() {
        return emotionalState;
    }

    public void setEmotionalState(EmotionalState emotionalState) {
        this.emotionalState = emotionalState;
    }

    public String getId() {
        return id;
    }

    public DocumentReference getParticipantRef() {
        return participantRef;
    }

    @Override
    public int compareTo(MoodEvent event) {
        return this.timestamp.compareTo(event.getTimestamp());
    }
}
