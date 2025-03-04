package com.example.bread.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.MoodEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.List;

public class MoodEventRepository {
    private final FirebaseService firebaseService;

    public MoodEventRepository() {
        firebaseService = new FirebaseService();
    }

    private CollectionReference getMoodEventCollRef() {
        return firebaseService.getDb().collection("moodEvents");
    }

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
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("MoodEventRepository", "Failed to fetch mood events with participantRef: " + participantRef, e));
    }

    public void listenForEventsWithParticipantRef(@NonNull DocumentReference participantRef, @NonNull OnSuccessListener<List<MoodEvent>> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        getMoodEventCollRef().whereEqualTo("participantRef", participantRef)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        onFailureListener.onFailure(error);
                        return;
                    }
                    if (value != null) {
                        List<MoodEvent> moodEvents = value.toObjects(MoodEvent.class);
                        onSuccessListener.onSuccess(moodEvents);
                    }
                }); //https://firebase.google.com/docs/firestore/query-data/listen
    }

    public void addMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().document(moodEvent.getId()).set(moodEvent)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("MoodEventRepository", "Failed to add mood event: " + moodEvent, e));
    }

    public void deleteMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().document(moodEvent.getId()).delete()
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("MoodEventRepository", "Failed to delete mood event: " + moodEvent, e));
    }

    public void updateMoodEvent(@NonNull MoodEvent moodEvent, @NonNull OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        getMoodEventCollRef().document(moodEvent.getId()).set(moodEvent)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener != null ? onFailureListener : e -> Log.e("MoodEventRepository", "Failed to update mood event: " + moodEvent.getId(), e));
    }
}
