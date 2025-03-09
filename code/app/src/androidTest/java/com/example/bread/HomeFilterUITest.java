package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import android.util.Log;
import androidx.test.espresso.action.ViewActions;
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
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class HomeFilterUITest {
    @Rule
    public ActivityScenarioRule<HomePage> activityScenarioRule = new ActivityScenarioRule<>(HomePage.class);

    @BeforeClass
    public static void testSetup() {
        // Connecting to emulators and creating test participant to represent signed in user
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

        MoodEvent m1 = new MoodEvent("Test Event 1", "test reason 1", MoodEvent.EmotionalState.HAPPY, p2Ref);
        MoodEvent m2 = new MoodEvent("Test Event 2", "test reason 2", MoodEvent.EmotionalState.ANGRY, p2Ref);
        MoodEvent m3 = new MoodEvent("Test Event 3", "test reason 3", MoodEvent.EmotionalState.SAD, p2Ref);
        m1.setTimestamp(new GregorianCalendar(2024, Calendar.MARCH, 1).getTime());
        m2.setTimestamp(new GregorianCalendar(2024, Calendar.MARCH, 2).getTime());
        m3.setTimestamp(new Date());
        db.collection("moodEvents").document("mood1").set(m1);
        db.collection("moodEvents").document("mood2").set(m2);
        db.collection("moodEvents").document("mood3").set(m3);
    }

    // testing for everything to appear correctly
    @Test
    public void displaysAllMoodEventsAndDialogTest() throws InterruptedException, ExecutionException {
        Thread.sleep(2000);
        onView(withText("test reason 1")).check(matches(isDisplayed()));
        onView(withText("test reason 2")).check(matches(isDisplayed()));
        onView(withText("test reason 3")).check(matches(isDisplayed()));

        Thread.sleep(2000);
        // Click the filter button
        onView(withId(R.id.filter_button_home)).perform(click());

        // Verify that the dialog appears with expected elements
        onView(withText("Filter Mood Events")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withId(R.id.recent_week_switch)).check(matches(isDisplayed()));
        onView(withId(R.id.mood_spinner)).check(matches(isDisplayed()));
        onView(withId(R.id.keyword_edit_text)).check(matches(isDisplayed()));
        onView(withId(R.id.apply_button)).check(matches(isDisplayed()));
        onView(withId(R.id.reset_button)).check(matches(isDisplayed()));
    }

    @Test
    public void filterByNothingTest() throws InterruptedException {
        Thread.sleep(2000);
        onView(withId(R.id.filter_button_home)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.apply_button)).perform(click());

        Thread.sleep(1000);
        onView(withText("test reason 1")).check(matches(isDisplayed()));
        onView(withText("test reason 2")).check(matches(isDisplayed()));
        onView(withText("test reason 3")).check(matches(isDisplayed()));
    }

    //  test for filtering by most recent week
    @Test
    public void filterByMostRecentWeekTest() throws InterruptedException {
        Thread.sleep(1000);

        onView(withId(R.id.filter_button_home)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.recent_week_switch)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.apply_button)).perform(click());

        Thread.sleep(1000);
        onView(withText("test reason 3")).check(matches(isDisplayed()));
        onView(withText("test reason 2")).check(doesNotExist());
        onView(withText("test reason 1")).check(doesNotExist());
    }

    //  test for filtering by mood state
    @Test
    public void filterByMoodStateTest() throws InterruptedException {
        Thread.sleep(1000);

        onView(withId(R.id.filter_button_home)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.mood_spinner)).perform(click());
        Thread.sleep(1000);

        onView(withText("HAPPY")) //searches for "HAPPY" state within spinner
                .inRoot(isPlatformPopup()) //ensure we look in the popup filter window not main screen
                .perform(click());

        onView(withId(R.id.apply_button)).perform(click());

        Thread.sleep(1000);
        onView(withText("test reason 3")).check(doesNotExist());
        onView(withText("test reason 1")).check(matches(isDisplayed()));
        onView(withText("test reason 2")).check(doesNotExist());
    }

    // test for filtering by keyword reason
    @Test
    public void filterByReasonKeywordTest() throws InterruptedException {
        Thread.sleep(1000);

        onView(withId(R.id.filter_button_home)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.keyword_edit_text)).perform(ViewActions.typeText("2"));
        Thread.sleep(1000);
        onView(withId(R.id.apply_button)).perform(click());

        Thread.sleep(1000);
        onView(withText("test reason 3")).check(doesNotExist());
        onView(withText("test reason 1")).check(doesNotExist());
        onView(withText("test reason 2")).check(matches(isDisplayed()));
    }

    // test for filtering by all three
    @Test
    public void filterByAllThreeFilters() throws InterruptedException {
        Thread.sleep(1000);

        //filtering by recent week
        onView(withId(R.id.filter_button_home)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.recent_week_switch)).perform(click());
        Thread.sleep(1000);

        //filtering by dropdown
        onView(withId(R.id.mood_spinner)).perform(click());
        Thread.sleep(1000);
        onView(withText("SAD")) //searches for "HAPPY" state within spinner
                .inRoot(isPlatformPopup()) //ensure we look in the popup filter window not main screen
                .perform(click());

        //filtering by keyword
        onView(withId(R.id.keyword_edit_text)).perform(ViewActions.typeText("3"));
        Thread.sleep(1000);
        onView(withId(R.id.apply_button)).perform(click());

        Thread.sleep(1000);
        onView(withText("test reason 3")).check(matches(isDisplayed()));
        onView(withText("test reason 2")).check(doesNotExist());
        onView(withText("test reason 1")).check(doesNotExist());
    }

    // test for filtering for one thing then another
    @Test
    public void filterByMultipleFiltersTest() throws InterruptedException {
        Thread.sleep(1000);

        //filtering by most recent week first
        onView(withId(R.id.filter_button_home)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.recent_week_switch)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.apply_button)).perform(click());
        Thread.sleep(1000);
        onView(withText("test reason 3")).check(matches(isDisplayed()));
        onView(withText("test reason 2")).check(doesNotExist());
        onView(withText("test reason 1")).check(doesNotExist());

        //unselect most recent week
        onView(withId(R.id.filter_button_home)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.recent_week_switch)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.apply_button)).perform(click());
        Thread.sleep(1000);
        onView(withText("test reason 1")).check(matches(isDisplayed()));
        onView(withText("test reason 2")).check(matches(isDisplayed()));
        onView(withText("test reason 3")).check(matches(isDisplayed()));

        //keyword search test
        onView(withId(R.id.filter_button_home)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.keyword_edit_text)).perform(ViewActions.typeText("1"));
        Thread.sleep(1000);
        onView(withId(R.id.apply_button)).perform(click());
        Thread.sleep(1000);
        onView(withText("test reason 1")).check(matches(isDisplayed()));
        onView(withText("test reason 2")).check(doesNotExist());
        onView(withText("test reason 3")).check(doesNotExist());
    }

    @After
    public void tearDown() {
        clearAuthEmulator();
        clearFirestoreEmulator();
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
