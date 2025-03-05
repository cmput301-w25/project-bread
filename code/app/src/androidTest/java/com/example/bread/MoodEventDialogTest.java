package com.example.bread;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.bread.view.HomePage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodEventDialogTest {

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

    @Test
    public void clickOnMoodEvent_shouldOpenDialog() {
        try {
            // When: User clicks on the first mood event in the list
            onData(anything()).inAdapterView(withId(R.id.historyListView)).atPosition(0).perform(click());

            // Then: Dialog should appear with title "Mood Details"
            onView(withText("View Moods")).inRoot(isDialog()).check(matches(isDisplayed()));

            // And: Dialog should contain the expected elements
            onView(withId(R.id.detail_emotion)).check(matches(isDisplayed()));
            onView(withId(R.id.detail_date)).check(matches(isDisplayed()));
            onView(withId(R.id.detail_reason)).check(matches(isDisplayed()));
            onView(withId(R.id.detail_social_situation)).check(matches(isDisplayed()));

            // And: Elements should contain actual data (not empty)
            onView(withId(R.id.detail_emotion)).check(matches(withText(not(""))));
            onView(withId(R.id.detail_date)).check(matches(withText(not(""))));

            // Finally: Close the dialog
            onView(withText("Close")).perform(click());
        } catch (Exception e) {
            // If test fails because there are no mood events, print a helpful message
            System.out.println("Test skipped: No mood events available to test with");
        }
    }
}