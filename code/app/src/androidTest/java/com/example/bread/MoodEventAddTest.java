package com.example.bread;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
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

import org.hamcrest.Matchers;
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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodEventAddTest {

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
    public void navigateToAddTab() {
        // Navigate to the "add" tab
        onView(withId(R.id.add)).perform(click());
    }

    @Before
    public void seedDatabase() {
        // Seed the database with users and relationships
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference participants = db.collection("participants");

        // Create testUser
        Participant p1 = new Participant();
        p1.setUsername("testUser");
        DocumentReference p1Ref = participants.document("testUser");
        p1Ref.set(p1);

        // Create testUser2 and make testUser2 follow testUser
        Participant p2 = new Participant();
        p2.setUsername("testUser2");
        DocumentReference p2Ref = participants.document("testUser2");
        p2Ref.set(p2);
        p2Ref.collection("following").document("testUser").set(new HashMap<String, Object>() {{
            put("username", "testUser");
        }});

        // Make testUser have testUser2 as a follower
        p1Ref.collection("followers").document("testUser2").set(new HashMap<String, Object>() {{
            put("username", "testUser2");
        }});
    }

    @Test
    public void testAddMoodEventToFirebase() throws InterruptedException {
        // Fill in the mood event details
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Test Mood Event 1"));
        onView(withId(R.id.reasonEditText)).perform(replaceText("ReasonIsRandom"));

        // Select emotional state
        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        onData(is(MoodEvent.EmotionalState.HAPPY)).inRoot(isPlatformPopup()).perform(click());

        // Select social situation
        onView(withId(R.id.socialSituationSpinner)).perform(click());
        onData(is(MoodEvent.SocialSituation.ALONE)).inRoot(isPlatformPopup()).perform(click());

        onView(withText("Save Mood")).perform(click());

        // Navigate to history and check if the event is displayed
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(2000);
        onView(withText("Test Mood Event 1")).check(matches(isDisplayed()));
    }


    @Test
    public void testPublicAndPrivateMoodEventVisibility() throws InterruptedException {
        // Step 1: As testUser, create a public mood event
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Public Mood Event"));
        onView(withId(R.id.reasonEditText)).perform(replaceText("Public Reason"));

        // Select emotional state
        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        onData(is(MoodEvent.EmotionalState.HAPPY)).inRoot(isPlatformPopup()).perform(click());

        // Select social situation
        onView(withId(R.id.socialSituationSpinner)).perform(click());
        onData(is(MoodEvent.SocialSituation.ALONE)).inRoot(isPlatformPopup()).perform(click());

        // Save the mood event
        onView(withText("Save Mood")).perform(click());

        onView(withId(R.id.add)).perform(click());

        // Step 2: As testUser, create a private mood event
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Private Mood Event"));
        onView(withId(R.id.reasonEditText)).perform(replaceText("Private Reason"));

        // Select emotional state
        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        onData(is(MoodEvent.EmotionalState.HAPPY)).inRoot(isPlatformPopup()).perform(click());

        // Select social situation
        onView(withId(R.id.socialSituationSpinner)).perform(click());
        onData(is(MoodEvent.SocialSituation.ALONE)).inRoot(isPlatformPopup()).perform(click());

        // Click the public chip to make the event private
        onView(withId(R.id.publicChip)).perform(click());

        onView(withText("Save Mood")).perform(click());

        // Step 3: Sign out testUser and sign in as testUser2
        FirebaseAuth.getInstance().signOut();

        // Create testUser2 in Firebase Auth if not already created
        try {
            Tasks.await(
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword("testUser2@test.com", "testPassword").addOnSuccessListener(authResult -> {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName("testUser2")
                                .build();
                        Objects.requireNonNull(authResult.getUser()).updateProfile(profileUpdates);
                    })
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Sign in as testUser2
        try {
            Tasks.await(
                    FirebaseAuth.getInstance().signInWithEmailAndPassword("testUser2@test.com", "testPassword")
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Step 4: Navigate to the home page as testUser2
        // Since HomePage is already open, we need to refresh the activity to reflect the new user
        activityScenarioRule.getScenario().recreate();

        // Wait for the home page to load
        Thread.sleep(2000);

        onView(withText("Public Mood Event")).check(matches(isDisplayed()));

        onView(withText("Private Mood Event")).check(doesNotExist());

    }



    @Test
    public void testUiElementsAreDisplayed() {
        // Verify that key UI elements are displayed
        onView(withId(R.id.emotionalStateSpinner)).check(matches(isDisplayed()));
        onView(withId(R.id.eventTitleEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.reasonEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.socialSituationSpinner)).check(matches(isDisplayed()));
        onView(withId(R.id.locationChip)).check(matches(isDisplayed()));
        onView(withId(R.id.publicChip)).check(matches(isDisplayed()));
        onView(withId(R.id.cancelButton)).check(matches(isDisplayed()));
        onView(withId(R.id.saveButton)).check(matches(isDisplayed()));
        onView(withId(R.id.imageAdd)).check(matches(isDisplayed()));
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
        onData(is(MoodEvent.EmotionalState.HAPPY)).inRoot(isPlatformPopup()).perform(click());

        // Add a reason that is too long (> 200 chars)
        onView(withId(R.id.reasonEditText)).perform(replaceText("ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"));

        // Click save button
        onView(withText("Save Mood")).perform(click());

        // Verify error is shown
        onView(withId(R.id.reasonEditText)).check(matches(hasErrorText(containsString("Reason must be 200 characters or fewer"))));
    }


    @Test
    public void testNoneEmotionalStateValidation() throws InterruptedException {
        // Enter valid title
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Test Event"));

        // Enter valid reason (within limits)
        onView(withId(R.id.reasonEditText)).perform(replaceText("Some Reason"));

        onView(withId(R.id.socialSituationSpinner)).perform(click());
        onData(is(MoodEvent.SocialSituation.ALONE)).inRoot(isPlatformPopup()).perform(click());

        // Click save
        onView(withText("Save Mood")).perform(click());
        Thread.sleep(3000);
        onView(withId(R.id.eventTitleEditText)).check(matches(isDisplayed()));
    }
    @After
    public void tearDownAuth() {
        String projectId = BuildConfig.FIREBASE_PROJECT_ID;
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
        String projectId = BuildConfig.FIREBASE_PROJECT_ID;
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
