package com.example.bread;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.bread.model.Comment;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.view.HomePage;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailTest {

    @Rule
    public ActivityScenarioRule<HomePage> activityScenarioRule = new ActivityScenarioRule<>(HomePage.class);

    @BeforeClass
    public static void testSetup() {
        String androidLocalHost = "10.0.2.2";
        FirebaseFirestore.getInstance().useEmulator(androidLocalHost, 8080);
        FirebaseAuth.getInstance().useEmulator(androidLocalHost, 9099);

        try {
            Tasks.await(
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword("testUser@test.com", "testPassword").addOnSuccessListener(authResult -> {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName("testUser")
                                .build();
                        Objects.requireNonNull(authResult.getUser()).updateProfile(profileUpdates);
                    })
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            Tasks.await(
                    FirebaseAuth.getInstance().signInWithEmailAndPassword("testUser@test.com", "testPassword")
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void seedDatabase() {
        // Seed the database with some mood events
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference participants = db.collection("participants");
        Participant p1 = new Participant();
        p1.setUsername("testUser");

        DocumentReference p1Ref = participants.document("testUser");
        p1Ref.set(p1);
        p1Ref.collection("following").document("testUser2").set(new HashMap<>() {
            {
                put("username", "testUser2");
            }
        });

        Participant p2 = new Participant();
        p2.setUsername("testUser2");

        DocumentReference p2Ref = participants.document("testUser2");
        p2Ref.set(p2);
        p2Ref.collection("followers").document("testUser").set(new HashMap<>() {
            {
                put("username", "testUser");
            }
        });

        MoodEvent moodEvent1 = new MoodEvent("Test Event 1", "test reason", MoodEvent.EmotionalState.HAPPY, p2Ref);
        moodEvent1.setSocialSituation(MoodEvent.SocialSituation.ALONE);
        MoodEvent moodEvent2 = new MoodEvent("Test Event 2", "test reason", MoodEvent.EmotionalState.ANGRY, p2Ref);
        moodEvent2.setSocialSituation(MoodEvent.SocialSituation.WITH_FRIENDS);

        CollectionReference moodEvents = db.collection("moodEvents");
        MoodEvent[] events = {
                moodEvent1, moodEvent2
        };

        for (MoodEvent event : events) {
            moodEvents.document(event.getId()).set(event);
        }

        Comment comment1 = new Comment(p1Ref, "test comment 1");
        Comment comment2 = new Comment(p1Ref, "test comment 2");

        for (Comment comment : List.of(comment1, comment2)) {
            moodEvents.document(moodEvent1.getId()).collection("comments")
                    .document(comment.getId()).set(comment);
        }

        for (Comment comment : List.of(comment1, comment2)) {
            moodEvents.document(moodEvent2.getId()).collection("comments")
                    .document(comment.getId()).set(comment);
        }
    }

    @Test
    public void testEventDetailOpens() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onData(anything()).inAdapterView(withId(R.id.homeListView)).atPosition(0).perform(click());

        // Check that the event detail dialog is displayed
        onView(withId(R.id.event_title)).check(matches(isDisplayed()));
        onView(withId(R.id.emotional_state_text)).check(matches(isDisplayed()));
        onView(withId(R.id.social_situation_text)).check(matches(isDisplayed()));
        onView(withId(R.id.reason_text)).check(matches(isDisplayed()));
        onView(withText("test comment 1")).check(matches(isDisplayed()));
        onView(withText("test comment 2")).check(matches(isDisplayed()));
    }

    @Test
    public void testEventDetailsCommentDialog() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onData(anything()).inAdapterView(withId(R.id.homeListView)).atPosition(0).perform(click());
        onView(withId(R.id.add_comment_fab)).perform(click());
        onView(withId(R.id.comment_edit_text)).check(matches(isDisplayed()));
        onView(withId(R.id.add_comment_button)).check(matches(isDisplayed()));

        onView(withId(R.id.comment_edit_text)).perform(typeText("test comment 3"));
        onView(withId(R.id.add_comment_button)).perform(click());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onView(withText("test comment 3")).check(matches(isDisplayed()));
    }

    @After
    public void tearDown() {
        clearFirestoreEmulator();
        clearAuthEmulator();
    }

    private void clearFirestoreEmulator() {
        String projectId = BuildConfig.FIREBASE_PROJECT_ID;
        String firestoreUrl = "http://10.0.2.2:8080/emulator/v1/projects/"
                + projectId
                + "/databases/(default)/documents";

        HttpURLConnection connection = null;
        try {
            URL url = new URL(firestoreUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            int responseCode = connection.getResponseCode();
            Log.i("tearDown", "Cleared Firestore emulator, response code: " + responseCode);
        } catch (IOException e) {
            Log.e("tearDown", "Error clearing Firestore emulator", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void clearAuthEmulator() {
        String projectId = BuildConfig.FIREBASE_PROJECT_ID;
        // This is the Auth emulator endpoint for deleting all test users
        String authUrl = "http://10.0.2.2:9099/emulator/v1/projects/"
                + projectId
                + "/accounts";

        HttpURLConnection connection = null;
        try {
            URL url = new URL(authUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            int responseCode = connection.getResponseCode();
            Log.i("tearDown", "Cleared Auth emulator users, response code: " + responseCode);
        } catch (IOException e) {
            Log.e("tearDown", "Error clearing Auth emulator", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
