package com.example.bread;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;

import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MoodHistoryFragmentTest {

    @Rule
    public ActivityScenarioRule<HomePage> activityScenarioRule = new ActivityScenarioRule<>(HomePage.class);

    @BeforeClass
    public static void testSetup() {
        // Connecting to emulators and creating test participant
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
        // Seed the database with user mood events
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference participants = db.collection("participants");
        Participant p1 = new Participant();
        p1.setUsername("testUser");

        DocumentReference p1Ref = participants.document("testUser");
        p1Ref.set(p1);

        MoodEvent m1 = new MoodEvent("Test Event 1", "test reason 1", MoodEvent.EmotionalState.HAPPY, p1Ref);
        MoodEvent m2 = new MoodEvent("Test Event 2", "test reason 2", MoodEvent.EmotionalState.SAD, p1Ref);
        MoodEvent m3 = new MoodEvent("Test Event 3", "test reason 3", MoodEvent.EmotionalState.CONFUSED, p1Ref);
        MoodEvent m4 = new MoodEvent("Test Event 4", "test reason 4", MoodEvent.EmotionalState.NEUTRAL, p1Ref);

        // Manually setting time rather than having it done automatically so we can test for sorting
        m1.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 1).getTime());
        m2.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 2).getTime());
        m3.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 3).getTime());
        m4.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 4).getTime());

        db.collection("moodEvents").document("mood1").set(m1);
        db.collection("moodEvents").document("mood2").set(m2);
        db.collection("moodEvents").document("mood3").set(m3);
        db.collection("moodEvents").document("mood4").set(m4);
    }

    @Test
    public void displaysAllMoodEventsTest() throws InterruptedException {
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);
        onView(withText("Test Event 1")).check(matches(isDisplayed()));
        onView(withText("Test Event 2")).check(matches(isDisplayed()));
        onView(withText("Test Event 3")).check(matches(isDisplayed()));
        onView(withText("Test Event 4")).check(matches(isDisplayed()));
    }

    @Test
    public void sortReverseChronologicalOrderTest() throws InterruptedException {
        Thread.sleep(2000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(3000);
        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(0)
                .onChildView(withId(R.id.history_title_text))
                .check(matches(withText("Test Event 4")));
        Thread.sleep(1000);
        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(3)
                .onChildView(withId(R.id.history_title_text))
                .check(matches(withText("Test Event 1")));
    }

    @Test
    public void deleteMoodEventTest() throws InterruptedException {
        Thread.sleep(2000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(2000);
        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(0)
                .onChildView(withId(R.id.checkbox))
                .perform(click());

        onView(withId(R.id.deleteButton)).perform(click());

        onView(withId(android.R.id.button1)).perform(click());
        onView(withText("Test Event 4")).check(doesNotExist());
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
