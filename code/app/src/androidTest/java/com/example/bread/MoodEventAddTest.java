package com.example.bread;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onIdle;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodEventAddTest {

    @Rule
    public ActivityScenarioRule<HomePage> activityRule = new ActivityScenarioRule<>(HomePage.class);

    @BeforeClass
    public static void testSetup() {
        // Connecting to emulators and creating test participant
        String androidLocalHost = "10.0.2.2";
        FirebaseFirestore.getInstance().useEmulator(androidLocalHost, 8080);
        FirebaseAuth.getInstance().useEmulator(androidLocalHost, 9099);

        try {
            Tasks.await(
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword("testUserRandom@test.com", "testPassword").addOnSuccessListener(authResult -> {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName("testUserRandom")
                                .build();
                        Objects.requireNonNull(authResult.getUser()).updateProfile(profileUpdates);
                    })
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            Tasks.await(
                    FirebaseAuth.getInstance().signInWithEmailAndPassword("testUserRandom@test.com", "testPassword")
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference participants = db.collection("participants");
        Participant p1 = new Participant();
        p1.setUsername("testUserRandom");
        DocumentReference p1Ref = participants.document("testUserRandom");
        p1Ref.set(p1);
    }

    @Before
    public void navigateToAddTab() {
        // Navigate to the "add" tab
        onView(withId(R.id.add)).perform(click());
        onIdle(); // Wait for UI to settle
    }

    @Test
    public void testAddMoodEventToFirebase() throws InterruptedException {
        // Fill in the mood event details
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Test Mood Event"));
        onView(withId(R.id.reasonEditText)).perform(replaceText("ReasonIsRandom"));
        onView(withId(R.id.triggerEditText)).perform(replaceText("Test trigger"));

        // Select emotional state
        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        onData(is(MoodEvent.EmotionalState.HAPPY)).perform(click());

        // Select social situation
        onView(withId(R.id.socialSituationSpinner)).perform(click());
        onData(is(MoodEvent.SocialSituation.ALONE)).perform(click());

        // Click save button
        onView(withId(R.id.saveButton)).perform(click());

        // Delay to allow Firebase to process
        //Thread.sleep(3000);

        // Navigate to history and check if the event is displayed
        onView(withId(R.id.history)).perform(click());
        //Thread.sleep(2000);
        onView(withText("ReasonIsRandom")).check(matches(isDisplayed()));

    }
    @Test
    public void testUiElementsAreDisplayed() {
        // Verify that key UI elements are displayed
        onView(withId(R.id.emotionalStateSpinner)).check(matches(isDisplayed()));
        onView(withId(R.id.eventTitleEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.reasonEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.triggerEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.socialSituationSpinner)).check(matches(isDisplayed()));
        onView(withId(R.id.locationCheckbox)).check(matches(isDisplayed()));
        onView(withId(R.id.saveButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyTitleValidation() throws InterruptedException {

        onView(withId(R.id.saveButton)).perform(click());

        onView(withId(R.id.eventTitleEditText)).check(matches(hasErrorText("Event title cannot be empty")));

    }

    @Test
    public void testReasonLengthValidation() {
        // Add a valid event title
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Test Event"));

        // Select a valid emotional state
        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        onData(is(MoodEvent.EmotionalState.HAPPY)).perform(click());

        // Add a reason that is too long (> 20 chars)
        onView(withId(R.id.reasonEditText)).perform(replaceText("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));

        // Click save button
        onView(withId(R.id.saveButton)).perform(click());

        // Verify error is shown
        onView(withId(R.id.reasonEditText)).check(matches(hasErrorText(containsString("Reason must be 20 characters or fewer and 3 words or fewer"))));
    }

    @Test
    public void testReasonWordCountValidation() {
        // Add a valid event title
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Test Event"));

        // Select a valid emotional state
        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        onData(is(MoodEvent.EmotionalState.HAPPY)).perform(click());

        // Add a reason with too many words (> 3 words)
        onView(withId(R.id.reasonEditText)).perform(replaceText("Typing a lot of words"));

        // Click save button
        onView(withId(R.id.saveButton)).perform(click());

        // Verify error is shown
        onView(withId(R.id.reasonEditText)).check(matches(hasErrorText(containsString("Reason must be 20 characters or fewer and 3 words or fewer"))));
    }

    @Test
    public void testNoneEmotionalStateValidation() throws InterruptedException {

        // Enter valid title
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Test Event"));


        // Enter valid reason (within limits)
        onView(withId(R.id.reasonEditText)).perform(replaceText("Some Reason"));


        onView(withId(R.id.triggerEditText)).perform(replaceText("Some Trigger"));
        onView(withId(R.id.socialSituationSpinner)).perform(click());
        onData(is(MoodEvent.SocialSituation.ALONE)).perform(click());
        // Click save
        onView(withId(R.id.saveButton)).perform(click());
        Thread.sleep(3000);
        onView(withId(R.id.eventTitleEditText)).check(matches(isDisplayed()));

    }

    @After
    public void tearDownAuth() {
        String projectId = "temp-bread";  //SET TO YOUR PROJECT ID
        URL url = null;
        try {
            url = new URL("http://10.0.2.2:9099/emulator/v1/projects/"+projectId+"/accounts");
        } catch (MalformedURLException exception) {
            Log.e("URL Error", Objects.requireNonNull(exception.getMessage()));
        }
        HttpURLConnection urlConnection = null;
        try {
            assert url != null;
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            int response = urlConnection.getResponseCode();
            Log.i("Response Code", "Response Code: " + response);
        } catch (IOException exception) {
            Log.e("IO Error", Objects.requireNonNull(exception.getMessage()));
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    @After
    public void tearDownDb() {
        String projectId = "temp-bread"; //SET TO YOUR PROJECT ID
        URL url = null;
        try {
            url = new URL("http://10.0.2.2:8080/emulator/v1/projects/"+projectId+"/databases/(default)/documents");
        } catch (MalformedURLException exception) {
            Log.e("URL Error", Objects.requireNonNull(exception.getMessage()));
        }
        HttpURLConnection urlConnection = null;
        try {
            assert url != null;
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            int response = urlConnection.getResponseCode();
            Log.i("Response Code", "Response Code: " + response);
        } catch (IOException exception) {
            Log.e("IO Error", Objects.requireNonNull(exception.getMessage()));
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
