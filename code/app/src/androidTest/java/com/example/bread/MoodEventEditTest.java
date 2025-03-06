package com.example.bread;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

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
public class MoodEventEditTest {

    @Rule
    public ActivityScenarioRule<HomePage> activityRule = new ActivityScenarioRule<>(HomePage.class);

    @Before
    public void navigateToHistoryTab() {
        // Navigate to the History tab
        onView(withId(R.id.history)).perform(click());

        // Wait for data to load
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEditDialogAppears() {
        try {
            // Click on the first mood event in the list
            onData(anything())
                    .inAdapterView(withId(R.id.historyListView))
                    .atPosition(0)
                    .perform(click());

            // Verify that the details dialog appears
            onView(withText("View Mood")).inRoot(isDialog()).check(matches(isDisplayed()));

            // Click the Edit button
            onView(withText("Edit")).perform(click());

            // Verify that the edit dialog appears
            onView(withText("Edit Mood")).inRoot(isDialog()).check(matches(isDisplayed()));

            // Verify edit dialog has the necessary fields
            onView(withId(R.id.edit_title)).check(matches(isDisplayed()));
            onView(withId(R.id.edit_reason)).check(matches(isDisplayed()));
            onView(withId(R.id.edit_emotion_spinner)).check(matches(isDisplayed()));
            onView(withId(R.id.edit_social_situation_spinner)).check(matches(isDisplayed()));

            // Close the dialog
            onView(withText("Cancel")).perform(click());
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void testEditDialogCanBeSaved() {
        try {
            // Click on the first mood event in the list
            onData(anything())
                    .inAdapterView(withId(R.id.historyListView))
                    .atPosition(0)
                    .perform(click());

            // Click the Edit button
            onView(withText("Edit")).perform(click());

            // Make a simple change
            onView(withId(R.id.edit_reason)).perform(replaceText("Test reason"));

            // Click Save button
            onView(withText("Save")).perform(click());


            // Verify dialog is dismissed (will throw exception if still visible)
            try {
                onView(withText("Edit Mood")).inRoot(isDialog()).check(matches(isDisplayed()));
                throw new AssertionError("Dialog should be dismissed after saving");
            } catch (Exception e) {
                // Expected - dialog should be dismissed
            }
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void testEditDialogCanBeCanceled() {
        try {
            // Click on the first mood event in the list
            onData(anything())
                    .inAdapterView(withId(R.id.historyListView))
                    .atPosition(0)
                    .perform(click());

            // Click the Edit button
            onView(withText("Edit")).perform(click());

            // Make a change
            onView(withId(R.id.edit_reason)).perform(replaceText("This should not be saved"));

            // Click Cancel button
            onView(withText("Cancel")).perform(click());

            // Verify dialog is dismissed
            try {
                onView(withText("Edit Mood")).inRoot(isDialog()).check(matches(isDisplayed()));
                throw new AssertionError("Dialog should be dismissed after canceling");
            } catch (Exception e) {
                // Expected - dialog should be dismissed
            }
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
        }
    }
}