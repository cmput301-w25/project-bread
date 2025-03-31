package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.allOf;

import android.util.Log;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.utils.EmotionUtils;
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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserProfileFragmentTest {

    @Rule
    public ActivityScenarioRule<HomePage> activityScenarioRule = new ActivityScenarioRule<>(HomePage.class);

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

        Participant p2 = new Participant();
        p2.setUsername("testUser2");
        DocumentReference p2Ref = participants.document("testUser2");
        p2Ref.set(p2);

        Participant p3 = new Participant();
        p3.setUsername("testUser3");
        DocumentReference p3Ref = participants.document("testUser3");
        p3Ref.set(p3);

        p1Ref.collection("following").document("testUser2").set(new HashMap<>() {
            {
                put("username", "testUser2");
            }
        });
        p2Ref.collection("followers").document("testUser").set(new HashMap<>() {
            {
                put("username", "testUser");
            }
        });

        p2Ref.collection("following").document("testUser").set(new HashMap<>() {
            {
                put("username", "testUser");
            }
        });
        p1Ref.collection("followers").document("testUser2").set(new HashMap<>() {
            {
                put("username", "testUser2");
            }
        });

        MoodEvent moodEvent1 = new MoodEvent("Test Event 1", "test reason 1", MoodEvent.EmotionalState.HAPPY, p2Ref);
        moodEvent1.setSocialSituation(MoodEvent.SocialSituation.ALONE);

        MoodEvent moodEvent2 = new MoodEvent("Test Event 2", "test reason 2", MoodEvent.EmotionalState.SAD, p2Ref);
        moodEvent2.setSocialSituation(MoodEvent.SocialSituation.ALONE);

        CollectionReference moodEvents = db.collection("moodEvents");
        MoodEvent[] events = {
                moodEvent1, moodEvent2
        };

        for (MoodEvent event : events) {
            moodEvents.document(event.getId()).set(event);
        }
    }

    @Test
    public void testNotFollowingUser() throws InterruptedException {
        // Navigate to a user we do not follow through the search bar
        Thread.sleep(1000);
        onView(withId(R.id.search_button)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.search_edit_text)).perform(ViewActions.typeText("test"));
        Thread.sleep(2000);
        onView(withText("testUser3")).perform(click());
        Thread.sleep(2000);
        onView(withText("testUser3")).check(matches(isDisplayed()));

        // Ensure proper NOT FOLLOWED user UI appears and that follow request works
        onView(withText("Follow")).check(matches(isDisplayed()));
        onView(withId(R.id.follow_button)).perform(click());
        Thread.sleep(2000);
        onView(withText("Requested")).check(matches(isDisplayed()));
    }

    @Test
    public void testFollowingUser() throws InterruptedException {
        // Navigate to a user we follow through the search bar
        Thread.sleep(1000);
        onView(withId(R.id.search_button)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.search_edit_text)).perform(ViewActions.typeText("test"));
        Thread.sleep(2000);
        onView(Matchers.allOf(withId(R.id.follow_username_text), withText("testUser2"),
                isDisplayed()
        )).perform(click());

        // Ensure proper FOLLOWED user information appears
        Thread.sleep(3000);
        onView(withText("Recents")).check(matches(isDisplayed()));
        onView(withText("Sad " + EmotionUtils.getEmoticon(MoodEvent.EmotionalState.SAD))).check(matches(isDisplayed()));
    }

    @Test
    public void testFollowerList() throws InterruptedException {
        // Navigate to one of our followers
        Thread.sleep(1000);
        onView(withId(R.id.profile)).perform(click());
        Thread.sleep(2000);
        onView(withId(R.id.followers_layout)).perform(click());
        Thread.sleep(2000);
        onView(withText("testUser2")).perform(click());
        Thread.sleep(2000);

        // Ensuring proper follower information appears
        onView(withText("Recents")).check(matches(isDisplayed()));
        onView(withText("Sad " + EmotionUtils.getEmoticon(MoodEvent.EmotionalState.SAD))).check(matches(isDisplayed()));
    }

    @Test
    public void testFollowingList() throws InterruptedException {
        // Navigate to a user we are following
        Thread.sleep(1000);
        onView(withId(R.id.profile)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.following_layout)).perform(click());
        Thread.sleep(2000);
        onView(withText("testUser2")).perform(click());
        Thread.sleep(2000);

        // Ensure we can see their recent mood
        onView(withText("Recents")).check(matches(isDisplayed()));
        onView(withText("Sad " + EmotionUtils.getEmoticon(MoodEvent.EmotionalState.SAD))).check(matches(isDisplayed()));
    }

    @Test
    public void testReturnUserToSelf() throws InterruptedException {
        // Navigate to a different user first
        Thread.sleep(1000);
        onView(withId(R.id.profile)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.following_layout)).perform(click());
        Thread.sleep(2000);
        onView(withText("testUser2")).perform(click());
        Thread.sleep(2000);

        // Navigate back to current user
        onView(withId(R.id.following_layout)).perform(click());
        onView(withText("testUser")).perform(click());
        Thread.sleep(1000);

        // Only current user can view requests so we check for this
        onView(withText("Requests")).check(matches(isDisplayed()));
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
