package com.example.bread.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a participant in the app, containing user profile information such as username,
 * email, first name, last name, and profile picture. Implements {@link Serializable} to allow
 * easy storage and retrieval from the database.
 */
@IgnoreExtraProperties
public class Participant implements Serializable {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePicture;
    private int followerCount;
    private int followingCount;

    @Exclude
    private List<String> followers;
    @Exclude
    private List<String> following;
    @Exclude
    private List<FollowRequest> followRequests;

    /**
     * Default constructor required for Firestore serialization.
     */
    public Participant() {
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
        this.followRequests = new ArrayList<>();
        this.followerCount = 0;
        this.followingCount = 0;
    }

    /**
     * Constructs a Participant with the specified details.
     *
     * @param username  the username of the participant
     * @param email     the email address of the participant
     * @param firstName the first name of the participant
     * @param lastName  the last name of the participant
     */
    public Participant(String username, String email, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
        this.lastName = lastName.substring(0, 1).toUpperCase() + lastName.substring(1).toLowerCase();
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
        this.followRequests = new ArrayList<>();
        this.followerCount = 0;
        this.followingCount = 0;
    }

    /**
     * Returns a string representation of the Participant.
     *
     * @return a string containing the participant's details.
     */
    @NonNull
    @Override
    public String toString() {
        return "Participant{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", followerCount=" + followerCount + '\'' +
                ", followingCount=" + followingCount + '\'' +
                '}';
    }

    /**
     * Gets the username of the participant.
     *
     * @return the username as a String.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the participant.
     *
     * @param username the username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the email address of the participant.
     *
     * @return the email address as a String.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the participant.
     *
     * @param email the email address to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the first name of the participant.
     *
     * @return the first name as a String.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name of the participant.
     *
     * @param firstName the first name to set.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the last name of the participant.
     *
     * @return the last name as a String.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name of the participant.
     *
     * @param lastName the last name to set.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the list of followers for the participant.
     * This field is excluded from Firestore storage.
     *
     * @return a list of usernames representing followers.
     */
    public List<String> getFollowers() {
        return followers;
    }

    /**
     * Sets the list of followers for the participant and updates the follower count.
     * This field is excluded from Firestore storage.
     *
     * @param followers a list of usernames representing followers.
     */
    public void setFollowers(List<String> followers) {
        this.followers = followers;
        this.followerCount = followers != null ? followers.size() : 0;
    }

    /**
     * Gets the list of users that the participant is following.
     * This field is excluded from Firestore storage.
     *
     * @return a list of usernames representing following users.
     */
    public List<String> getFollowing() {
        return following;
    }

    /**
     * Sets the list of users that the participant is following and updates the following count.
     * This field is excluded from Firestore storage.
     *
     * @param following a list of usernames representing following users.
     */
    public void setFollowing(List<String> following) {
        this.following = following;
        this.followingCount = following != null ? following.size() : 0;
    }

    /**
     * Gets the URL of the participant's profile picture.
     *
     * @return the profile picture URL as a String.
     */
    public String getProfilePicture() {
        return profilePicture;
    }

    /**
     * Sets the URL of the participant's profile picture.
     *
     * @param profilePicture the profile picture URL to set.
     */
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    /**
     * Gets the list of follow requests for this participant.
     * This field is excluded from Firestore storage.
     *
     * @return List of FollowRequest objects.
     */
    public List<FollowRequest> getFollowRequests() {
        return followRequests;
    }

    /**
     * Sets the list of follow requests.
     * This field is excluded from Firestore storage.
     *
     * @param followRequests List of FollowRequest objects.
     */
    public void setFollowRequests(List<FollowRequest> followRequests) {
        this.followRequests = followRequests;
    }

    /**
     * Gets a formatted display name combining first and last name.
     *
     * @return Formatted display name.
     */
    public String getDisplayName() {
        return capitalize(firstName) + " " + capitalize(lastName);
    }

    /**
     * Gets the follower count.
     *
     * @return Number of followers.
     */
    public int getFollowerCount() {
        return followerCount;
    }

    /**
     * Sets the follower count.
     *
     * @param followerCount The count to set.
     */
    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    /**
     * Gets the following count.
     *
     * @return Number of users being followed.
     */
    public int getFollowingCount() {
        return followingCount;
    }

    /**
     * Sets the following count.
     *
     * @param followingCount The count to set.
     */
    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    /**
     * Helper method to capitalize the first letter of a string.
     *
     * @param input The input string.
     * @return String with first letter capitalized.
     */
    private String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}