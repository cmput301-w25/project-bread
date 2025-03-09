package com.example.bread;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;

import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

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
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomeFragmentActivityTest {

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

        CollectionReference moodEvents = db.collection("moodEvents");
        MoodEvent[] events = {
                new MoodEvent("Test Event 1", "test reason", MoodEvent.EmotionalState.HAPPY, p2Ref),
                new MoodEvent("Test Event 2", "test reason", MoodEvent.EmotionalState.ANGRY, p2Ref),
        };

        for (MoodEvent event : events) {
            moodEvents.add(event);
        }
    }

    @Test
    public void homeFragmentShowsEvents() {

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onData(anything()).inAdapterView(withId(R.id.homeListView)).atPosition(0).check(matches(isDisplayed()));
        onData(anything()).inAdapterView(withId(R.id.homeListView)).atPosition(1).check(matches(isDisplayed()));
    }

    @After
    public void tearDown() {
        clearFirestoreEmulator();
        clearAuthEmulator();
    }

    private void clearFirestoreEmulator() {
        String projectId = "project-db"; // Change to your project ID
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
        String projectId = "project-db"; // Change to your project ID
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
