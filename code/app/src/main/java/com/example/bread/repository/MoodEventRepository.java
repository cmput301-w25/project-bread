package com.example.bread.repository;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bread.firebase.FirebaseService;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository class for handling mood events in the database
 */
public class MoodEventRepository {
    private final FirebaseService firebaseService;
    private static final String TAG = "MoodEventRepository";

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
     */
    public void fetchEventsWithParticipantRef(@NonNull DocumentReference participantRef, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().whereEqualTo("participantRef", participantRef).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("MoodEventRepository", "No mood events found with participantRef: " + participantRef);
                        onSuccessListener.onSuccess(null);
                        return;
                    }
                    List<MoodEvent> moodEvents = queryDocumentSnapshots.toObjects(MoodEvent.class);
                    onSuccessListener.onSuccess(moodEvents);
                })
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to fetch mood events with participantRef: " + participantRef, e));
    }

    /**
     * Listens for all mood events from the database with the given participant reference
     *
     * @param participantRef    The reference to the participant whose mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     */
    public void listenForEventsWithParticipantRef(@NonNull DocumentReference participantRef, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        getMoodEventCollRef().whereEqualTo("participantRef", participantRef)
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
                                Log.d("MoodEventRepository", "Loaded mood with ID: " + doc.getId());
                                moodEvents.add(moodEvent);
                            }
                        }
                        onSuccessListener.onSuccess(moodEvents);
                    }
                });
    }

    /**
     * Listens for all mood events that are created by the participants that the given participant is following
     *
     * @param username          The username of the participant whose following's mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     */
    public void listenForEventsFromFollowing(@NonNull String username, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        ParticipantRepository participantRepository = new ParticipantRepository();
        participantRepository.fetchFollowing(username, following -> {
            List<MoodEvent> allMoodEvents = new ArrayList<>();
            AtomicInteger queriesRemaining = new AtomicInteger(following.size());
            for (String followingUsername : following) {
                getMoodEventCollRef().whereEqualTo("participantRef", participantRepository.getParticipantRef(followingUsername))
                        .addSnapshotListener(((value, error) -> {
                            if (error != null) {
                                onFailureListener.onFailure(error);
                                return;
                            } else if (value != null) {
                                List<MoodEvent> moodEvents = value.toObjects(MoodEvent.class);
                                synchronized (allMoodEvents) {
                                    allMoodEvents.addAll(moodEvents);
                                }
                            }
                            if (queriesRemaining.decrementAndGet() == 0) {
                                onSuccessListener.onSuccess(allMoodEvents);
                            }
                        }));
            }
        }, onFailureListener);
    }

    /**
     * Fetches all mood events in the radius of the given location that the participant is following
     *
     * <p>
     * Referenced <a href="https://firebase.google.com/docs/firestore/solutions/geoqueries#query_geohashes">Firebase Geo-hashes</a>
     * </p>
     *
     * @param location          current location of the user
     * @param radius            radius of the area to search for mood events, in kilometers
     * @param onSuccessListener listener to be called when the mood events are successfully fetched
     * @param onFailureListener listener to be called when the mood events cannot be fetched
     */
    public void fetchForInRadiusEvents(@NonNull String username, @NonNull Location location, double radius, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, OnFailureListener onFailureListener) {
        ParticipantRepository participantRepository = new ParticipantRepository();
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
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to add mood event: " + moodEvent, e));
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
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to delete mood event: " + moodEvent, e));
    }

    /**
     * Updates a mood event in the database
     *
     * @param moodEvent         The mood event to be updated
     * @param onSuccessListener The listener to be called when the mood event is successfully updated
     * @param onFailureListener The listener to be called when the mood event cannot be updated
     */
    public void updateMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {

        // need to add check if the mood event id is null
        if (moodEvent.getId() == null) {
            onFailureListener.onFailure(new IllegalArgumentException("Mood event ID cannot be null"));
            return;
        }
        getMoodEventCollRef().document(moodEvent.getId()).set(moodEvent)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e(TAG, "Failed to update mood event: " + moodEvent.getId(), e));
    }
}
