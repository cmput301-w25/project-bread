package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.bread.firebase.FirebaseService;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AnalyticsFragmentTest {

    public ActivityScenario<HomePage> scenario;

    @BeforeClass
    public static void testSetup() {
        FirebaseEmulatorRule.initializeEmulators();

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
        FirebaseFirestore db = new FirebaseService().getDb();
        CollectionReference participants = db.collection("participants");
        Participant p1 = new Participant();
        p1.setUsername("testUser");

        DocumentReference p1Ref = participants.document("testUser");
        p1Ref.set(p1);

        MoodEvent moodEvent1 = new MoodEvent("Test Event 1", "test reason", MoodEvent.EmotionalState.HAPPY, p1Ref);
        moodEvent1.setSocialSituation(MoodEvent.SocialSituation.ALONE);
        MoodEvent moodEvent2 = new MoodEvent("Test Event 2", "test reason", MoodEvent.EmotionalState.ANGRY, p1Ref);
        moodEvent2.setSocialSituation(MoodEvent.SocialSituation.WITH_FRIENDS);
        MoodEvent moodEvent3 = new MoodEvent("Test Event 3", "test reason", MoodEvent.EmotionalState.SURPRISED, p1Ref);

        CollectionReference moodEvents = db.collection("moodEvents");
        MoodEvent[] events = {
                moodEvent1, moodEvent2, moodEvent3
        };

        for (MoodEvent event : events) {
            moodEvents.document(event.getId()).set(event);
        }
        scenario = ActivityScenario.launch(HomePage.class);
    }

    @Test
    public void TestAnalyticsFragmentShowsGraphs() throws InterruptedException {
        Thread.sleep(2000);
        onView(withId(R.id.history)).perform(click());
        onView(withId(R.id.chartButton)).perform(click());

        // Check that the chart is displayed
        onView(withId(R.id.pie_chart)).check(matches(isDisplayed()));
        onView(withId(R.id.bar_monthly_chart)).check(matches(isDisplayed()));
        onView(withId(R.id.line_monthly_chart)).check(matches(isDisplayed()));

        // Check the streaks are displayed
        onView(withId(R.id.streak_text)).check(matches(isDisplayed()));
        onView(withId(R.id.longest_streak_text)).check(matches(isDisplayed()));

        onView(withId(R.id.streak_text)).check(matches(withText("3")));
        onView(withId(R.id.longest_streak_text)).check(matches(withText("3")));
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        clearFirestoreEmulator();
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

    @AfterClass
    public static void clearAuthEmulator() {
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
