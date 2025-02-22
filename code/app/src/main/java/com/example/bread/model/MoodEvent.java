package com.example.bread.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
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
        NEUTRAL,
        CONFUSED,
        FEARFUL,
        SHAMEFUL,
        SURPRISED,
    }

    /**
     * Enum representing the different social situations a user can be in
     */
    public enum SocialSituation {
        ALONE,
        WITH_FAMILY,
        WITH_FRIENDS,
        WITH_COWORKERS,
        WITH_STRANGERS
    }

    private String id;
    @ServerTimestamp
    private Date timestamp;
    private String reason;
    private DocumentReference participantRef;

    private EmotionalState emotionalState;
    private SocialSituation socialSituation;
    private String imageUrl;

    public MoodEvent() {
    }

    public MoodEvent(String reason, EmotionalState emotionalState, DocumentReference participantRef, SocialSituation socialSituation, String imageUrl) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = null;
        this.reason = reason;
        this.emotionalState = emotionalState;
        this.participantRef = participantRef;
        this.socialSituation = socialSituation;
        this.imageUrl = imageUrl;
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
                ", socialSituation=" + socialSituation +
                '}';
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
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

    public void setId(String id) {
        this.id = id;
    }

    public DocumentReference getParticipantRef() {
        return participantRef;
    }

    public void setParticipantRef(DocumentReference participantRef) {
        this.participantRef = participantRef;
    }

    public SocialSituation getSocialSituation() {
        return socialSituation;
    }

    public void setSocialSituation(SocialSituation socialSituation) {
        this.socialSituation = socialSituation;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public int compareTo(MoodEvent event) {
        return this.timestamp.compareTo(event.getTimestamp());
    }
}
