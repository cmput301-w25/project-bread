package com.example.bread;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodEventEditTest {

    @Rule
    public ActivityScenarioRule<HomePage> activityScenarioRule = new ActivityScenarioRule<>(HomePage.class);

    Context context = ApplicationProvider.getApplicationContext();

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

        MoodEvent moodEvent1 = new MoodEvent("Test Event 1", "test reason", MoodEvent.EmotionalState.HAPPY, p1Ref);
        moodEvent1.setSocialSituation(MoodEvent.SocialSituation.ALONE);

        // https://stackoverflow.com/questions/38739244/convert-image-to-base64-string-android
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.test_image);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
        byte[] byteArray = byteStream.toByteArray();
        String baseString = Base64.encodeToString(byteArray,Base64.DEFAULT);
        moodEvent1.setAttachedImage(baseString);

        CollectionReference moodEvents = db.collection("moodEvents");
        MoodEvent[] events = {
                moodEvent1
        };

        for (MoodEvent event : events) {
            moodEvents.document(event.getId()).set(event);
        }
    }

    @Test
    public void testEditDialogAppears() throws InterruptedException {
        Thread.sleep(2000);

        // Navigate to the History tab
        onView(withId(R.id.history)).perform(click());

        // Ensure UI updates before proceeding
        Thread.sleep(2000);
        // Select and click the first mood event automatically
        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(0)
                .perform(click());

        // Ensure UI updates
        Thread.sleep(2000);

        // Ensure image is set
        onView(withId(R.id.mood_event_image)).check(matches(isDisplayed()));

        onView(withText("No image uploaded.")).check(doesNotExist());

        // Verify that the details dialog appears
        onView(withText("View Mood")).check(matches(isDisplayed()));

        // Click the Edit button
        onView(withText("Edit")).perform(click());

        // Ensure UI updates
        Thread.sleep(2000);

        // Verify that the edit dialog appears
        onView(withText("Edit Mood")).check(matches(isDisplayed()));

        // Verify edit dialog has the necessary fields
        onView(withId(R.id.edit_title)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_reason)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_emotion_spinner)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_social_situation_spinner)).check(matches(isDisplayed()));
        onView(withId(R.id.image_edit_button)).check(matches(isDisplayed()));
        onView(withId(R.id.delete_image_button)).check(matches(isDisplayed()));

        // Close the dialog
        onView(withText("Cancel")).perform(click());

        // Ensure UI updates
        Thread.sleep(2000);
    }

    @Test
    public void testEditDialogCanBeSaved() throws InterruptedException {
        Thread.sleep(2000);

        // Navigate to the History tab
        onView(withId(R.id.history)).perform(click());

        // Ensure UI updates before proceeding
        Thread.sleep(2000);

        // Select and click the first mood event automatically
        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(0)
                .perform(click());

        // Ensure UI updates
        Thread.sleep(2000);

        // Click the Edit button
        onView(withText("Edit")).perform(click());

        Thread.sleep(2000);

        // Make a simple change
        onView(withId(R.id.edit_reason)).perform(replaceText("Test reason"));

        // Ensure UI updates
        Thread.sleep(2000);

        // Click Save button
        onView(withText("Save")).perform(click());

        // Ensure UI updates
        Thread.sleep(2000);

        // Verify dialog is dismissed
        try {
            onView(withText("Edit Mood")).check(matches(isDisplayed()));
            throw new AssertionError("Dialog should be dismissed after saving");
        } catch (Exception e) {
            // Expected - dialog should be dismissed
        }
    }

    @Test
    public void testEditDialogCanBeCanceled() throws InterruptedException {
        Thread.sleep(2000);

        // Navigate to the History tab
        onView(withId(R.id.history)).perform(click());

        // Ensure UI updates before proceeding
        Thread.sleep(2000);

        // Select and click the first mood event automatically
        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(0)
                .perform(click());

        // Ensure UI updates
        Thread.sleep(2000);

        // Click the Edit button
        onView(withText("Edit")).perform(click());

        // Ensure UI updates
        Thread.sleep(2000);

        // Make a change
        onView(withId(R.id.edit_reason)).perform(replaceText("This should not be saved"));

        // Ensure UI updates
        Thread.sleep(2000);

        // Click Cancel button
        onView(withText("Cancel")).perform(click());

        // Ensure UI updates
        Thread.sleep(2000);

        // Verify dialog is dismissed
        try {
            onView(withText("Edit Mood")).check(matches(isDisplayed()));
            throw new AssertionError("Dialog should be dismissed after canceling");
        } catch (Exception e) {
            // Expected - dialog should be dismissed
        }
    }

    @Test
    public void removeMoodImageTest() throws InterruptedException {
        Thread.sleep(2000);

        // Navigate to the History tab
        onView(withId(R.id.history)).perform(click());

        // Ensure UI updates before proceeding
        Thread.sleep(2000);

        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(0)
                .perform(click());

        // Click the Edit button
        onView(withText("Edit")).perform(click());

        // Ensure UI updates
        Thread.sleep(2000);

        // Remove image associated with mood
        onView(withId(R.id.delete_image_button)).perform(click());

        // Ensure UI updates
        Thread.sleep(2000);

        // Save changes
        onView(withText("Save")).perform(click());

        // Select same mood image
        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(0)
                .perform(click());

        onView(withText("No image uploaded.")).check(matches(isDisplayed()));
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
