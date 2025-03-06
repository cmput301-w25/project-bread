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

public class ParticipantRepository {
    private final FirebaseService firebaseService;

    public ParticipantRepository() {
        firebaseService = new FirebaseService();
    }

    private CollectionReference getParticipantCollRef() {
        return firebaseService.getDb().collection("participants");
    }

    public void fetchBaseParticipant(@NonNull String username, @NonNull OnSuccessListener<Participant> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Participant participant = documentSnapshot.toObject(Participant.class);
                        onSuccessListener.onSuccess(participant);
                    } else {
                        Log.e("ParticipantRepository", "Participant with username: " + username + " does not exist");
                        onSuccessListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to fetch participant with username: " + username, e));
    }

    public void fetchParticipant(@NonNull String username, @NonNull OnSuccessListener<Participant> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Participant participant = documentSnapshot.toObject(Participant.class);
                        fetchFollowersAndFollowing(Objects.requireNonNull(participant), onSuccessListener, onFailureListener);
                    } else {
                        Log.e("ParticipantRepository", "Participant with username: " + username + " does not exist");
                        onSuccessListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to fetch participant with username: " + username, e));
    }

    public DocumentReference getParticipantRef(@NonNull String username) {
        return getParticipantCollRef().document(username);
    }

    public void fetchFollowersAndFollowing(@NonNull Participant participant, @NonNull OnSuccessListener<Participant> onSuccessListener, OnFailureListener onFailureListener) {
        fetchFollowing(participant.getUsername(), following -> {
            participant.setFollowing(following);
            fetchFollowers(participant.getUsername(), followers -> {
                participant.setFollowers(followers);
                onSuccessListener.onSuccess(participant);
            }, onFailureListener);
        }, onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to fetch following for participant: " + participant.getUsername(), e));
    }

    public void fetchFollowers(@NonNull String username, @NonNull OnSuccessListener<List<String>> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).collection("followers").get()
                .addOnSuccessListener(followersSnapshot -> {
                    List<String> followers = new ArrayList<>();
                    for (DocumentSnapshot doc : followersSnapshot) {
                        followers.add(doc.getString("username"));
                    }
                    onSuccessListener.onSuccess(followers);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to fetch followers for participant: " + username, e));
    }

    public void fetchFollowing(@NonNull String username, @NonNull OnSuccessListener<List<String>> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).collection("following").get()
                .addOnSuccessListener(followingSnapshot -> {
                    List<String> following = new ArrayList<>();
                    for (DocumentSnapshot doc : followingSnapshot) {
                        following.add(doc.getString("username"));
                    }
                    onSuccessListener.onSuccess(following);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to fetch following for participant: " + username, e));
    }

    public void addParticipant(@NonNull Participant participant, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(participant.getUsername()).set(participant)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to add participant: " + participant, e));
    }

    public void addFollower(@NonNull String username, String followerUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // TODO: check if already followed
        // TODO: decide on what to store in the follower document
        Map<String, String> follower = new HashMap<>();
        follower.put("username", followerUsername);
        getParticipantCollRef().document(username).collection("followers").document(followerUsername).set(follower)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to add follower: " + followerUsername + " to participant: " + username, e));
    }

    public void addFollowing(@NonNull String username, String followingUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // TODO: check if already following
        // TODO: decide on what to store in the following document
        Map<String, String> following = new HashMap<>();
        following.put("username", followingUsername);
        getParticipantCollRef().document(username).collection("following").document(followingUsername).set(following)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to add following: " + followingUsername + " to participant: " + username, e));
    }

    public void checkIfUsernameExists(@NonNull String username, @NonNull OnSuccessListener<Boolean> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(username).get()
                .addOnSuccessListener(documentSnapshot -> onSuccessListener.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to check if username exists: " + username, e));
    }

    public void checkIfAlreadyFollowed(@NonNull String username, String followerUsername, @NonNull OnSuccessListener<Boolean> onSuccessListener, OnFailureListener onFailureListener) {
    }

    public void checkIfAlreadyFollowing(@NonNull String username, String followingUsername, @NonNull OnSuccessListener<Boolean> onSuccessListener, OnFailureListener onFailureListener) {
    }
}
