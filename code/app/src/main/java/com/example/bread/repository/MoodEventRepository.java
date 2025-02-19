package com.example.bread.repository;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.MoodEvent;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MoodEventRepository {
    private final FirebaseService firebaseService;

    public MoodEventRepository() {
        firebaseService = new FirebaseService();
    }

    private CollectionReference getMoodEventCollRef() {
        return firebaseService.getDb().collection("moodEvents");
    }

    public void fetchEventsWithParticipantRef(DocumentReference participantRef, OnCompleteListener<QuerySnapshot> onCompleteListener) {
        CollectionReference moodEventCollRef = getMoodEventCollRef();
        moodEventCollRef.whereEqualTo("participantRef", participantRef).get().addOnCompleteListener(onCompleteListener);
    }

    public void fetchEventsWithUsername(String username, OnCompleteListener<QuerySnapshot> onCompleteListener)  {
        ParticipantRepository participantRepository = new ParticipantRepository();
        participantRepository.fetchParticipantByUsername(username, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    DocumentReference participantRef = document.getReference();
                    fetchEventsWithParticipantRef(participantRef, onCompleteListener);
                }
            }
        });
    }

    public void addMoodEvent(MoodEvent moodEvent, OnCompleteListener<Void> onCompleteListener) {
        DocumentReference moodEventRef = getMoodEventCollRef().document(moodEvent.getId());
        moodEventRef.set(moodEvent).addOnCompleteListener(onCompleteListener);
    }
}
