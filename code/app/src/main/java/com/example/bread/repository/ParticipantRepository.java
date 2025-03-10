package com.example.bread.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.FollowRequest;
import com.example.bread.model.Participant;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository class for handling participants in the database
 */
public class ParticipantRepository {
    private final FirebaseService firebaseService;
    private static final String TAG = "ParticipantRepository";

    // List types enum
    public enum ListType {
        FOLLOWERS,
        FOLLOWING
    }

    public ParticipantRepository() {
        firebaseService = new FirebaseService();
    }

    public CollectionReference getParticipantCollRef() {
        return firebaseService.getDb().collection("participants");
    }

    /**
     * Fetches the base participant object from firebase without fetching followers and following
     *
     * @param username          The username of the participant to fetch
     * @param onSuccessListener The listener to be called when the participant is successfully fetched
     * @param onFailureListener The listener to be called when the participant cannot be fetched
     */
    public void fetchBaseParticipant(@NonNull String username, @NonNull OnSuccessListener<Participant> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Participant participant = documentSnapshot.toObject(Participant.class);
                        onSuccessListener.onSuccess(participant);
                    } else {
                        Log.e(TAG, "Participant with username: " + username + " does not exist");
                        onSuccessListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch participant with username: " + username, e));
    }

    /**
     * Fetches the participant object from firebase with followers and following
     *
     * @param username          The username of the participant to fetch
     * @param onSuccessListener The listener to be called when the participant is successfully fetched
     * @param onFailureListener The listener to be called when the participant cannot be fetched
     */
    public void fetchParticipant(@NonNull String username, @NonNull OnSuccessListener<Participant> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Participant participant = documentSnapshot.toObject(Participant.class);
                        fetchFollowersAndFollowing(Objects.requireNonNull(participant), onSuccessListener, onFailureListener);
                    } else {
                        Log.e(TAG, "Participant with username: " + username + " does not exist");
                        onSuccessListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch participant with username: " + username, e));
    }

    /**
     * Fetches the base participant object from firebase with the given reference
     *
     * @param participantRef    The reference to the participant to fetch
     * @param onSuccessListener The listener to be called when the participant is successfully fetched
     * @param onFailureListener The listener to be called when the participant cannot be fetched
     */
    public void fetchParticipantByRef(@NonNull DocumentReference participantRef, @NonNull OnSuccessListener<Participant> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        participantRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Participant participant = documentSnapshot.toObject(Participant.class);
                        onSuccessListener.onSuccess(participant);
                    } else {
                        Log.e(TAG, "Participant with reference: " + participantRef + " does not exist");
                        onSuccessListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailureListener);
    }

    /**
     * Constructs a reference to the participant with the given username
     *
     * @param username The username of the participant
     * @return The reference to the participant
     */
    public DocumentReference getParticipantRef(@NonNull String username) {
        return getParticipantCollRef().document(username);
    }

    /**
     * Fetches the followers and following of the given participant
     *
     * @param participant       The participant to fetch followers and following for
     * @param onSuccessListener The listener to be called when the followers and following are successfully fetched
     * @param onFailureListener The listener to be called when the followers and following cannot be fetched
     */
    public void fetchFollowersAndFollowing(@NonNull Participant participant, @NonNull OnSuccessListener<Participant> onSuccessListener, OnFailureListener onFailureListener) {
        fetchFollowing(participant.getUsername(), following -> {
            participant.setFollowing(following);
            fetchFollowers(participant.getUsername(), followers -> {
                participant.setFollowers(followers);
                onSuccessListener.onSuccess(participant);
            }, onFailureListener);
        }, onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch following for participant: " + participant.getUsername(), e));
    }

    /**
     * Fetches the followers of the given participant
     *
     * @param username          The username of the participant to fetch followers for
     * @param onSuccessListener The listener to be called when the followers are successfully fetched
     * @param onFailureListener The listener to be called when the followers cannot be fetched
     */
    public void fetchFollowers(@NonNull String username, @NonNull OnSuccessListener<List<String>> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).collection("followers").get()
                .addOnSuccessListener(followersSnapshot -> {
                    List<String> followers = new ArrayList<>();
                    for (DocumentSnapshot doc : followersSnapshot) {
                        followers.add(doc.getString("username"));
                    }
                    // Update follower count in participant document
                    updateFollowerCount(username, followers.size());
                    onSuccessListener.onSuccess(followers);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch followers for participant: " + username, e));
    }

    /**
     * Fetches the following of the given participant
     *
     * @param username          The username of the participant to fetch following for
     * @param onSuccessListener The listener to be called when the following are successfully fetched
     * @param onFailureListener The listener to be called when the following cannot be fetched
     */
    public void fetchFollowing(@NonNull String username, @NonNull OnSuccessListener<List<String>> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).collection("following").get()
                .addOnSuccessListener(followingSnapshot -> {
                    List<String> following = new ArrayList<>();
                    for (DocumentSnapshot doc : followingSnapshot) {
                        following.add(doc.getString("username"));
                    }
                    // Update following count in participant document
                    updateFollowingCount(username, following.size());
                    onSuccessListener.onSuccess(following);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch following for participant: " + username, e));
    }

    /**
     * Update follower count in the participant document
     */
    private void updateFollowerCount(String username, int count) {
        getParticipantCollRef().document(username).update("followerCount", count)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update follower count for " + username, e));
    }

    /**
     * Update following count in the participant document
     */
    private void updateFollowingCount(String username, int count) {
        getParticipantCollRef().document(username).update("followingCount", count)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update following count for " + username, e));
    }

    /**
     * Adds a participant to the database
     *
     * @param participant       The participant to add
     * @param onSuccessListener The listener to be called when the participant is successfully added
     * @param onFailureListener The listener to be called when the participant cannot be added
     */
    public void addParticipant(@NonNull Participant participant, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(participant.getUsername()).set(participant)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to add participant: " + participant, e));
    }

    /**
     * Adds a follower to the given participant
     *
     * @param username          The username of the participant to add the follower to
     * @param followerUsername  The username of the follower to add
     * @param onSuccessListener The listener to be called when the follower is successfully added
     * @param onFailureListener The listener to be called when the follower cannot be added
     */
    public void addFollower(@NonNull String username, String followerUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        Map<String, String> follower = new HashMap<>();
        follower.put("username", followerUsername);
        getParticipantCollRef().document(username).collection("followers").document(followerUsername).set(follower)
                .addOnSuccessListener(aVoid -> {
                    // Update the follower count after adding
                    fetchFollowers(username, followers -> {
                        onSuccessListener.onSuccess(aVoid);
                    }, e -> onSuccessListener.onSuccess(aVoid));
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to add follower: " + followerUsername + " to participant: " + username, e));
    }

    /**
     * Adds a following to the given participant
     *
     * @param username          The username of the participant to add the following to
     * @param followingUsername The username of the following to add
     * @param onSuccessListener The listener to be called when the following is successfully added
     * @param onFailureListener The listener to be called when the following cannot be added
     */
    public void addFollowing(@NonNull String username, String followingUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        Map<String, String> following = new HashMap<>();
        following.put("username", followingUsername);
        getParticipantCollRef().document(username).collection("following").document(followingUsername).set(following)
                .addOnSuccessListener(aVoid -> {
                    // Update the following count after adding
                    fetchFollowing(username, followingList -> {
                        onSuccessListener.onSuccess(aVoid);
                    }, e -> onSuccessListener.onSuccess(aVoid));
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to add following: " + followingUsername + " to participant: " + username, e));
    }

    /**
     * Sends a follow request to a participant
     *
     * @param fromUsername      The username of the participant sending the request
     * @param toUsername        The username of the participant to receive the request
     * @param onSuccessListener The listener to be called when the request is successfully sent
     * @param onFailureListener The listener to be called when the request cannot be sent
     */
    public void sendFollowRequest(@NonNull String fromUsername, @NonNull String toUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        FollowRequest request = new FollowRequest(fromUsername);
        getParticipantCollRef().document(toUsername).collection("followRequests").document(fromUsername).set(request)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to send follow request from: " + fromUsername + " to: " + toUsername, e));
    }

    /**
     * Fetch all follow requests for a participant
     *
     * @param username          The username of the participant to fetch requests for
     * @param onSuccessListener The listener to be called when requests are successfully fetched
     * @param onFailureListener The listener to be called when requests cannot be fetched
     */
    public void fetchFollowRequests(@NonNull String username, @NonNull OnSuccessListener<List<FollowRequest>> onSuccessListener, OnFailureListener onFailureListener) {
        // Query for pending follow requests
        getParticipantCollRef().document(username).collection("followRequests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FollowRequest> requests = querySnapshot.toObjects(FollowRequest.class);
                    onSuccessListener.onSuccess(requests);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to fetch follow requests for: " + username, e));
    }

    /**
     * Accept a follow request
     *
     * @param username          The username of the participant accepting the request
     * @param requestorUsername The username of the participant who sent the request
     * @param onSuccessListener The listener to be called when the request is successfully accepted
     * @param onFailureListener The listener to be called when the request cannot be accepted
     */
    public void acceptFollowRequest(@NonNull String username, @NonNull String requestorUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // Update request status to accepted
        getParticipantCollRef().document(username).collection("followRequests").document(requestorUsername)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    // Add follower relationship
                    addFollower(username, requestorUsername, unused -> {
                        // Add following relationship
                        addFollowing(requestorUsername, username, onSuccessListener, onFailureListener);
                    }, onFailureListener);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to accept follow request from: " + requestorUsername + " for: " + username, e));
    }

    /**
     * Decline a follow request
     *
     * @param username          The username of the participant declining the request
     * @param requestorUsername The username of the participant who sent the request
     * @param onSuccessListener The listener to be called when the request is successfully declined
     * @param onFailureListener The listener to be called when the request cannot be declined
     */
    public void declineFollowRequest(@NonNull String username, @NonNull String requestorUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // Delete the follow request document
        deleteFollowRequest(username, requestorUsername, onSuccessListener, onFailureListener);
    }

    /**
     * Delete a follow request document
     */
    private void deleteFollowRequest(@NonNull String username, @NonNull String requestorUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).collection("followRequests").document(requestorUsername).delete()
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to delete follow request from: " + requestorUsername + " for: " + username, e));
    }

    /**
     * Search for participants by username prefix
     * <p>
     * This method uses a Firestore range query with a special Unicode character that sorts
     * after all other characters to find all usernames that start with the given prefix.
     *
     * @param usernamePrefix    The prefix to search for
     * @param onSuccessListener The listener to be called when participants are successfully found
     * @param onFailureListener The listener to be called when the search fails
     */
    public void searchUsersByUsername(String usernamePrefix, @NonNull OnSuccessListener<List<Participant>> onSuccessListener, OnFailureListener onFailureListener) {
        String endPrefix = usernamePrefix + "\uf8ff"; // Unicode character that sorts after all other characters

        getParticipantCollRef()
                .whereGreaterThanOrEqualTo("username", usernamePrefix)
                .whereLessThan("username", endPrefix)
                .limit(20) // Limit results to avoid too many results
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        onSuccessListener.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<Participant> participants = queryDocumentSnapshots.toObjects(Participant.class);
                    onSuccessListener.onSuccess(participants);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to search users with prefix: " + usernamePrefix, e));
    }

    /**
     * Checks if the given username exists in the database
     *
     * @param username          The username to check
     * @param onSuccessListener The listener to be called when the username exists
     * @param onFailureListener The listener to be called when the request fails
     */
    public void checkIfUsernameExists(@NonNull String username, @NonNull OnSuccessListener<Boolean> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).get()
                .addOnSuccessListener(documentSnapshot -> onSuccessListener.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to check if username exists: " + username, e));
    }

    /**
     * Check if a user has already sent a follow request to another user
     *
     * @param fromUsername      The username of the sender
     * @param toUsername        The username of the receiver
     * @param onSuccessListener The listener to be called with the result
     * @param onFailureListener The listener to be called when the check fails
     */
    public void checkFollowRequestExists(@NonNull String fromUsername, @NonNull String toUsername, @NonNull OnSuccessListener<Boolean> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(toUsername).collection("followRequests").document(fromUsername).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Check if the document exists AND its status is "pending"
                    boolean exists = documentSnapshot.exists();
                    if (exists) {
                        String status = documentSnapshot.getString("status");
                        exists = "pending".equals(status); // Only consider it existing if status is pending
                    }
                    onSuccessListener.onSuccess(exists);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to check follow request from: " + fromUsername + " to: " + toUsername, e));
    }

    /**
     * Check if the user is already following another user
     *
     * @param username          The username checking if they're following
     * @param targetUsername    The username being followed
     * @param onSuccessListener The listener with the result
     * @param onFailureListener The listener if the check fails
     */
    public void isFollowing(@NonNull String username, @NonNull String targetUsername, @NonNull OnSuccessListener<Boolean> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).collection("following").document(targetUsername).get()
                .addOnSuccessListener(documentSnapshot -> onSuccessListener.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to check if " + username + " is following " + targetUsername, e));
    }

    /**
     * Remove a follower from the participant's followers
     *
     * @param username          The username of the participant removing the follower
     * @param followerUsername  The username of the follower to remove
     * @param onSuccessListener The listener to be called when the follower is successfully removed
     * @param onFailureListener The listener to be called when the follower cannot be removed
     */
    public void removeFollower(@NonNull String username, @NonNull String followerUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // Remove follower from user's followers collection
        getParticipantCollRef().document(username).collection("followers").document(followerUsername).delete()
                .addOnSuccessListener(unused -> {
                    // Also remove user from follower's following collection
                    getParticipantCollRef().document(followerUsername).collection("following").document(username).delete()
                            .addOnSuccessListener(aVoid -> {
                                // Also delete any previous follow request documents to allow new requests
                                deleteFollowRequest(username, followerUsername, onSuccessListener, e -> {
                                    // Still consider success even if request delete fails
                                    Log.w(TAG, "Failed to delete follow request after removing follower", e);
                                    onSuccessListener.onSuccess(null);
                                });
                            })
                            .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                                    Log.e(TAG, "Failed to remove " + username + " from " + followerUsername + "'s following", e));
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to remove " + followerUsername + " from " + username + "'s followers", e));
    }

    /**
     * Unfollow a user
     *
     * @param username          The username of the participant unfollowing
     * @param targetUsername    The username of the participant to unfollow
     * @param onSuccessListener The listener to be called when successfully unfollowed
     * @param onFailureListener The listener to be called when unfollowing fails
     */
    public void unfollowUser(@NonNull String username, @NonNull String targetUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // Remove target from user's following collection
        getParticipantCollRef().document(username).collection("following").document(targetUsername).delete()
                .addOnSuccessListener(unused -> {
                    // Also remove user from target's followers collection
                    getParticipantCollRef().document(targetUsername).collection("followers").document(username).delete()
                            .addOnSuccessListener(aVoid -> {
                                // Also delete any previous follow request documents to allow new requests
                                deleteFollowRequest(targetUsername, username, onSuccessListener, e -> {
                                    // Still consider success even if request delete fails
                                    Log.w(TAG, "Failed to delete follow request after unfollowing", e);
                                    onSuccessListener.onSuccess(null);
                                });
                            })
                            .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                                    Log.e(TAG, "Failed to remove " + username + " from " + targetUsername + "'s followers", e));
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to remove " + targetUsername + " from " + username + "'s following", e));
    }

    /**
     * Set up a real-time listener for participant data updates
     *
     * @param username                    The username of the participant to listen for
     * @param onParticipantUpdateListener The listener to be called when the participant data updates
     * @return A ListenerRegistration that can be used to remove the listener when not needed
     */
    public ListenerRegistration listenForParticipantUpdates(@NonNull String username, @NonNull OnSuccessListener<Participant> onParticipantUpdateListener) {
        return getParticipantCollRef().document(username)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for participant updates", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Participant participant = documentSnapshot.toObject(Participant.class);
                        if (participant != null) {
                            onParticipantUpdateListener.onSuccess(participant);
                        }
                    }
                });
    }
}
