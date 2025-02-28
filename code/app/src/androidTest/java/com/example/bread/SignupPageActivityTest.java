package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.bread.view.SignupPage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SignupPageActivityTest {
    @Rule
    public ActivityScenarioRule<SignupPage> scenario = new ActivityScenarioRule<>(SignupPage.class);

    @Test
    public void testLaunch() {
        scenario.getScenario().onActivity(activity -> {
            assertNotNull(activity.findViewById(R.id.signup_firstname_text));
            assertNotNull(activity.findViewById(R.id.signup_lastname_text));
            assertNotNull(activity.findViewById(R.id.signup_username_text));
            assertNotNull(activity.findViewById(R.id.signup_email_text));
            assertNotNull(activity.findViewById(R.id.signup_password_text));
            assertNotNull(activity.findViewById(R.id.signup_button));
        });
    }

    @Test
    public void testEmptyFieldsShowsErrors() {
        onView(withId(R.id.signup_button)).perform(click());
        onView(withId(R.id.signup_firstname_text)).check(matches(hasErrorText("First Name is required")));
        onView(withId(R.id.signup_lastname_text)).check(matches(hasErrorText("Last Name is required")));
        onView(withId(R.id.signup_username_text)).check(matches(hasErrorText("Username is required")));
        onView(withId(R.id.signup_email_text)).check(matches(hasErrorText("Email is required")));
        onView(withId(R.id.signup_password_text)).check(matches(hasErrorText("Password is required")));
    }
}
