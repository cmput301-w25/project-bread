package com.example.bread.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a participant (user) in the application.
 * Contains user information, profile data, and relationship metadata.
 */
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
     * Default constructor initializing empty lists and counters
     */
    public Participant() {
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
        this.followRequests = new ArrayList<>();
        this.followerCount = 0;
        this.followingCount = 0;
    }

    /**
     * Create a new participant with basic user information
     *
     * @param username  The unique username for the participant
     * @param email     The email address of the participant
     * @param firstName The first name of the participant
     * @param lastName  The last name of the participant
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
     * Returns a string representation of the participant
     *
     * @return String representation with participant details
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
     * Get the username of the participant
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the username of the participant
     *
     * @param username The username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get the email address of the participant
     *
     * @return The email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the email address of the participant
     *
     * @param email The email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Get the first name of the participant
     *
     * @return The first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Set the first name of the participant
     *
     * @param firstName The first name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Get the last name of the participant
     *
     * @return The last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Set the last name of the participant
     *
     * @param lastName The last name to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Get the list of followers for this participant
     *
     * @return List of follower usernames
     */
    public List<String> getFollowers() {
        return followers;
    }

    /**
     * Set the list of followers and update the follower count
     *
     * @param followers List of follower usernames
     */
    public void setFollowers(List<String> followers) {
        this.followers = followers;
        this.followerCount = followers != null ? followers.size() : 0;
    }

    /**
     * Get the list of users this participant is following
     *
     * @return List of following usernames
     */
    public List<String> getFollowing() {
        return following;
    }

    /**
     * Set the list of following and update the following count
     *
     * @param following List of following usernames
     */
    public void setFollowing(List<String> following) {
        this.following = following;
        this.followingCount = following != null ? following.size() : 0;
    }

    /**
     * Get the profile picture as a Base64 encoded string
     *
     * @return Base64 encoded profile picture string or null if not set
     */
    public String getProfilePicture() {
        return profilePicture;
    }

    /**
     * Set the profile picture as a Base64 encoded string
     *
     * @param profilePicture Base64 encoded profile picture string
     */
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    /**
     * Get the list of follow requests for this participant
     *
     * @return List of FollowRequest objects
     */
    public List<FollowRequest> getFollowRequests() {
        return followRequests;
    }

    /**
     * Set the list of follow requests
     *
     * @param followRequests List of FollowRequest objects
     */
    public void setFollowRequests(List<FollowRequest> followRequests) {
        this.followRequests = followRequests;
    }

    /**
     * Get a formatted display name combining first and last name
     *
     * @return Formatted display name
     */
    public String getDisplayName() {
        return capitalize(firstName) + " " + capitalize(lastName);
    }

    /**
     * Get the follower count
     *
     * @return Number of followers
     */
    public int getFollowerCount() {
        return followerCount;
    }

    /**
     * Set the follower count
     *
     * @param followerCount The count to set
     */
    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    /**
     * Get the following count
     *
     * @return Number of users being followed
     */
    public int getFollowingCount() {
        return followingCount;
    }

    /**
     * Set the following count
     *
     * @param followingCount The count to set
     */
    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    /**
     * Helper method to capitalize the first letter of a string
     *
     * @param input The input string
     * @return String with first letter capitalized
     */
    private String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}