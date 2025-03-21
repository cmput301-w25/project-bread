package com.example.bread.model;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a mood event, encapsulating the user's mood, emotional state, social situation, location
 * and other metadata. Implements {@link Serializable} and {@link Comparable} for sorting.
 */
public class MoodEvent implements Serializable, Comparable<MoodEvent> {
    /**
     * Enum representing the different emotional states a user can have
     */
    public enum EmotionalState {
        NONE,
        HAPPY,
        SAD,
        ANGRY,
        ANXIOUS,
        NEUTRAL,
        CONFUSED,
        FEARFUL,
        SHAMEFUL,
        SURPRISED;

        @NonNull
        @Override
        public String toString() {
            return capitalize(name());
        }

        private static String capitalize(String input) {
            if (input == null || input.isEmpty()) {
                return input;
            }
            return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
        }
    }

    /**
     * Enum representing the different social situations a user can be in
     */
    public enum SocialSituation {
        NONE,
        ALONE,
        WITH_ONE_OTHER_PERSON,
        WITH_TWO_TO_SEVERAL_PEOPLE,
        WITH_FAMILY,
        WITH_FRIENDS,
        WITH_COWORKERS,
        WITH_STRANGERS;

        /**
         * Converts the enum name to a more readable format
         *
         * @return a formatted string representation of the social situation
         */
        @NonNull
        @Override
        public String toString() {
            // This will format the enum's name as required
            return capitalizeFully(name().replace('_', ' '));
        }

        /**
         * Capitalizes each word in the input string.
         *
         * @param input the string to capitalize.
         * @return the capitalized string.
         */
        private static String capitalizeFully(String input) {
            if (input == null || input.isEmpty()) {
                return input;
            }
            String[] words = input.toLowerCase().split(" ");
            StringBuilder builder = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    builder.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        builder.append(word.substring(1));
                    }
                    builder.append(" ");
                }
            }
            return builder.toString().trim();
        }
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
    private String attachedImage;
    private String trigger;


    /**
     * Default constructor for MoodEvent.
     */
    public MoodEvent() {
    }

    /**
     * Constructs a MoodEvent with the specified details.
     *
     * @param title          the title of the mood event
     * @param reason         the reason behind the mood
     * @param emotionalState the emotional state associated with this event
     * @param participantRef a reference to the participant's document in the database
     */
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
                "id=" + id +
                ", timestamp=" + timestamp +
                ", title=" + title +
                ", reason=" + reason +
                ", trigger=" + trigger +
                ", participantRef=" + participantRef.getPath() +
                ", emotionalState=" + emotionalState +
                ", socialSituation=" + socialSituation +
                ", geoInfo=" + geoInfo +
                '}';
    }

    /**
     * Returns the timestamp of the mood event.
     *
     * @return the timestamp as a Date object.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of the mood event.
     *
     * @param timestamp the Date to set.
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the title of the mood event.
     *
     * @return the title as a String.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the mood event.
     *
     * @param title the title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the reason behind the mood event.
     *
     * @return the reason as a String.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the reason behind the mood event.
     *
     * @param reason the reason to set.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Returns the emotional state associated with this mood event.
     *
     * @return the emotional state as an EmotionalState enum.
     */
    public EmotionalState getEmotionalState() {
        return emotionalState;
    }

    /**
     * Sets the emotional state of this mood event.
     *
     * @param emotionalState the EmotionalState to set.
     */
    public void setEmotionalState(EmotionalState emotionalState) {
        this.emotionalState = emotionalState;
    }

    /**
     * Returns the unique identifier for this mood event.
     *
     * @return the ID as a String.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this mood event.
     *
     * @param id the ID to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the participant reference associated with this mood event.
     *
     * @return a DocumentReference object.
     */
    public DocumentReference getParticipantRef() {
        return participantRef;
    }

    /**
     * Sets the participant reference for this mood event.
     *
     * @param participantRef the DocumentReference to set.
     */
    public void setParticipantRef(DocumentReference participantRef) {
        this.participantRef = participantRef;
    }

    /**
     * Returns the social situation during the mood event.
     *
     * @return the social situation as a SocialSituation enum.
     */
    public SocialSituation getSocialSituation() {
        return socialSituation;
    }

    /**
     * Sets the social situation for this mood event.
     *
     * @param socialSituation the SocialSituation to set.
     */
    public void setSocialSituation(SocialSituation socialSituation) {
        this.socialSituation = socialSituation;
    }

    /**
     * Returns the attached image URL for this mood event.
     *
     * @return the image URL as a String.
     */
    public String getAttachedImage() {
        return attachedImage;
    }

    /**
     * Sets the attached image URL for this mood event.
     *
     * @param attachedImage the image URL to set.
     */
    public void setAttachedImage(String attachedImage) {
        this.attachedImage = attachedImage;
    }

    /**
     * Returns the geographical information for this mood event.
     *
     * @return a Map containing geohash, latitude, and longitude.
     */
    public Map<String, Object> getGeoInfo() {
        return geoInfo;
    }

    /**
     * Sets the geographical information for this mood event.
     *
     * @param geoInfo the Map containing geohash, latitude, and longitude.
     */
    public void setGeoInfo(@Nullable Map<String, Object> geoInfo) {
        this.geoInfo = geoInfo;
    }

    /**
     * Returns the trigger for this mood event.
     *
     * @return the trigger as a String.
     */
    public String getTrigger() {
        return trigger;
    }

    /**
     * Sets the trigger for this mood event.
     *
     * @param trigger the trigger to set.
     */
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }


    /**
     * Generates geographical information (geohash, latitude, longitude) for a given location.
     *
     * @param location the Android Location object.
     * @return a Map containing geohash, latitude, and longitude.
     */
    public Map<String, Object> generateGeoInfo(Location location) {
        String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(location.getLatitude(), location.getLongitude()));
        Map<String, Object> geoInfo = new HashMap<>();
        geoInfo.put("geohash", hash);
        geoInfo.put("latitude", location.getLatitude());
        geoInfo.put("longitude", location.getLongitude());
        return geoInfo;
    }

    /**
     * Compares this MoodEvent to another based on their timestamps.
     *
     * @param event the MoodEvent to compare to.
     * @return an integer representing the comparison result.
     */
    @Override
    public int compareTo(MoodEvent event) {
        if (this.timestamp == null && event.timestamp == null) {
            return 0;
        }
        if (this.timestamp == null) {
            return -1;
        }
        if (event.timestamp == null) {
            return 1;
        }
        return this.timestamp.compareTo(event.timestamp);
    }

}
