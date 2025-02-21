package com.example.bread.repository;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.Participant;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

public class ParticipantRepository {
    private final FirebaseService firebaseService;

    public ParticipantRepository() {
        firebaseService = new FirebaseService();
    }

    private CollectionReference getParticipantCollRef() {
        return firebaseService.getDb().collection("participants");
    }

    public void fetchParticipantByUsername(String username, OnCompleteListener<DocumentSnapshot> onCompleteListener) {
        CollectionReference participantCollRef = getParticipantCollRef();
        DocumentReference participantRef = participantCollRef.document(username);
        participantRef.get().addOnCompleteListener(onCompleteListener);
    }

    public void addParticipant(Participant participant, OnCompleteListener<Void> onCompleteListener) {
        DocumentReference participantRef = getParticipantCollRef().document(participant.getUsername());
        participantRef.set(participant).addOnCompleteListener(onCompleteListener);
    }
}
