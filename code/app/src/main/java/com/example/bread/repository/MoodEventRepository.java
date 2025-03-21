package com.example.bread.repository;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.Comment;
import com.example.bread.model.MoodEvent;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Repository class for handling mood events in the database
 */
public class MoodEventRepository {
    private final FirebaseService firebaseService;
    private static final String TAG = "MoodEventRepository";
    private static final int MAX_EVENTS_PER_USER = 3;
    private final ParticipantRepository participantRepository = new ParticipantRepository();

    public MoodEventRepository() {
        firebaseService = new FirebaseService();
    }

    public MoodEventRepository(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    private CollectionReference getMoodEventCollRef() {
        return firebaseService.getDb().collection("moodEvents");
    }

    /**
     * Fetches all mood events from the database with the given participant reference
     *
     * @param participantRef    The reference to the participant whose mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     * @param limit Optional parameter to limit the number of results
     */
    public void fetchEventsWithParticipantRef(@NonNull DocumentReference participantRef,
                                              @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener,
                                              OnFailureListener onFailureListener,
                                              int limit) {
        getMoodEventCollRef()
                .whereEqualTo("participantRef", participantRef)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("MoodEventRepository", "No mood events found with participantRef: " + participantRef);
                        onSuccessListener.onSuccess(new ArrayList<>());
                        return;
                    }
                    List<MoodEvent> moodEvents = queryDocumentSnapshots.toObjects(MoodEvent.class);
                    onSuccessListener.onSuccess(moodEvents);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to fetch mood events with participantRef: " + participantRef, e));
    }

    /**
     * Fetches all mood events from the database with the given participant reference
     * Uses default limit of 20 events
     *
     * @param participantRef The reference to the participant whose mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     */
    public void fetchEventsWithParticipantRef(@NonNull DocumentReference participantRef,
                                              @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener,
                                              OnFailureListener onFailureListener) {
        fetchEventsWithParticipantRef(participantRef, onSuccessListener, onFailureListener, 20);
    }

    /**
     * Listens for all mood events from the database with the given participant reference
     *
     * @param participantRef    The reference to the participant whose mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     * @param limit Optional parameter to limit the number of results
     */
    public void listenForEventsWithParticipantRef(@NonNull DocumentReference participantRef,
                                                  @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener,
                                                  @NonNull OnFailureListener onFailureListener,
                                                  int limit) {
        // Start with a base query on participantRef
        Query query = getMoodEventCollRef()
                .whereEqualTo("participantRef", participantRef)
                .limit(limit);

        query.addSnapshotListener((value, error) -> {
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

                // Return the events without sorting - let caller handle sorting
                onSuccessListener.onSuccess(moodEvents);
            } else {
                onSuccessListener.onSuccess(new ArrayList<>());
            }
        });
    }

    /**
     * Listens for all mood events from the database with the given participant reference
     * Uses default limit of 20 events
     *
     * @param participantRef The reference to the participant whose mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     */
    public void listenForEventsWithParticipantRef(@NonNull DocumentReference participantRef,
                                                  @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener,
                                                  @NonNull OnFailureListener onFailureListener) {
        listenForEventsWithParticipantRef(participantRef, onSuccessListener, onFailureListener, 20);
    }

    /**
     * Listens for all mood events that are created by the participants that the given participant is following
     *
     * @param username          The username of the participant whose following's mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     * @param maxFollowing Optional parameter to limit the number of followed users to query
     * @param totalEventsLimit Optional parameter to limit the total number of events returned
     */
    public void listenForEventsFromFollowing(@NonNull String username,
                                             @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener,
                                             @NonNull OnFailureListener onFailureListener,
                                             int maxFollowing,
                                             int totalEventsLimit) {
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
            if (participantRefs.size() > maxFollowing) {
                participantRefs = participantRefs.subList(0, maxFollowing);
            }

            // Use whereIn to batch query just the followed users' events
            Query query = getMoodEventCollRef()
                    .whereIn("participantRef", participantRefs)
                    .limit(totalEventsLimit);

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

                    // Return the events without sorting - let caller handle sorting
                    onSuccessListener.onSuccess(allMoodEvents);
                } else {
                    onSuccessListener.onSuccess(new ArrayList<>());
                }
            });

        }, onFailureListener);
    }

    /**
     * Listens for all mood events that are created by the participants that the given participant is following
     * Uses default limits (50 following users, 50 total events)
     *
     * @param username The username of the participant whose following's mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     */
    public void listenForEventsFromFollowing(@NonNull String username,
                                             @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener,
                                             @NonNull OnFailureListener onFailureListener) {
        listenForEventsFromFollowing(username, onSuccessListener, onFailureListener, 50, 50);
    }

    /**
     * Fetches all mood events in the radius of the given location that the participant is following
     *
     * <p>
     * Referenced <a href="https://firebase.google.com/docs/firestore/solutions/geoqueries#query_geohashes">Firebase Geo-hashes</a>
     * </p>
     *
     * @param username          username of the participant
     * @param location          current location of the user
     * @param radius            radius of the area to search for mood events, in kilometers
     * @param onSuccessListener listener to be called when the mood events are successfully fetched
     * @param onFailureListener listener to be called when the mood events cannot be fetched
     */
    public void fetchForInRadiusEventsFromFollowing(@NonNull String username, @NonNull Location location, double radius, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, OnFailureListener onFailureListener) {
        GeoLocation center = new GeoLocation(location.getLatitude(), location.getLongitude());
        // Query all the bounds for the given location and radius
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radius * 1000);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds b : bounds) {
            Query q = getMoodEventCollRef().orderBy("geoInfo.geohash").startAt(b.startHash).endAt(b.endHash);
            tasks.add(q.get());
        }

        // Collect all the query results together
        Tasks.whenAllComplete(tasks).addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
            @Override
            public void onComplete(@NonNull Task<List<Task<?>>> t) {
                if (!t.isSuccessful()) {
                    onFailureListener.onFailure(t.getException() != null ? t.getException() : new Exception("Failed to fetch mood events in radius"));
                }
                List<MoodEvent> matchingDocs = new ArrayList<>();
                for (Task<QuerySnapshot> task : tasks) {
                    QuerySnapshot snap = task.getResult();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        double lat = doc.getDouble("geoInfo.latitude");
                        double lng = doc.getDouble("geoInfo.longitude");

                        GeoLocation docLocation = new GeoLocation(lat, lng);
                        double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                        if (distanceInM <= radius && !Objects.requireNonNull(doc.get("participantRef")).equals(participantRepository.getParticipantRef(username))) {
                            matchingDocs.add(doc.toObject(MoodEvent.class));
                        }

                    }
                }

                participantRepository.fetchFollowing(username, following -> {
                    Set<String> followingSet = new HashSet<>(following);
                    List<MoodEvent> filteredByFollowing = new ArrayList<>();
                    for (MoodEvent event : matchingDocs) {
                        if (followingSet.contains(event.getParticipantRef().getId())) {
                            filteredByFollowing.add(event);
                        }
                    }

                    Map<String, MoodEvent> mostRecentByUser = new HashMap<>();
                    for (MoodEvent event : filteredByFollowing) {
                        String user = event.getParticipantRef().getId();
                        if (!mostRecentByUser.containsKey(user) || event.getTimestamp().after(mostRecentByUser.get(user).getTimestamp())) {
                            mostRecentByUser.put(user, event);
                        }
                    }
                    onSuccessListener.onSuccess(new ArrayList<>(mostRecentByUser.values()));
                }, onFailureListener);
            }
        });
    }

    /**
     * Adds a mood event to the database
     *
     * @param moodEvent         The mood event to be added
     * @param onSuccessListener The listener to be called when the mood event is successfully added
     * @param onFailureListener The listener to be called when the mood event cannot be added
     */
    public void addMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().document(moodEvent.getId()).set(moodEvent)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to add mood event: " + moodEvent, e));
    }

    /**
     * Deletes a mood event from the database
     *
     * @param moodEvent         The mood event to be deleted
     * @param onSuccessListener The listener to be called when the mood event is successfully deleted
     * @param onFailureListener The listener to be called when the mood event cannot be deleted
     */
    public void deleteMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().document(moodEvent.getId()).delete()
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to delete mood event: " + moodEvent, e));
    }

    /**
     * Updates a mood event in the database
     *
     * @param moodEvent         The mood event to be updated
     * @param onSuccessListener The listener to be called when the mood event is successfully updated
     * @param onFailureListener The listener to be called when the mood event cannot be updated
     */
    public void updateMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        // Need to add check if the mood event id is null
        if (moodEvent.getId() == null) {
            if (onFailureListener != null) {
                onFailureListener.onFailure(new IllegalArgumentException("Mood event ID cannot be null"));
            }
            return;
        }
        getMoodEventCollRef().document(moodEvent.getId()).set(moodEvent)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e ->
                        Log.e(TAG, "Failed to update mood event: " + moodEvent.getId(), e));
    }

    /**
     * Fetches all comments for the given mood event
     *
     * @param moodEvent         The mood event whose comments are to be fetched
     * @param onSuccessListener The listener to be called when the comments are successfully fetched
     * @param onFailureListener The listener to be called when the comments cannot be fetched
     */
    public void fetchComments(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<List<Comment>> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().document(moodEvent.getId()).collection("comments").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("MoodEventRepository", "No comments found for mood event: " + moodEvent);
                        onSuccessListener.onSuccess(new ArrayList<>());
                        return;
                    }
                    List<Comment> comments = queryDocumentSnapshots.toObjects(Comment.class);
                    onSuccessListener.onSuccess(comments);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch comments for mood event: " + moodEvent, e));
    }

    /**
     * Adds a comment to the given mood event
     *
     * @param moodEvent         The mood event to which the comment is to be added
     * @param comment           The comment to be added
     * @param onSuccessListener The listener to be called when the comment is successfully added
     * @param onFailureListener The listener to be called when the comment cannot be added
     */
    public void addComment(@NonNull MoodEvent moodEvent, @NonNull Comment comment, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().document(moodEvent.getId()).collection("comments").document(comment.getId()).set(comment)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to add comment: " + comment, e));
    }
}