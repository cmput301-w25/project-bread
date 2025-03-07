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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodHistoryFragmentTest {

    @BeforeClass
    public static void setup(){
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    @Rule
    public ActivityScenarioRule<HomePage> activityRule = new ActivityScenarioRule<>(HomePage.class);

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

        mood1.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 1).getTime());
        mood2.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 2).getTime());
        mood3.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 3).getTime());
        mood4.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 4).getTime());

        db.collection("moodEvents").document("mood1").set(mood1);
        db.collection("moodEvents").document("mood2").set(mood2);
        db.collection("moodEvents").document("mood3").set(mood3);
        db.collection("moodEvents").document("mood4").set(mood4);

        DocumentReference mood1Ref = db.collection("moodEvents").document("mood1");
        Tasks.await(mood1Ref.set(mood1));
        }

    @Test
    public void displaysAllMoodEventsTest() throws InterruptedException, ExecutionException {
        seedDatabase();
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);
        onView(withText("so tired")).check(matches(isDisplayed()));
        onView(withText("coffee time")).check(matches(isDisplayed()));
        onView(withText("exam time")).check(matches(isDisplayed()));
        onView(withText("failed it")).check(matches(isDisplayed()));
    }


    @Test
    public void sortReverseChronologicalOrderTest() throws InterruptedException, ExecutionException {
        seedDatabase();
        Thread.sleep(1000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(1000);
        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(0)
                .onChildView(withId(R.id.reason))
                .check(matches(withText("failed it")));
        Thread.sleep(1000);
        onData(anything())
                .inAdapterView(withId(R.id.historyListView))
                .atPosition(3)
                .onChildView(withId(R.id.reason))
                .check(matches(withText("so tired")));
    }

    @Test
    public void deleteMoodEventTest() throws InterruptedException, ExecutionException {
        seedDatabase();
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
        onView(withText("failed it")).check(doesNotExist());
    }

    @After
    public void tearDown() {
        String projectId = "project-db"; //set to your project ID
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
