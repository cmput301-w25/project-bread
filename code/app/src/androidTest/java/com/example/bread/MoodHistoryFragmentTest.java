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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodHistoryFragmentTest {

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

        DocumentReference mood1Ref = db.collection("moodEvents").document("mood1");
        Tasks.await(mood1Ref.set(mood1));

        mood1.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 1).getTime());
        mood2.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 2).getTime());
        mood3.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 3).getTime());
        mood4.setTimestamp(new GregorianCalendar(2025, Calendar.MARCH, 4).getTime());

        db.collection("moodEvents").document("mood1").set(mood1);
        db.collection("moodEvents").document("mood2").set(mood2);
        db.collection("moodEvents").document("mood3").set(mood3);
        db.collection("moodEvents").document("mood4").set(mood4);
        }

    @Test
    public void displaysAllMoodEventsTest() throws InterruptedException {
        Thread.sleep(2000);
        onView(withId(R.id.history)).perform(click());
        Thread.sleep(2000);
        onView(withText("so tired")).check(matches(isDisplayed()));
        onView(withText("coffee time")).check(matches(isDisplayed()));
        onView(withText("exam time")).check(matches(isDisplayed()));
        onView(withText("failed it")).check(matches(isDisplayed()));
    }


//    @Test
//    public void sortReverseChronologicalOrderTest() throws InterruptedException {
//        Thread.sleep(2000);
//        onData(anything())
//                .inAdapterView(withId(R.id.historyListView))
//                .atPosition(0)
//                .onChildView(withId(R.id.reason))
//                .check(matches(withText("so tired")));
//        Thread.sleep(2000);
//        onData(anything())
//                .inAdapterView(withId(R.id.historyListView))
//                .atPosition(3)
//                .onChildView(withId(R.id.reason))
//                .check(matches(withText("failed it")));
//    }

//    @Test
//    public void deleteMoodEventTest() throws InterruptedException {
//        Thread.sleep(2000);
//        onData(anything())
//                .inAdapterView(withId(R.id.historyListView))
//                .atPosition(0)
//                .onChildView(withId(R.id.checkbox))
//                .perform(click());
//        onView(withId(android.R.id.button1)).perform(click());
//        onView(withText("so tired")).check(doesNotExist());
//    }

//    @After
//    public void clearDb(){
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//    }
}
