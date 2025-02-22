package com.example.bread.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.Participant;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ParticipantRepository {
    private final FirebaseService firebaseService;

    public ParticipantRepository() {
        firebaseService = new FirebaseService();
    }

    private CollectionReference getParticipantCollRef() {
        return firebaseService.getDb().collection("participants");
    }

    public void fetchParticipant(String username, @NonNull OnSuccessListener<Participant> onSuccessListener, OnFailureListener onFailureListener) {
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

    private void fetchFollowersAndFollowing(Participant participant, @NonNull OnSuccessListener<Participant> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(participant.getUsername()).collection("followers").get()
                .addOnSuccessListener(followersSnapshot -> {
                    List<String> followers = new ArrayList<>();
                    for (DocumentSnapshot doc: followersSnapshot) {
                        followers.add(doc.getString("username"));
                    }
                    participant.setFollowers(followers);

                    getParticipantCollRef().document(participant.getUsername()).collection("following").get()
                            .addOnSuccessListener(followingSnapshot -> {
                                List<String> following = new ArrayList<>();
                                for (DocumentSnapshot doc: followersSnapshot) {
                                    following.add(doc.getString("username"));
                                }
                                participant.setFollowing(following);
                                onSuccessListener.onSuccess(participant);
                            })
                            .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to fetch following for participant: " + participant.getUsername(), e));
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to fetch followers for participant: " + participant.getUsername(), e));
    }

    public void addParticipant(Participant participant, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(participant.getUsername()).set(participant)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to add participant: " + participant, e));
    }
}
