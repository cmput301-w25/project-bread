package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.util.Log;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import com.example.bread.model.MoodEvent;
import com.example.bread.view.HomePage;
import com.google.android.gms.tasks.Tasks;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodHistoryFilterTest{

    Date moodDate1;
    Date moodDate2;
    Date moodDate3;
    Date moodDate4;

    @BeforeClass
    public static void setup(){
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    @Rule
    public ActivityScenarioRule<HomePage> activityRule = new ActivityScenarioRule<>(HomePage.class);

    //https://stackoverflow.com/questions/17210839/get-last-week-date-range-for-a-date-in-java
    @Before
    public void setDates(){
        moodDate1 = new Date();
        moodDate2 = new Date();
        moodDate3 = new Date();
        moodDate4 = new Date();

        Calendar c = Calendar.getInstance(); //sets calendar to today

        c.setTime(moodDate1);
        moodDate1 = c.getTime();

        c.add(Calendar.DATE, -8); //one week before current date
        moodDate2 = c.getTime();

        c.add(Calendar.DATE, -9); //8 days before current date
        moodDate3 = c.getTime();

        c.add(Calendar.DATE, -10); //9 days before current date
        moodDate4 = c.getTime();
    }

    //https://firebase.google.com/docs/firestore/manage-data/add-data
    @Before
    public void seedDatabase() throws ExecutionException, InterruptedException {
        //creating firebase instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();

        user.put("username", "testUser");
        DocumentReference participantRef = db.collection("participants").document("testUser");
        participantRef.set(user);

        Tasks.await(participantRef.set(user));

        MoodEvent mood1 = new MoodEvent("mood1", "so tired", MoodEvent.EmotionalState.SAD, participantRef);
        MoodEvent mood2 = new MoodEvent("mood2", "coffee time", MoodEvent.EmotionalState.HAPPY, participantRef);
        MoodEvent mood3 = new MoodEvent("mood3", "exam time", MoodEvent.EmotionalState.ANXIOUS, participantRef);
        MoodEvent mood4 = new MoodEvent("mood4", "failed it", MoodEvent.EmotionalState.SHAMEFUL, participantRef);

        mood1.setTimestamp(moodDate1);
        mood2.setTimestamp(moodDate2);
        mood3.setTimestamp(moodDate3);
        mood4.setTimestamp(moodDate4);

        db.collection("moodEvents").document("mood1").set(mood1);
        db.collection("moodEvents").document("mood2").set(mood2);
        db.collection("moodEvents").document("mood3").set(mood3);
        db.collection("moodEvents").document("mood4").set(mood4);
    }

    //test for loading events properly
    @Test
    public void loadMoodEventHistoryTest() throws ExecutionException, InterruptedException {
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);
        onView(withText("so tired")).check(matches(isDisplayed()));
        onView(withText("coffee time")).check(matches(isDisplayed()));
        onView(withText("exam time")).check(matches(isDisplayed()));
        onView(withText("failed it")).check(matches(isDisplayed()));
    }

    //test for filtering with nothing
    @Test
    public void filterByNothingTest() throws ExecutionException, InterruptedException {
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.filterButton)).perform(click());
        Thread.sleep(1000);
        onView(withId(android.R.id.button1)).perform(click());

        Thread.sleep(1000);
        onView(withText("so tired")).check(matches(isDisplayed()));
        onView(withText("coffee time")).check(matches(isDisplayed()));
        onView(withText("exam time")).check(matches(isDisplayed()));
        onView(withText("failed it")).check(matches(isDisplayed()));
    }

    //test for filtering by most recent week
    @Test
    public void filterByMostRecentWeekTest() throws ExecutionException, InterruptedException {
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.filterButton)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.mostRecentWeekButton)).perform(click());
        Thread.sleep(1000);
        onView(withId(android.R.id.button1)).perform(click());

        Thread.sleep(1000);
        onView(withText("so tired")).check(matches(isDisplayed()));
        onView(withText("coffee time")).check(doesNotExist());
        onView(withText("exam time")).check(doesNotExist());
        onView(withText("failed it")).check(doesNotExist());
    }

//    test for filtering by mood state
    @Test
    public void filterByMoodStateTest() throws ExecutionException, InterruptedException {
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.filterButton)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.moodDropdown)).perform(click());
        Thread.sleep(1000);

        onView(withText("HAPPY")) //searches for "HAPPY" state within spinner
                .inRoot(isPlatformPopup()) //ensure we look in the popup filter window not main screen
                .perform(click());

        onView(withId(android.R.id.button1)).perform(click());

        Thread.sleep(1000);
        onView(withText("so tired")).check(doesNotExist());
        onView(withText("coffee time")).check(matches(isDisplayed()));
        onView(withText("exam time")).check(doesNotExist());
        onView(withText("failed it")).check(doesNotExist());
    }

//    test for filtering by keyword reason
    @Test
    public void filterByReasonKeywordTest() throws ExecutionException, InterruptedException {
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.filterButton)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.reasonKeywordEdit)).perform(ViewActions.typeText("exam"));
        Thread.sleep(1000);
        onView(withId(android.R.id.button1)).perform(click());

        Thread.sleep(1000);
        onView(withText("so tired")).check(doesNotExist());
        onView(withText("coffee time")).check(doesNotExist());
        onView(withText("exam time")).check(matches(isDisplayed()));
        onView(withText("failed it")).check(doesNotExist());
    }

    //test for filtering by all three
    @Test
    public void filterByAllThreeFilters() throws ExecutionException, InterruptedException {
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);

        //filtering by recent week
        onView(withId(R.id.filterButton)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.mostRecentWeekButton)).perform(click());
        Thread.sleep(1000);

        //filtering by dropdown
        onView(withId(R.id.moodDropdown)).perform(click());
        Thread.sleep(1000);
        onView(withText("SAD")) //searches for "HAPPY" state within spinner
                .inRoot(isPlatformPopup()) //ensure we look in the popup filter window not main screen
                .perform(click());

        //filtering by keyword
        onView(withId(R.id.reasonKeywordEdit)).perform(ViewActions.typeText("tire"));
        Thread.sleep(1000);
        onView(withId(android.R.id.button1)).perform(click());

        Thread.sleep(1000);
        onView(withText("so tired")).check(matches(isDisplayed()));
        onView(withText("coffee time")).check(doesNotExist());
        onView(withText("exam time")).check(doesNotExist());
        onView(withText("failed it")).check(doesNotExist());
    }

    //test for filtering for one thing then another
    @Test
    public void filterByMultipleFiltersTest() throws ExecutionException, InterruptedException {
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);

        //filtering by most recent week first
        onView(withId(R.id.filterButton)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.mostRecentWeekButton)).perform(click());
        Thread.sleep(1000);
        onView(withId(android.R.id.button1)).perform(click());
        Thread.sleep(1000);
        onView(withText("so tired")).check(matches(isDisplayed()));
        onView(withText("coffee time")).check(doesNotExist());
        onView(withText("exam time")).check(doesNotExist());
        onView(withText("failed it")).check(doesNotExist());

        //unselect most recent week
        onView(withId(R.id.filterButton)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.mostRecentWeekButton)).perform(click());
        Thread.sleep(1000);
        onView(withId(android.R.id.button1)).perform(click());
        Thread.sleep(1000);
        onView(withText("so tired")).check(matches(isDisplayed()));
        onView(withText("coffee time")).check(matches(isDisplayed()));
        onView(withText("exam time")).check(matches(isDisplayed()));
        onView(withText("failed it")).check(matches(isDisplayed()));

        //keyword search test
        onView(withId(R.id.filterButton)).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.reasonKeywordEdit)).perform(ViewActions.typeText("coffee"));
        Thread.sleep(1000);
        onView(withId(android.R.id.button1)).perform(click());
        Thread.sleep(1000);
        onView(withText("so tired")).check(doesNotExist());
        onView(withText("coffee time")).check(matches(isDisplayed()));
        onView(withText("exam time")).check(doesNotExist());
        onView(withText("failed it")).check(doesNotExist());
    }

    @After
    public void tearDown() {
        String projectId = "project-db"; //!set to your project ID!
        URL url = null;
        try {
            url = new URL("http://10.0.2.2:8080/emulator/v1/projects/+"+projectId+"/databases/%28default%29/documents");
        } catch (MalformedURLException exception) {
            Log.e("URL Error", Objects.requireNonNull(exception.getMessage()));
        }
        HttpURLConnection urlConnection = null;
        try {
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
