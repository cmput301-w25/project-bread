package com.example.bread.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

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

    private String timestamp;
    private String reason;

    private EmotionalState emotionalState;

    public MoodEvent(String timestamp, String reason, EmotionalState emotionalState) {
        this.timestamp = timestamp;
        this.reason = reason;
        this.emotionalState = emotionalState;
    }

    @NonNull
    @Override
    public String toString() {
        return "MoodEvent{" +
                "timestamp='" + timestamp + '\'' +
                ", reason='" + reason + '\'' +
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

}
