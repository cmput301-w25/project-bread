package com.example.bread.model;

import android.location.Location;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
        NONE,
    }

    /**
     * Enum representing the different social situations a user can be in
     */
    public enum SocialSituation {
        ALONE,
        WITH_FAMILY,
        WITH_FRIENDS,
        WITH_COWORKERS,
        WITH_STRANGERS,
        NONE,
    }

    private String id;
    private String title;
    @ServerTimestamp
    private Date timestamp;
    private String reason;
    private Map<String, Object> geoInfo;
    private DocumentReference participantRef;

    private EmotionalState emotionalState;
    private SocialSituation socialSituation;
    private String imageUrl;
    private String trigger;
    public MoodEvent() {
    }

    public MoodEvent(String title, String reason, EmotionalState emotionalState, DocumentReference participantRef) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.timestamp = null;
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
                ", title='" + title + '\'' +
                ", reason='" + reason + '\'' +
                ", trigger='" + trigger + '\''+
                ", participantRef=" + participantRef +
                ", emotionalState=" + emotionalState +
                ", socialSituation=" + socialSituation +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Map<String, Object> getGeoInfo() {
        return geoInfo;
    }

    public void setGeoInfo(Map<String, Object> geoInfo) {
        this.geoInfo = geoInfo;
    }
    public String getTrigger() {
        return trigger;
    }
    public void setTrigger(String trigger){
        this.trigger = trigger;
    }

    public Map<String, Object> generateGeoInfo(Location location) {
        String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(location.getLatitude(), location.getLongitude()));
        Map<String, Object> geoInfo = new HashMap<>();
        geoInfo.put("geohash", hash);
        geoInfo.put("latitude", location.getLatitude());
        geoInfo.put("longitude", location.getLongitude());
        return geoInfo;
    }

    @Override
    public int compareTo(MoodEvent event) {
        return this.timestamp.compareTo(event.getTimestamp());
    }
}
