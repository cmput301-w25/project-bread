package com.example.bread.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.MoodEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository class for handling mood events in the database
 */
public class MoodEventRepository {
    private final FirebaseService firebaseService;
    private static final String TAG = "MoodEventRepository";
    private static final int MAX_EVENTS_PER_USER = 20; // Limit number of events fetched per user

    public MoodEventRepository() {
        firebaseService = new FirebaseService();
    }

    private CollectionReference getMoodEventCollRef() {
        return firebaseService.getDb().collection("moodEvents");
    }

    /**
     * Fetches all mood events from the database with the given participant reference
     * @param participantRef The reference to the participant whose mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     */
    public void fetchEventsWithParticipantRef(@NonNull DocumentReference participantRef, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef()
                .whereEqualTo("participantRef", participantRef)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_EVENTS_PER_USER) // Limit query to improve performance
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("MoodEventRepository", "No mood events found with participantRef: " + participantRef);
                        onSuccessListener.onSuccess(new ArrayList<>()); // Return empty list instead of null
                        return;
                    }
                    List<MoodEvent> moodEvents = queryDocumentSnapshots.toObjects(MoodEvent.class);
                    onSuccessListener.onSuccess(moodEvents);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch mood events with participantRef: " + participantRef, e));
    }

    /**
     * Listens for all mood events from the database with the given participant reference
     * @param participantRef The reference to the participant whose mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     */
    public void listenForEventsWithParticipantRef(@NonNull DocumentReference participantRef, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        getMoodEventCollRef()
                .whereEqualTo("participantRef", participantRef)
                .orderBy("timestamp", Query.Direction.DESCENDING) // Order by timestamp to get newest first
                .limit(MAX_EVENTS_PER_USER) // Limit query to improve performance
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        onFailureListener.onFailure(error);
                        return;
                    }
                    if (value != null) {
                        List<MoodEvent> moodEvents = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            MoodEvent moodEvent = doc.toObject(MoodEvent.class);
                            if (moodEvent != null) {
                                // Explicitly set the ID from the document
                                moodEvent.setId(doc.getId());
                                moodEvents.add(moodEvent);
                            }
                        }
                        onSuccessListener.onSuccess(moodEvents);
                    } else {
                        onSuccessListener.onSuccess(new ArrayList<>()); // Return empty list instead of null
                    }
                });
    }

    /**
     * Listens for all mood events that are created by the participants that the given participant is following
     * Optimized version that batches queries and limits results
     * @param username The username of the participant whose following's mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     */
    public void listenForEventsFromFollowing(@NonNull String username, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        ParticipantRepository participantRepository = new ParticipantRepository();

        participantRepository.fetchFollowing(username, following -> {
            if (following == null || following.isEmpty()) {
                // If not following anyone, return empty list
                onSuccessListener.onSuccess(new ArrayList<>());
                return;
            }

            // Create a list to hold all participant references
            List<DocumentReference> participantRefs = new ArrayList<>();
            for (String followingUsername : following) {
                participantRefs.add(participantRepository.getParticipantRef(followingUsername));
            }

            // If following list is large, limit it to prevent performance issues
            final int MAX_FOLLOWING = 50; // Max number of users to query
            if (participantRefs.size() > MAX_FOLLOWING) {
                participantRefs = participantRefs.subList(0, MAX_FOLLOWING);
            }

            // Use whereIn to batch query just the followed users' events (not including user's own moods)
            final int LIMIT_TOTAL_EVENTS = 50; // Limit total events returned

            // Create query - using whereIn allows us to fetch events from multiple users at once
            Query query = getMoodEventCollRef()
                    .whereIn("participantRef", participantRefs)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(LIMIT_TOTAL_EVENTS);

            // Set up snapshot listener to get real-time updates
            query.addSnapshotListener((value, error) -> {
                if (error != null) {
                    onFailureListener.onFailure(error);
                    return;
                }

                if (value != null) {
                    List<MoodEvent> allMoodEvents = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        MoodEvent moodEvent = doc.toObject(MoodEvent.class);
                        if (moodEvent != null) {
                            // Explicitly set the ID from the document
                            moodEvent.setId(doc.getId());
                            allMoodEvents.add(moodEvent);
                        }
                    }
                    onSuccessListener.onSuccess(allMoodEvents);
                } else {
                    onSuccessListener.onSuccess(new ArrayList<>());
                }
            });

        }, onFailureListener);
    }

    /**
     * Adds a mood event to the database
     * @param moodEvent The mood event to be added
     * @param onSuccessListener The listener to be called when the mood event is successfully added
     * @param onFailureListener The listener to be called when the mood event cannot be added
     */
    public void addMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().document(moodEvent.getId()).set(moodEvent)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to add mood event: " + moodEvent, e));
    }

    /**
     * Deletes a mood event from the database
     * @param moodEvent The mood event to be deleted
     * @param onSuccessListener The listener to be called when the mood event is successfully deleted
     * @param onFailureListener The listener to be called when the mood event cannot be deleted
     */
    public void deleteMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().document(moodEvent.getId()).delete()
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to delete mood event: " + moodEvent, e));
    }

    /**
     * Updates a mood event in the database
     * @param moodEvent The mood event to be updated
     * @param onSuccessListener The listener to be called when the mood event is successfully updated
     * @param onFailureListener The listener to be called when the mood event cannot be updated
     */
    public void updateMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // need to add check if the mood event id is null
        if (moodEvent.getId() == null) {
            onFailureListener.onFailure(new IllegalArgumentException("Mood event ID cannot be null"));
            return;
        }
        Log.d("MoodEventRepository", "Updating mood event with ID: " + moodEvent.getId());
        getMoodEventCollRef().document(moodEvent.getId()).set(moodEvent)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to update mood event: " + moodEvent.getId(), e));
    }
}