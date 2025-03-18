package com.example.bread.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.Participant;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Repository class for handling participants in the database
 */
public class ParticipantRepository {
    private final FirebaseService firebaseService;
    private final String TAG = "ParticipantRepository";

    public ParticipantRepository() {
        firebaseService = new FirebaseService();
    }

    public ParticipantRepository(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    private CollectionReference getParticipantCollRef() {
        return firebaseService.getDb().collection("participants");
    }

    /**
     * Fetches the base participant object from firebase without fetching followers and following
     * @param username The username of the participant to fetch
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
     * @param username The username of the participant to fetch
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
     * @param participantRef The reference to the participant to fetch
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
     * @param username The username of the participant
     * @return The reference to the participant
     */
    public DocumentReference getParticipantRef(@NonNull String username) {
        return getParticipantCollRef().document(username);
    }

    /**
     * Fetches the followers and following of the given participant
     * @param participant The participant to fetch followers and following for
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
     * @param username The username of the participant to fetch followers for
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
                    onSuccessListener.onSuccess(followers);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch followers for participant: " + username, e));
    }

    /**
     * Fetches the following of the given participant
     * @param username The username of the participant to fetch following for
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
                    onSuccessListener.onSuccess(following);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch following for participant: " + username, e));
    }

    /**
     * Adds a participant to the database
     * @param participant The participant to add
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
     * @param username The username of the participant to add the follower to
     * @param followerUsername The username of the follower to add
     * @param onSuccessListener The listener to be called when the follower is successfully added
     * @param onFailureListener The listener to be called when the follower cannot be added
     */
    public void addFollower(@NonNull String username, String followerUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // TODO: check if already followed
        // TODO: decide on what to store in the follower document
        Map<String, String> follower = new HashMap<>();
        follower.put("username", followerUsername);
        getParticipantCollRef().document(username).collection("followers").document(followerUsername).set(follower)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to add follower: " + followerUsername + " to participant: " + username, e));
    }

    /**
     * Adds a following to the given participant
     * @param username The username of the participant to add the following to
     * @param followingUsername The username of the following to add
     * @param onSuccessListener The listener to be called when the following is successfully added
     * @param onFailureListener The listener to be called when the following cannot be added
     */
    public void addFollowing(@NonNull String username, String followingUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // TODO: check if already following
        // TODO: decide on what to store in the following document
        Map<String, String> following = new HashMap<>();
        following.put("username", followingUsername);
        getParticipantCollRef().document(username).collection("following").document(followingUsername).set(following)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to add following: " + followingUsername + " to participant: " + username, e));
    }

    /**
     * Checks if the given username exists in the database
     * @param username The username to check
     * @param onSuccessListener The listener to be called when the username exists
     * @param onFailureListener The listener to be called when the request fails
     */
    public void checkIfUsernameExists(@NonNull String username, @NonNull OnSuccessListener<Boolean> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).get()
                .addOnSuccessListener(documentSnapshot -> onSuccessListener.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to check if username exists: " + username, e));
    }

    public void checkIfAlreadyFollowed(@NonNull String username, String followerUsername, @NonNull OnSuccessListener<Boolean> onSuccessListener, OnFailureListener onFailureListener) {
    }

    public void checkIfAlreadyFollowing(@NonNull String username, String followingUsername, @NonNull OnSuccessListener<Boolean> onSuccessListener, OnFailureListener onFailureListener) {
    }
}
