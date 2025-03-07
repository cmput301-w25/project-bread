package com.example.bread.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.Participant;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ParticipantRepository {
    private final FirebaseService firebaseService;
    private final FirebaseFirestore db;

    public ParticipantRepository() {
        firebaseService = new FirebaseService();
        db = firebaseService.getDb();
    }

    private CollectionReference getParticipantCollRef() {
        return firebaseService.getDb().collection("participants");
    }

    /**
     * Fetches a participant by username.
     * @param username The username of the participant.
     * @param onSuccessListener Callback on success.
     * @param onFailureListener Callback on failure.
     */
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

    private void fetchFollowersAndFollowing(@NonNull Participant participant, @NonNull OnSuccessListener<Participant> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(participant.getUsername()).collection("followers").get()
                .addOnSuccessListener(followersSnapshot -> {
                    List<String> followers = new ArrayList<>();
                    for (DocumentSnapshot doc : followersSnapshot) {
                        followers.add(doc.getString("username"));
                    }
                    participant.setFollowers(followers);

                    getParticipantCollRef().document(participant.getUsername()).collection("following").get()
                            .addOnSuccessListener(followingSnapshot -> {
                                List<String> following = new ArrayList<>();
                                for (DocumentSnapshot doc : followersSnapshot) {
                                    following.add(doc.getString("username"));
                                }
                                participant.setFollowing(following);
                                onSuccessListener.onSuccess(participant);
                            })
                            .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to fetch following for participant: " + participant.getUsername(), e));
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to fetch followers for participant: " + participant.getUsername(), e));
    }

    public void addParticipant(@NonNull Participant participant, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getParticipantCollRef().document(participant.getUsername()).set(participant)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("ParticipantRepository", "Failed to add participant: " + participant, e));
    }

    public void createFollowRequest(String senderId, String receiverId, OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        if (senderId.equals(receiverId)) {
            onFailureListener.onFailure(new Exception("Cannot follow oneself."));
            return;
        }

        // Check if the follow request already exists
        db.collection("followRequests")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("receiverId", receiverId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        onFailureListener.onFailure(new Exception("Follow request already sent."));
                    } else {
                        // Create the follow request if not existing
                        DocumentReference followRequestRef = db.collection("followRequests").document();
                        Map<String, Object> followRequestData = new HashMap<>();
                        followRequestData.put("senderId", senderId);
                        followRequestData.put("receiverId", receiverId);
                        followRequestRef.set(followRequestData)
                                .addOnSuccessListener(onSuccessListener)
                                .addOnFailureListener(onFailureListener);
                    }
                });
    }


    public void addFollower(@NonNull String username, @NonNull String followerUsername, @NonNull OnSuccessListener<Void> onSuccess, @NonNull OnFailureListener onFailure) {
        DocumentReference userRef = getParticipantCollRef().document(username);
        userRef.collection("followers").document(followerUsername).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        onFailure.onFailure(new Exception("Follower already added."));
                    } else {
                        userRef.update("followers", FieldValue.arrayUnion(followerUsername))
                                .addOnSuccessListener(onSuccess)
                                .addOnFailureListener(onFailure);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    public void addFollowing(@NonNull String username, String followingUsername, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // TODO: check if already following
        // TODO: decide on what to store in the following document
        getParticipantCollRef().document(username).collection("following").document(followingUsername).set(new Object())
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

    public void acceptFollowRequest(String requestId, String senderId, String receiverId, OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        WriteBatch batch = db.batch();

        DocumentReference senderRef = db.collection("participants").document(senderId);
        DocumentReference receiverRef = db.collection("participants").document(receiverId);
        DocumentReference requestRef = db.collection("followRequests").document(requestId);

        // Delete the follow request
        batch.delete(requestRef);

        // Conditionally add to following and followers list if not already added
        senderRef.get().addOnSuccessListener(senderSnapshot -> {
            List<String> following = (List<String>) senderSnapshot.get("following");
            if (following == null || !following.contains(receiverId)) {
                batch.update(senderRef, "following", FieldValue.arrayUnion(receiverId));
            }

            receiverRef.get().addOnSuccessListener(receiverSnapshot -> {
                List<String> followers = (List<String>) receiverSnapshot.get("followers");
                if (followers == null || !followers.contains(senderId)) {
                    batch.update(receiverRef, "followers", FieldValue.arrayUnion(senderId));

                    // Commit the batch
                    batch.commit().addOnSuccessListener(onSuccessListener).addOnFailureListener(onFailureListener);
                } else {
                    onFailureListener.onFailure(new Exception("Already following."));
                }
            }).addOnFailureListener(onFailureListener);
        }).addOnFailureListener(onFailureListener);
    }

    public void declineFollowRequest(String requestId, OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        DocumentReference requestRef = db.collection("followRequests").document(requestId);
        requestRef.delete().addOnSuccessListener(onSuccessListener).addOnFailureListener(onFailureListener);
    }

    public void updateFollowCounts(String userId, boolean isIncrement, boolean isFollowers) {
        DocumentReference userRef = db.collection("participants").document(userId);
        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userRef);
                    long newCount = snapshot.getLong(isFollowers ? "followersCount" : "followingCount") + (isIncrement ? 1 : -1);
                    transaction.update(userRef, isFollowers ? "followersCount" : "followingCount", newCount);
                    return null;
                }).addOnSuccessListener(aVoid -> Log.d("ParticipantRepository", "Count updated successfully."))
                .addOnFailureListener(e -> Log.e("ParticipantRepository", "Error updating count: " + e.getMessage()));
    }

    public void unfollowUser(String currentUserId, String unfollowUserId, OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        DocumentReference currentUserRef = db.collection("participants").document(currentUserId);
        DocumentReference unfollowUserRef = db.collection("participants").document(unfollowUserId);

        WriteBatch batch = db.batch();
        batch.update(currentUserRef, "following", FieldValue.arrayRemove(unfollowUserId));
        batch.update(unfollowUserRef, "followers", FieldValue.arrayRemove(currentUserId));

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("ParticipantRepository", "Successfully unfollowed the user.");
                    onSuccessListener.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e("ParticipantRepository", "Failed to unfollow the user: " + e.getMessage());
                    onFailureListener.onFailure(e);
                });
    }



}
