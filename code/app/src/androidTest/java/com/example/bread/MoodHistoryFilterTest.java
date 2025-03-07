package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.bread.view.HomePage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodHistoryFilterTest {

    @Rule
    public ActivityScenarioRule<HomePage> activityRule = new ActivityScenarioRule<>(HomePage.class);

    @Before
    public void navigateToHistoryTab() {
        // Navigate to the History tab
        onView(withId(R.id.history)).perform(click());

        // Wait for data to load (consider using IdlingResource in production tests)
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //test for loading events properly

    //test for filtering with nothing

    //test for filtering by most recent week

    //test for filtering by mood state

    //test for filtering by keyword reason

    //test for filtering by all three

    //test for filtering for one thing then another
}
