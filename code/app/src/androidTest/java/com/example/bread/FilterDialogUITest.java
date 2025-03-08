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