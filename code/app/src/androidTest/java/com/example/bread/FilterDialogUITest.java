package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.Manifest;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.bread.view.HomePage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FilterDialogUITest {

    @Rule
    public ActivityScenarioRule<HomePage> scenario = new ActivityScenarioRule<>(HomePage.class);

    @Before
    public void setup() {
        // Grant location permission
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "pm grant " + InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName() + " " + Manifest.permission.ACCESS_FINE_LOCATION);

        // Navigate to History tab
        onView(withId(R.id.history)).perform(click());

        // Wait for the fragment to load
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFilterButtonOpensDialog() {
        // Click the filter button
        onView(withId(R.id.filter_button)).perform(click());

        // Verify that the dialog appears with expected elements
        onView(withText("Filter Mood Events")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withId(R.id.recent_week_switch)).check(matches(isDisplayed()));
        onView(withId(R.id.mood_spinner)).check(matches(isDisplayed()));
        onView(withId(R.id.keyword_edit_text)).check(matches(isDisplayed()));
        onView(withId(R.id.apply_button)).check(matches(isDisplayed()));
        onView(withId(R.id.reset_button)).check(matches(isDisplayed()));
    }

    @Test
    public void testKeywordFilter() {
        // Open filter dialog
        onView(withId(R.id.filter_button)).perform(click());

        // Enter a keyword
        onView(withId(R.id.keyword_edit_text)).perform(typeText("test"), closeSoftKeyboard());

        // Click apply
        onView(withId(R.id.apply_button)).perform(click());

        // Dialog should be dismissed after clicking apply
        try {
            Thread.sleep(500);
            // This just verifies the dialog closed successfully
            onView(withText("Filter Mood Events")).check(matches(isDisplayed()));
            throw new AssertionError("Dialog should be dismissed");
        } catch (Exception e) {
            // Expected - dialog should be dismissed
        }
    }

    @Test
    public void testResetButton() {
        // Open filter dialog
        onView(withId(R.id.filter_button)).perform(click());

        // Enter a keyword (to verify it gets cleared)
        onView(withId(R.id.keyword_edit_text)).perform(typeText("test"), closeSoftKeyboard());

        // Click reset button
        onView(withId(R.id.reset_button)).perform(click());

        // Dialog should be dismissed after clicking reset
        try {
            Thread.sleep(500);
            onView(withText("Filter Mood Events")).check(matches(isDisplayed()));
            throw new AssertionError("Dialog should be dismissed");
        } catch (Exception e) {
            // Expected - dialog should be dismissed
        }
    }
}

//package com.example.bread;
//
//import static androidx.test.espresso.Espresso.onView;
//import static androidx.test.espresso.action.ViewActions.click;
//import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
//import static androidx.test.espresso.assertion.ViewAssertions.matches;
//import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
//import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
//import static androidx.test.espresso.matcher.ViewMatchers.withId;
//import static androidx.test.espresso.matcher.ViewMatchers.withText;
//
//import android.util.Log;
//
//import androidx.test.espresso.action.ViewActions;
//import androidx.test.ext.junit.rules.ActivityScenarioRule;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//import androidx.test.filters.LargeTest;
//import com.example.bread.model.MoodEvent;
//import com.example.bread.view.HomePage;
//import com.google.android.gms.tasks.Tasks;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.UserProfileChangeRequest;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.FirebaseFirestore;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import java.io.IOException;
//import java.net.HttpURLConnection;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.ExecutionException;
//
//@RunWith(AndroidJUnit4.class)
//@LargeTest
//public class FilterDialogUITest{
//
//    Date moodDate1;
//    Date moodDate2;
//    Date moodDate3;
//    Date moodDate4;
//
//    @BeforeClass
//    public static void testSetup() {
//        String androidLocalHost = "10.0.2.2";
//        FirebaseFirestore.getInstance().useEmulator(androidLocalHost, 8080);
//        FirebaseAuth.getInstance().useEmulator(androidLocalHost, 9099);
//
//        try {
//            Tasks.await(
//                    FirebaseAuth.getInstance().createUserWithEmailAndPassword("testUser@test.com", "testPassword").addOnSuccessListener(authResult -> {
//                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
//                                .setDisplayName("testUser")
//                                .build();
//                        Objects.requireNonNull(authResult.getUser()).updateProfile(profileUpdates);
//                    })
//            );
//        } catch (ExecutionException | InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        try {
//            Tasks.await(
//                    FirebaseAuth.getInstance().signInWithEmailAndPassword("testUser@test.com", "testPassword")
//            );
//        } catch (ExecutionException | InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Rule
//    public ActivityScenarioRule<HomePage> activityRule = new ActivityScenarioRule<>(HomePage.class);
//
//    //https://stackoverflow.com/questions/17210839/get-last-week-date-range-for-a-date-in-java
//    @Before
//    public void setDates(){
//        moodDate1 = new Date();
//        moodDate2 = new Date();
//        moodDate3 = new Date();
//        moodDate4 = new Date();
//
//        Calendar c = Calendar.getInstance(); //sets calendar to today
//
//        c.setTime(moodDate1);
//        moodDate1 = c.getTime();
//
//        c.add(Calendar.DATE, -8); //one week before current date
//        moodDate2 = c.getTime();
//
//        c.add(Calendar.DATE, -9); //8 days before current date
//        moodDate3 = c.getTime();
//
//        c.add(Calendar.DATE, -10); //9 days before current date
//        moodDate4 = c.getTime();
//    }
//
//    //https://firebase.google.com/docs/firestore/manage-data/add-data
//    @Before
//    public void seedDatabase() throws ExecutionException, InterruptedException {
//        //creating firebase instance
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        Map<String, Object> user = new HashMap<>();
//
//        user.put("username", "testUser");
//        DocumentReference participantRef = db.collection("participants").document("testUser");
//        participantRef.set(user);
//
//        Tasks.await(participantRef.set(user));
//
//        MoodEvent mood1 = new MoodEvent("mood1", "so tired", MoodEvent.EmotionalState.SAD, participantRef);
//        MoodEvent mood2 = new MoodEvent("mood2", "coffee time", MoodEvent.EmotionalState.HAPPY, participantRef);
//        MoodEvent mood3 = new MoodEvent("mood3", "exam time", MoodEvent.EmotionalState.ANXIOUS, participantRef);
//        MoodEvent mood4 = new MoodEvent("mood4", "failed it", MoodEvent.EmotionalState.SHAMEFUL, participantRef);
//
//        mood1.setTimestamp(moodDate1);
//        mood2.setTimestamp(moodDate2);
//        mood3.setTimestamp(moodDate3);
//        mood4.setTimestamp(moodDate4);
//
//        db.collection("moodEvents").document("mood1").set(mood1);
//        db.collection("moodEvents").document("mood2").set(mood2);
//        db.collection("moodEvents").document("mood3").set(mood3);
//        db.collection("moodEvents").document("mood4").set(mood4);
//    }
//
//    //test for loading events properly
//    @Test
//    public void loadMoodEventHistoryTest() throws ExecutionException, InterruptedException {
//        Thread.sleep(1000);
//        onView(withId(R.id.history)).perform(click());
//        Thread.sleep(1000);
//        onView(withText("so tired")).check(matches(isDisplayed()));
//        onView(withText("coffee time")).check(matches(isDisplayed()));
//        onView(withText("exam time")).check(matches(isDisplayed()));
//        onView(withText("failed it")).check(matches(isDisplayed()));
//    }
//
//    //test for filtering with nothing
//    @Test
//    public void filterByNothingTest() throws ExecutionException, InterruptedException {
//        Thread.sleep(1000);
//        onView(withId(R.id.history)).perform(click());
//        Thread.sleep(1000);
//
//        onView(withId(R.id.filter_button)).perform(click());
//        Thread.sleep(1000);
//        onView(withId(android.R.id.button1)).perform(click());
//
//        Thread.sleep(1000);
//        onView(withText("so tired")).check(matches(isDisplayed()));
//        onView(withText("coffee time")).check(matches(isDisplayed()));
//        onView(withText("exam time")).check(matches(isDisplayed()));
//        onView(withText("failed it")).check(matches(isDisplayed()));
//    }
//
//    //test for filtering by most recent week
//    @Test
//    public void filterByMostRecentWeekTest() throws ExecutionException, InterruptedException {
//        Thread.sleep(1000);
//        onView(withId(R.id.history)).perform(click());
//        Thread.sleep(1000);
//
//        onView(withId(R.id.filter_button)).perform(click());
//        Thread.sleep(1000);
//        onView(withId(R.id.recent_week_switch)).perform(click());
//        Thread.sleep(1000);
//        onView(withId(android.R.id.button1)).perform(click());
//
//        Thread.sleep(1000);
//        onView(withText("so tired")).check(matches(isDisplayed()));
//        onView(withText("coffee time")).check(doesNotExist());
//        onView(withText("exam time")).check(doesNotExist());
//        onView(withText("failed it")).check(doesNotExist());
//    }
//
//    //    test for filtering by mood state
//    @Test
//    public void filterByMoodStateTest() throws ExecutionException, InterruptedException {
//        Thread.sleep(1000);
//        onView(withId(R.id.history)).perform(click());
//        Thread.sleep(1000);
//
//        onView(withId(R.id.filter_button)).perform(click());
//        Thread.sleep(1000);
//
//        onView(withId(R.id.mood_spinner)).perform(click());
//        Thread.sleep(1000);
//
//        onView(withText("HAPPY")) //searches for "HAPPY" state within spinner
//                .inRoot(isPlatformPopup()) //ensure we look in the popup filter window not main screen
//                .perform(click());
//
//        onView(withId(android.R.id.button1)).perform(click());
//
//        Thread.sleep(1000);
//        onView(withText("so tired")).check(doesNotExist());
//        onView(withText("coffee time")).check(matches(isDisplayed()));
//        onView(withText("exam time")).check(doesNotExist());
//        onView(withText("failed it")).check(doesNotExist());
//    }
//
//    //    test for filtering by keyword reason
//    @Test
//    public void filterByReasonKeywordTest() throws ExecutionException, InterruptedException {
//        Thread.sleep(1000);
//        onView(withId(R.id.history)).perform(click());
//        Thread.sleep(1000);
//
//        onView(withId(R.id.filter_button)).perform(click());
//        Thread.sleep(1000);
//
//        onView(withId(R.id.keyword_edit_text)).perform(ViewActions.typeText("exam"));
//        Thread.sleep(1000);
//        onView(withId(android.R.id.button1)).perform(click());
//
//        Thread.sleep(1000);
//        onView(withText("so tired")).check(doesNotExist());
//        onView(withText("coffee time")).check(doesNotExist());
//        onView(withText("exam time")).check(matches(isDisplayed()));
//        onView(withText("failed it")).check(doesNotExist());
//    }
//
//    //test for filtering by all three
//    @Test
//    public void filterByAllThreeFilters() throws ExecutionException, InterruptedException {
//        Thread.sleep(1000);
//        onView(withId(R.id.history)).perform(click());
//        Thread.sleep(1000);
//
//        //filtering by recent week
//        onView(withId(R.id.filter_button)).perform(click());
//        Thread.sleep(1000);
//        onView(withId(R.id.recent_week_switch)).perform(click());
//        Thread.sleep(1000);
//
//        //filtering by dropdown
//        onView(withId(R.id.mood_spinner)).perform(click());
//        Thread.sleep(1000);
//        onView(withText("SAD")) //searches for "HAPPY" state within spinner
//                .inRoot(isPlatformPopup()) //ensure we look in the popup filter window not main screen
//                .perform(click());
//
//        //filtering by keyword
//        onView(withId(R.id.keyword_edit_text)).perform(ViewActions.typeText("tire"));
//        Thread.sleep(1000);
//        onView(withId(android.R.id.button1)).perform(click());
//
//        Thread.sleep(1000);
//        onView(withText("so tired")).check(matches(isDisplayed()));
//        onView(withText("coffee time")).check(doesNotExist());
//        onView(withText("exam time")).check(doesNotExist());
//        onView(withText("failed it")).check(doesNotExist());
//    }
//
//    //test for filtering for one thing then another
//    @Test
//    public void filterByMultipleFiltersTest() throws ExecutionException, InterruptedException {
//        Thread.sleep(1000);
//        onView(withId(R.id.history)).perform(click());
//        Thread.sleep(1000);
//
//        //filtering by most recent week first
//        onView(withId(R.id.filter_button)).perform(click());
//        Thread.sleep(1000);
//        onView(withId(R.id.recent_week_switch)).perform(click());
//        Thread.sleep(1000);
//        onView(withId(android.R.id.button1)).perform(click());
//        Thread.sleep(1000);
//        onView(withText("so tired")).check(matches(isDisplayed()));
//        onView(withText("coffee time")).check(doesNotExist());
//        onView(withText("exam time")).check(doesNotExist());
//        onView(withText("failed it")).check(doesNotExist());
//
//        //unselect most recent week
//        onView(withId(R.id.filter_button)).perform(click());
//        Thread.sleep(1000);
//        onView(withId(R.id.recent_week_switch)).perform(click());
//        Thread.sleep(1000);
//        onView(withId(android.R.id.button1)).perform(click());
//        Thread.sleep(1000);
//        onView(withText("so tired")).check(matches(isDisplayed()));
//        onView(withText("coffee time")).check(matches(isDisplayed()));
//        onView(withText("exam time")).check(matches(isDisplayed()));
//        onView(withText("failed it")).check(matches(isDisplayed()));
//
//        //keyword search test
//        onView(withId(R.id.filter_button)).perform(click());
//        Thread.sleep(1000);
//        onView(withId(R.id.keyword_edit_text)).perform(ViewActions.typeText("coffee"));
//        Thread.sleep(1000);
//        onView(withId(android.R.id.button1)).perform(click());
//        Thread.sleep(1000);
//        onView(withText("so tired")).check(doesNotExist());
//        onView(withText("coffee time")).check(matches(isDisplayed()));
//        onView(withText("exam time")).check(doesNotExist());
//        onView(withText("failed it")).check(doesNotExist());
//    }
//
//    @After
//    public void tearDown() {
//        String projectId = "project-db"; //!set to your project ID!
//        URL url = null;
//        try {
//            url = new URL("http://10.0.2.2:8080/emulator/v1/projects/+"+projectId+"/databases/%28default%29/documents");
//        } catch (MalformedURLException exception) {
//            Log.e("URL Error", Objects.requireNonNull(exception.getMessage()));
//        }
//        HttpURLConnection urlConnection = null;
//        try {
//            urlConnection = (HttpURLConnection) url.openConnection();
//            urlConnection.setRequestMethod("DELETE");
//            int response = urlConnection.getResponseCode();
//            Log.i("Response Code", "Response Code: " + response);
//        } catch (IOException exception) {
//            Log.e("IO Error", Objects.requireNonNull(exception.getMessage()));
//        } finally {
//            if (urlConnection != null) {
//                urlConnection.disconnect();
//            }
//        }
//    }
//}
