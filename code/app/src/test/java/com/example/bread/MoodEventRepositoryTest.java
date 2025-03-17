package com.example.bread;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class MoodEventRepositoryTest {
    @Mock
    private FirebaseFirestore mockFirestore;
    @Mock
    private CollectionReference mockEventColl;
    private MoodEventRepository moodEventRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockFirestore.collection("moodEvents")).thenReturn(mockEventColl);
        FirebaseService firebaseService = new FirebaseService(mockFirestore);
        moodEventRepository = new MoodEventRepository(firebaseService);
    }

    private Task<QuerySnapshot> createSuccessQuerySnapshotTask(QuerySnapshot querySnapshot) {
        // We can mock a Task<QuerySnapshot> and manually invoke onSuccessListener in doAnswer
        Task<QuerySnapshot> mockTask = mock(Task.class);

        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockTask.getResult()).thenReturn(querySnapshot);

        doAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> successListener = invocation.getArgument(0);
            successListener.onSuccess(querySnapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));

        return mockTask;
    }

    @Test
    public void TestFetchEventsWithParticipantRef_Success() {
        DocumentReference mockParticipantRef = mock(DocumentReference.class);
        Query mockQuery = mock(Query.class);
        when(mockEventColl.whereEqualTo("participantRef", mockParticipantRef)).thenReturn(mockQuery);

        QuerySnapshot mockSnapshot = mock(QuerySnapshot.class);
        List<MoodEvent> dummyEvents = new ArrayList<>();
        dummyEvents.add(new MoodEvent("test title", "test reason", MoodEvent.EmotionalState.NEUTRAL, mockParticipantRef));
        dummyEvents.add(new MoodEvent("test title 2", "test reason 2", MoodEvent.EmotionalState.HAPPY, mockParticipantRef));
        when(mockSnapshot.toObjects(MoodEvent.class)).thenReturn(dummyEvents);
        when(mockSnapshot.isEmpty()).thenReturn(false);

        Task<QuerySnapshot> successTask = createSuccessQuerySnapshotTask(mockSnapshot);
        when(mockQuery.get()).thenReturn(successTask);

        OnSuccessListener<List<MoodEvent>> onSuccessListener = mock(OnSuccessListener.class);
        OnFailureListener onFailureListener = mock(OnFailureListener.class);

        moodEventRepository.fetchEventsWithParticipantRef(mockParticipantRef, onSuccessListener, onFailureListener);

        verify(onSuccessListener).onSuccess(dummyEvents);
        verify(onFailureListener, never()).onFailure(any());
    }

    @Test
    public void TestAddMoodEvent_Success() {
        DocumentReference mockParticipantRef = mock(DocumentReference.class);
        Query mockQuery = mock(Query.class);
        when(mockEventColl.whereEqualTo("participantRef", mockParticipantRef)).thenReturn(mockQuery);

        MoodEvent mockMoodEvent = new MoodEvent("test title", "test reason", MoodEvent.EmotionalState.NEUTRAL, mockParticipantRef);
        when(mockEventColl.document(mockMoodEvent.getId())).thenAnswer(invocation -> {
            DocumentReference docRef = mock(DocumentReference.class);
            Task<Void> mockTask = mock(Task.class);
            doAnswer(invocation1 -> {
                OnSuccessListener<Void> successListener = invocation1.getArgument(0);
                successListener.onSuccess(null);
                return mockTask;
            }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
            when(docRef.set(mockMoodEvent)).thenReturn(mockTask);
            return docRef;
        });

        OnSuccessListener<Void> successListener = mock(OnSuccessListener.class);
        OnFailureListener onFailureListener = mock(OnFailureListener.class);

        moodEventRepository.addMoodEvent(mockMoodEvent, successListener, onFailureListener);

        verify(successListener).onSuccess(null);
        verify(onFailureListener, never()).onFailure(any());
    }

    @Test
    public void testUpdateMoodEvent_Success() {
        DocumentReference mockParticipantRef = mock(DocumentReference.class);
        Query mockQuery = mock(Query.class);
        when(mockEventColl.whereEqualTo("participantRef", mockParticipantRef)).thenReturn(mockQuery);

        MoodEvent mockMoodEvent = new MoodEvent("test title", "test reason", MoodEvent.EmotionalState.NEUTRAL, mockParticipantRef);

        when(mockEventColl.document(mockMoodEvent.getId())).thenAnswer(invocation -> {
            DocumentReference docRef = mock(DocumentReference.class);

            Task<Void> mockTask = mock(Task.class);
            doAnswer(invocation1 -> {
                OnSuccessListener<Void> successListener = invocation1.getArgument(0);
                successListener.onSuccess(null);
                return mockTask;
            }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));

            when(docRef.set(mockMoodEvent)).thenReturn(mockTask);
            return docRef;
        });

        OnSuccessListener<Void> successListener = mock(OnSuccessListener.class);
        OnFailureListener failureListener = mock(OnFailureListener.class);

        moodEventRepository.updateMoodEvent(mockMoodEvent, successListener, failureListener);

        verify(successListener).onSuccess(null);
        verify(failureListener, never()).onFailure(any());
    }

    @Test
    public void testDeleteMoodEvent_Success() {
        DocumentReference mockParticipantRef = mock(DocumentReference.class);
        Query mockQuery = mock(Query.class);
        when(mockEventColl.whereEqualTo("participantRef", mockParticipantRef)).thenReturn(mockQuery);

        MoodEvent mockMoodEvent = new MoodEvent("test title", "test reason", MoodEvent.EmotionalState.NEUTRAL, mockParticipantRef);

        when(mockEventColl.document(mockMoodEvent.getId())).thenAnswer(invocation -> {
            DocumentReference docRef = mock(DocumentReference.class);

            Task<Void> mockTask = mock(Task.class);
            doAnswer(invocation1 -> {
                OnSuccessListener<Void> successListener = invocation1.getArgument(0);
                successListener.onSuccess(null);
                return mockTask;
            }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));

            when(docRef.delete()).thenReturn(mockTask);
            return docRef;
        });

        OnSuccessListener<Void> successListener = mock(OnSuccessListener.class);
        OnFailureListener failureListener = mock(OnFailureListener.class);

        moodEventRepository.deleteMoodEvent(mockMoodEvent, successListener, failureListener);

        verify(successListener).onSuccess(null);
        verify(failureListener, never()).onFailure(any());
    }
}
