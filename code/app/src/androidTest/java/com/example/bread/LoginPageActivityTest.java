package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;

import android.Manifest;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.bread.view.LoginPage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoginPageActivityTest {
    @Rule
    public ActivityScenarioRule<LoginPage> scenario = new ActivityScenarioRule<>(LoginPage.class);

    private void grantPermission() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "pm grant " + InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName() + " " + Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @Test
    public void testLaunch() {
        scenario.getScenario().onActivity(activity -> {
            assertNotNull(activity.findViewById(R.id.login_email_text));
            assertNotNull(activity.findViewById(R.id.login_password_text));
            assertNotNull(activity.findViewById(R.id.login_button));
        });
    }

    @Test
    public void testEmptyFieldsShowsErrors() {
        grantPermission();
        onView(withId(R.id.login_password_text)).perform(typeText("Password"), closeSoftKeyboard());
        onView(withId(R.id.login_button)).perform(click());
        onView(withId(R.id.login_email_text)).check(matches(hasErrorText("Email is required")));

        onView(withId(R.id.login_password_text)).perform(clearText());
        onView(withId(R.id.login_email_text)).perform(typeText("email@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.login_button)).perform(click());
        onView(withId(R.id.login_password_text)).check(matches(hasErrorText("Password is required")));
    }
}
