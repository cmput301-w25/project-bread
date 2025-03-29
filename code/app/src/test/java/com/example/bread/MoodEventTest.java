package com.example.bread;

import static org.mockito.Mockito.mock;

import com.example.bread.model.MoodEvent;
import com.google.firebase.firestore.DocumentReference;

import org.junit.Test;

import java.util.GregorianCalendar;

public class MoodEventTest {
    @Test
    public void TestMoodEventConstructor() {
        DocumentReference mockParticipantRef = mock(DocumentReference.class);
        MoodEvent moodEvent = new MoodEvent("Test Title", "Test Reason", MoodEvent.EmotionalState.HAPPY, mockParticipantRef);
        assert moodEvent.getTimestamp() == null;
        assert moodEvent.getTitle().equals("Test Title");
        assert moodEvent.getReason().equals("Test Reason");
        assert moodEvent.getEmotionalState().equals(MoodEvent.EmotionalState.HAPPY);
        assert moodEvent.getParticipantRef().equals(mockParticipantRef);
    }

    @Test
    public void TestMoodEventSetters() {
        DocumentReference mockParticipantRef = mock(DocumentReference.class);
        MoodEvent moodEvent = new MoodEvent("Test Title", "Test Reason", MoodEvent.EmotionalState.HAPPY, mockParticipantRef);
        moodEvent.setTimestamp(new GregorianCalendar(2024, 11, 31).getTime());
        assert moodEvent.getTimestamp().equals(new GregorianCalendar(2024, 11, 31).getTime());
        moodEvent.setTitle("New Title");
        assert moodEvent.getTitle().equals("New Title");
        moodEvent.setReason("New Reason");
        assert moodEvent.getReason().equals("New Reason");
        moodEvent.setEmotionalState(MoodEvent.EmotionalState.SAD);
        assert moodEvent.getEmotionalState().equals(MoodEvent.EmotionalState.SAD);
        DocumentReference newMockParticipantRef = mock(DocumentReference.class);
        moodEvent.setParticipantRef(newMockParticipantRef);
        assert moodEvent.getParticipantRef().equals(newMockParticipantRef);
    }

    @Test
    public void TestSocialSituationToString() {
        assert MoodEvent.SocialSituation.WITH_COWORKERS.toString().equals("With Coworkers");
        assert MoodEvent.SocialSituation.WITH_STRANGERS.toString().equals("With Strangers");
        assert MoodEvent.SocialSituation.WITH_FAMILY.toString().equals("With Family");
        assert MoodEvent.SocialSituation.WITH_FRIENDS.toString().equals("With Friends");
        assert MoodEvent.SocialSituation.ALONE.toString().equals("Alone");
        assert MoodEvent.SocialSituation.WITH_ONE_OTHER_PERSON.toString().equals("With One Other Person");
        assert MoodEvent.SocialSituation.WITH_TWO_TO_SEVERAL_PEOPLE.toString().equals("With Two To Several People");
    }

    @Test
    public void TestEmotionalStateToString() {
        assert MoodEvent.EmotionalState.HAPPY.toString().equals("Happy");
        assert MoodEvent.EmotionalState.SURPRISED.toString().equals("Surprised");
        assert MoodEvent.EmotionalState.SAD.toString().equals("Sad");
        assert MoodEvent.EmotionalState.ANGRY.toString().equals("Angry");
        assert MoodEvent.EmotionalState.NEUTRAL.toString().equals("Neutral");
    }

    @Test
    public void TestMoodEventScore() {
        DocumentReference mockParticipantRef = mock(DocumentReference.class);
        MoodEvent moodEvent = new MoodEvent("Test Title", "Test Reason", MoodEvent.EmotionalState.HAPPY, mockParticipantRef);
        assert moodEvent.getScore() == 2;
        moodEvent.setEmotionalState(MoodEvent.EmotionalState.SURPRISED);
        assert moodEvent.getScore() == 1;
        moodEvent.setEmotionalState(MoodEvent.EmotionalState.SAD);
        assert moodEvent.getScore() == -2;
        moodEvent.setEmotionalState(MoodEvent.EmotionalState.ANGRY);
        assert moodEvent.getScore() == -1;
        moodEvent.setEmotionalState(MoodEvent.EmotionalState.NEUTRAL);
        assert moodEvent.getScore() == 0;
    }
}
