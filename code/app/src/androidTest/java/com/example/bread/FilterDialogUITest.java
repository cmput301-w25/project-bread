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
import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.bread.view.HomePage;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FilterDialogUITest {

    @Rule
    public ActivityScenarioRule<HomePage> scenario = new ActivityScenarioRule<>(HomePage.class);

    @BeforeClass
    public static void testSetup() {
        FirebaseEmulatorRule.initializeEmulators();


        try {
            Tasks.await(
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword("testUser@test.com", "testPassword").addOnSuccessListener(authResult -> {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName("testUser")
                                .build();
                        Objects.requireNonNull(authResult.getUser()).updateProfile(profileUpdates);
                    })
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            Tasks.await(
                    FirebaseAuth.getInstance().signInWithEmailAndPassword("testUser@test.com", "testPassword")
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

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

    @AfterClass
    public static void clearAuthEmulator() {
        String projectId = BuildConfig.FIREBASE_PROJECT_ID;
        // This is the Auth emulator endpoint for deleting all test users
        String authUrl = "http://10.0.2.2:9099/emulator/v1/projects/"
                + projectId
                + "/accounts";

        HttpURLConnection connection = null;
        try {
            URL url = new URL(authUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            int responseCode = connection.getResponseCode();
            Log.i("tearDown", "Cleared Auth emulator users, response code: " + responseCode);
        } catch (IOException e) {
            Log.e("tearDown", "Error clearing Auth emulator", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}