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
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MoodEventRepository - Repository
 * <p>
 * Role / Purpose
 * Handles all interactions with the Firestore database related to MoodEvent objects.
 * Supports creating, updating, deleting, and retrieving mood events and associated comments.
 * Enables fetching mood events from followed users, with filtering by location, visibility, and recency.
 * Employs caching strategies to improve performance and reduce unnecessary network requests.
 * <p>
 * Design Patterns
 * Repository Pattern: Abstracts data access and operations, separating business logic from data layer.
 * Singleton Pattern: Leverages shared instances like FirebaseService and ParticipantRepository.
 * Observer Pattern: Uses success and failure listeners to handle asynchronous operations and UI updates.
 * Caching Strategy: Implements a simple memory-based cache for mood events to minimize redundant queries.
 * <p>
 * Outstanding Issues
 * - Partial real-time support: methods rely on direct fetches rather than listeners for data changes.
 */
public class MoodEventRepository {
    private final FirebaseService firebaseService;
    private static final String TAG = "MoodEventRepository";
    private static final int MAX_EVENTS_PER_USER = 3;
    private final ParticipantRepository participantRepository = new ParticipantRepository();
    private final Map<String, List<MoodEvent>> followingMoodsCache = new HashMap<>();
    private long lastCacheUpdateTime = 0;
    private static final long CACHE_EXPIRY_MS = 120 * 1000; // 2 minutes just in case there is no delay in fetching during demo or smn

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
        boolean offline = !FirebaseService.isNetworkConnected();
        Source source;
        if (offline) {
            source = Source.CACHE;
            Log.d(TAG, "Fetching mood events with source: CACHE (offline mode)");
        } else {
            source = Source.DEFAULT;
            Log.d(TAG, " Fetching mood events with source : Default (online mode)");
        }

        getMoodEventCollRef().whereEqualTo("participantRef", participantRef).get(source)  // Added source parameter here
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e("MoodEventRepository", "No mood events found with participantRef: " + participantRef);
                        onSuccessListener.onSuccess(new ArrayList<>());
                        return;
                    }
                    List<MoodEvent> moodEvents = queryDocumentSnapshots.toObjects(MoodEvent.class);

                    // Ensure all IDs are set
                    for (int i = 0; i < moodEvents.size(); i++) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(i);
                        MoodEvent event = moodEvents.get(i);
                        event.setId(doc.getId());
                    }

                    onSuccessListener.onSuccess(moodEvents);
                })
                .addOnFailureListener(e -> {  // Creating the 'e' variable here
                    Log.e(TAG, "Failed to fetch mood events with participantRef: " + participantRef, e);

                    // If we're online and the server query failed, try cache
                    if (!offline) {
                        Log.d(TAG, "Server fetch failed, falling back to cache");
                        getMoodEventCollRef().whereEqualTo("participantRef", participantRef)
                                .get(Source.CACHE)
                                .addOnSuccessListener(cacheResult -> {
                                    List<MoodEvent> moodEvents = cacheResult.toObjects(MoodEvent.class);
                                    Log.d(TAG, "Retrieved " + moodEvents.size() + " mood events from cache fallback");
                                    onSuccessListener.onSuccess(moodEvents);
                                })
                                .addOnFailureListener(cacheError -> {
                                    if (onFailureListener != null) {
                                        onFailureListener.onFailure(cacheError);
                                    }
                                });
                    } else if (onFailureListener != null) {
                        onFailureListener.onFailure(e);
                    }
                });
    }

    /**
     * fetches all mood events that are created by the participants that the given participant is following
     *
     * @param username          The username of the participant whose following's mood events are to be fetched
     * @param onSuccessListener The listener to be called when the mood events are successfully fetched
     * @param onFailureListener The listener to be called when the mood events cannot be fetched
     */
    public void fetchForEventsFromFollowing(@NonNull String username, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        // Check memory cache first
        long currentTime = System.currentTimeMillis();
        if (followingMoodsCache.containsKey(username) &&
                (currentTime - lastCacheUpdateTime < CACHE_EXPIRY_MS)) {
            Log.d(TAG, "Using memory cache for following moods");
            onSuccessListener.onSuccess(followingMoodsCache.get(username));
            return;
        }

        // Explicitly check network connectivity
        boolean offline = !FirebaseService.isNetworkConnected();

        if (offline) {
            Log.d(TAG, "Device is OFFLINE - forcing CACHE source only");

            participantRepository.fetchFollowing(username, following -> {
                if (following.isEmpty()) {
                    onSuccessListener.onSuccess(new ArrayList<>());
                    return;
                }

                List<MoodEvent> allMoodEvents = new ArrayList<>();
                AtomicInteger queriesRemaining = new AtomicInteger(following.size());

                for (String followingUsername : following) {
                    getMoodEventCollRef()
                            .whereEqualTo("participantRef", participantRepository.getParticipantRef(followingUsername))
                            .whereEqualTo("visibility", MoodEvent.Visibility.PUBLIC) // Add visibility filter
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(MAX_EVENTS_PER_USER)
                            .get(Source.CACHE)
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                List<MoodEvent> moodEvents = new ArrayList<>();
                                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                    MoodEvent event = doc.toObject(MoodEvent.class);
                                    if (event != null) {
                                        event.setId(doc.getId());
                                        moodEvents.add(event);
                                    }
                                }

                                Log.d(TAG, "Successfully fetched " + moodEvents.size() + " public events for " + followingUsername + " from CACHE");

                                synchronized (allMoodEvents) {
                                    allMoodEvents.addAll(moodEvents);
                                }

                                if (queriesRemaining.decrementAndGet() == 0) {
                                    followingMoodsCache.put(username, new ArrayList<>(allMoodEvents));
                                    lastCacheUpdateTime = System.currentTimeMillis();
                                    onSuccessListener.onSuccess(allMoodEvents);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to fetch public events for " + followingUsername + " from CACHE: " + e.getMessage());

                                if (queriesRemaining.decrementAndGet() == 0) {
                                    onSuccessListener.onSuccess(allMoodEvents);
                                }
                            });
                }
            }, onFailureListener);
        } else {
            Log.d(TAG, "Device is ONLINE - using DEFAULT source");

            participantRepository.fetchFollowing(username, following -> {
                if (following.isEmpty()) {
                    onSuccessListener.onSuccess(new ArrayList<>());
                    return;
                }

                List<MoodEvent> allMoodEvents = new ArrayList<>();
                AtomicInteger queriesRemaining = new AtomicInteger(following.size());

                for (String followingUsername : following) {
                    getMoodEventCollRef()
                            .whereEqualTo("participantRef", participantRepository.getParticipantRef(followingUsername))
                            .whereEqualTo("visibility", MoodEvent.Visibility.PUBLIC) // Add visibility filter
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(MAX_EVENTS_PER_USER)
                            .get(Source.DEFAULT)
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                List<MoodEvent> moodEvents = new ArrayList<>();
                                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                    MoodEvent event = doc.toObject(MoodEvent.class);
                                    if (event != null) {
                                        event.setId(doc.getId());
                                        moodEvents.add(event);
                                    }
                                }

                                synchronized (allMoodEvents) {
                                    allMoodEvents.addAll(moodEvents);
                                }

                                if (queriesRemaining.decrementAndGet() == 0) {
                                    followingMoodsCache.put(username, new ArrayList<>(allMoodEvents));
                                    lastCacheUpdateTime = System.currentTimeMillis();
                                    onSuccessListener.onSuccess(allMoodEvents);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.d(TAG, "Server fetch failed for " + followingUsername + ", falling back to cache");

                                getMoodEventCollRef()
                                        .whereEqualTo("participantRef", participantRepository.getParticipantRef(followingUsername))
                                        .whereEqualTo("visibility", MoodEvent.Visibility.PUBLIC) // Add visibility filter
                                        .orderBy("timestamp", Query.Direction.DESCENDING)
                                        .limit(MAX_EVENTS_PER_USER)
                                        .get(Source.CACHE)
                                        .addOnSuccessListener(cacheResult -> {
                                            List<MoodEvent> moodEvents = new ArrayList<>();
                                            for (DocumentSnapshot doc : cacheResult) {
                                                MoodEvent event = doc.toObject(MoodEvent.class);
                                                if (event != null) {
                                                    event.setId(doc.getId());
                                                    moodEvents.add(event);
                                                }
                                            }

                                            synchronized (allMoodEvents) {
                                                allMoodEvents.addAll(moodEvents);
                                            }

                                            if (queriesRemaining.decrementAndGet() == 0) {
                                                followingMoodsCache.put(username, new ArrayList<>(allMoodEvents));
                                                lastCacheUpdateTime = System.currentTimeMillis();
                                                onSuccessListener.onSuccess(allMoodEvents);
                                            }
                                        })
                                        .addOnFailureListener(cacheError -> {
                                            Log.e(TAG, "Cache fallback also failed for " + followingUsername, cacheError);

                                            if (queriesRemaining.decrementAndGet() == 0) {
                                                onSuccessListener.onSuccess(allMoodEvents);
                                            }
                                        });
                            });
                }
            }, onFailureListener);
        }
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
    public void fetchForInRadiusEventsFromFollowing(@NonNull String username, @NonNull Location location, double radius, @NonNull OnSuccessListener<Map<String, MoodEvent>> onSuccessListener, OnFailureListener onFailureListener) {
        GeoLocation center = new GeoLocation(location.getLatitude(), location.getLongitude());
        // Query all the bounds for the given location and radius
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radius * 1000);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds b : bounds) {
            Query q = getMoodEventCollRef().whereEqualTo("visibility", MoodEvent.Visibility.PUBLIC).orderBy("geoInfo.geohash").startAt(b.startHash).endAt(b.endHash);
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

                        if (distanceInM <= radius * 1000 && !Objects.requireNonNull(doc.get("participantRef")).equals(participantRepository.getParticipantRef(username))) {
                            Log.d(TAG, "Went inside loop");
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
//                    onSuccessListener.onSuccess(new ArrayList<>(mostRecentByUser.values()));
                    onSuccessListener.onSuccess(mostRecentByUser);
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