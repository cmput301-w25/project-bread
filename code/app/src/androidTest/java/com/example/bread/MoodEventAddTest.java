package com.example.bread;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onIdle;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import static java.util.EnumSet.allOf;

import android.os.IBinder;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.Root;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.view.HomePage;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodEventAddTest {

    @Rule
    public ActivityScenarioRule<HomePage> activityRule = new ActivityScenarioRule<>(HomePage.class);

    @Before
    public void navigateToAddTab() {
        // Navigate to the "add" tab
        onView(withId(R.id.add)).perform(click());
        onIdle(); // Wait for UI to settle
    }

    @Test
    public void testUiElementsAreDisplayed() {
        // Verify that key UI elements are displayed
        onView(withId(R.id.emotionalStateSpinner)).check(matches(isDisplayed()));
        onView(withId(R.id.eventTitleEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.reasonEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.triggerEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.socialSituationSpinner)).check(matches(isDisplayed()));
        onView(withId(R.id.locationCheckbox)).check(matches(isDisplayed()));
        onView(withId(R.id.saveButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyTitleValidation() throws InterruptedException {

        onView(withId(R.id.saveButton)).perform(click());

        onView(withId(R.id.eventTitleEditText)).check(matches(hasErrorText("Event title cannot be empty")));

    }

    @Test
    public void testReasonLengthValidation() {
        // Add a valid event title
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Test Event"));

        // Select a valid emotional state
        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        onData(is(MoodEvent.EmotionalState.HAPPY)).perform(click());

        // Add a reason that is too long (> 20 chars)
        onView(withId(R.id.reasonEditText)).perform(replaceText("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));

        // Click save button
        onView(withId(R.id.saveButton)).perform(click());

        // Verify error is shown
        onView(withId(R.id.reasonEditText)).check(matches(hasErrorText(containsString("Reason must be 20 characters or fewer and 3 words or fewer"))));
    }

    @Test
    public void testReasonWordCountValidation() {
        // Add a valid event title
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Test Event"));

        // Select a valid emotional state
        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        onData(is(MoodEvent.EmotionalState.HAPPY)).perform(click());

        // Add a reason with too many words (> 3 words)
        onView(withId(R.id.reasonEditText)).perform(replaceText("Typing a lot of words"));

        // Click save button
        onView(withId(R.id.saveButton)).perform(click());

        // Verify error is shown
        onView(withId(R.id.reasonEditText)).check(matches(hasErrorText(containsString("Reason must be 20 characters or fewer and 3 words or fewer"))));
    }

    @Test
    public void testNoneEmotionalStateValidation() throws InterruptedException {

        // Enter valid title
        onView(withId(R.id.eventTitleEditText)).perform(replaceText("Test Event"));


        // Enter valid reason (within limits)
        onView(withId(R.id.reasonEditText)).perform(replaceText("Some Reason"));


        onView(withId(R.id.triggerEditText)).perform(replaceText("Some Trigger"));
        onView(withId(R.id.socialSituationSpinner)).perform(click());
        onData(is(MoodEvent.SocialSituation.ALONE)).perform(click());
        // Click save
        onView(withId(R.id.saveButton)).perform(click());
        Thread.sleep(3000);
        onView(withId(R.id.eventTitleEditText)).check(matches(isDisplayed()));

    }



}