package com.example.bread.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;

import java.io.Serializable;
import java.util.UUID;

public class MoodEvent implements Serializable {
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
    private String timestamp;
    private String reason;
    private DocumentReference participantRef;

    private EmotionalState emotionalState;

    public MoodEvent() {}

    public MoodEvent(String timestamp, String reason, EmotionalState emotionalState, DocumentReference participantRef) {
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
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
}
