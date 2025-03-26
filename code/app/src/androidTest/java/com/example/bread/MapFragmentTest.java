package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.view.HomePage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;
import androidx.test.platform.app.InstrumentationRegistry;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapFragmentTest {

    //https://www.browserstack.com/guide/test-toast-message-using-espresso
    private View decorView;

    @Rule
    public ActivityScenarioRule<HomePage> activityScenarioRule = new ActivityScenarioRule<>(HomePage.class);

    @Before
    public void setup() {
        activityScenarioRule.getScenario().onActivity(activity -> {
            decorView = activity.getWindow().getDecorView();
        });
    }

    @BeforeClass
    public static void testSetup() {
        String androidLocalHost = "10.0.2.2";
        FirebaseFirestore.getInstance().useEmulator(androidLocalHost, 8080);
        FirebaseAuth.getInstance().useEmulator(androidLocalHost, 9099);

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
    public void seedDatabase() {

        // Seed the database with some mood events
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference participants = db.collection("participants");
        Participant p1 = new Participant();
        p1.setUsername("testUser");

        DocumentReference p1Ref = participants.document("testUser");
        p1Ref.set(p1);
        p1Ref.collection("following").document("testUser2").set(new HashMap<>() {
            {
                put("username", "testUser2");
            }
        });

        Participant p2 = new Participant();
        p2.setUsername("testUser2");

        DocumentReference p2Ref = participants.document("testUser2");
        p2Ref.set(p2);
        p2Ref.collection("followers").document("testUser").set(new HashMap<>() {
            {
                put("username", "testUser");
            }
        });

        double latitude1 = 37.43;
        double longitude1 = -122.083922;
        String geohash1 = "9q9hvumpqr";
        Map<String, Object> geoInfo1 = new HashMap<>();
        geoInfo1.put("latitude", latitude1);
        geoInfo1.put("longitude", longitude1);
        geoInfo1.put("geohash", geohash1);
        MoodEvent moodEvent1 = new MoodEvent("Test Event 1", "test reason 1", MoodEvent.EmotionalState.HAPPY, p1Ref);
        moodEvent1.setSocialSituation(MoodEvent.SocialSituation.ALONE);
        moodEvent1.setGeoInfo(geoInfo1);

        double latitude2 = 37.4219999;
        double longitude2 = -122.0840575;
        String geohash2 = "9q9hvumpqr";
        Map<String, Object> geoInfo2 = new HashMap<>();
        geoInfo2.put("latitude", latitude2);
        geoInfo2.put("longitude", longitude2);
        geoInfo2.put("geohash", geohash2);
        MoodEvent moodEvent2 = new MoodEvent("Test Event 2", "test reason 2", MoodEvent.EmotionalState.SAD, p2Ref);
        moodEvent2.setSocialSituation(MoodEvent.SocialSituation.ALONE);
        moodEvent2.setGeoInfo(geoInfo2);

        CollectionReference moodEvents = db.collection("moodEvents");
        MoodEvent[] events = {
                moodEvent1, moodEvent2
        };

        for (MoodEvent event : events) {
            moodEvents.document(event.getId()).set(event);
        }
    }

    //https://developer.android.com/training/testing/other-components/ui-automator
    @Test
    public void testMarkerAppearsAndNoFilter() throws Exception {
        Thread.sleep(1000);
        grantPermission();
        onView(withId(R.id.map)).perform(click());
        Thread.sleep(1000);

        // Creating a mock location to set as test location so the map centers on this
        Context context = ApplicationProvider.getApplicationContext();
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
        Tasks.await(fusedClient.setMockMode(true));
        Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
        mockLocation.setLatitude(37.4219999);
        mockLocation.setLongitude(-122.0840575);
        mockLocation.setAccuracy(1.0f);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        Tasks.await(fusedClient.setMockLocation(mockLocation));

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Having user select "While using app" when location permission request appears
        UiObject allowButton = device.findObject(new UiSelector().textContains("While using the app"));
        if (allowButton.exists()) {
            allowButton.click();
        }

        Thread.sleep(5000);

        // Turn personal mood history viewing on
        onView(withId(R.id.history_switch)).perform(click());

        Thread.sleep(1000);

        UiObject marker1 = device.findObject(new UiSelector().descriptionContains("@testUser: Happy"));
        assertTrue("Personal marker should be visible", marker1.exists());

        //Turn personal mood history viewing off and following on
        onView(withId(R.id.filter_button)).perform(click());
        onView(withId(R.id.history_switch)).perform(click());
        onView(withId(R.id.follower_switch)).perform(click());
        onView(withId(R.id.apply_button)).perform(click());

        UiObject marker2 = device.findObject(new UiSelector().descriptionContains("@testUser2: Sad"));
        assertTrue("Follower marker should be visible", marker2.exists());
    }

    @Test
    public void testRecentReasonMoodFilter() throws Exception{
        Thread.sleep(3000);
        grantPermission();
        onView(withId(R.id.map)).perform(click());
        Thread.sleep(3000);

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Having user select "While using app" when location permission request appears
        UiObject allowButton = device.findObject(new UiSelector().textContains("While using the app"));
        if (allowButton.exists()) {
            allowButton.click();
        }

        Thread.sleep(1000);

        // Creating a mock location to set as test location so the map centers on this
        Context context = ApplicationProvider.getApplicationContext();
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
        Tasks.await(fusedClient.setMockMode(true));
        Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
        mockLocation.setLatitude(37.4219999);
        mockLocation.setLongitude(-122.0840575);
        mockLocation.setAccuracy(1.0f);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        Tasks.await(fusedClient.setMockLocation(mockLocation));

        Thread.sleep(1000);

        onView(withId(R.id.history_switch)).perform(click());
        onView(withId(R.id.follower_switch)).perform(click());

        // Selecting most recent week
        onView(withId(R.id.recent_week_switch)).perform(click());

        // Selecting happy mood
        onView(withId(R.id.mood_spinner)).perform(click());
        onView(withText("Happy")) // Searches for "Happy" state within spinner
                .inRoot(isPlatformPopup()) // Ensure we look in the popup filter window not main screen
                .perform(click());
        onView(withId(R.id.apply_button)).perform(click());

        // Selecting mood reason
        onView(withId(R.id.reason_text)).perform(replaceText("test reason 1"));

        // Ensuring the mood that fits all criteria appears
        UiObject marker1 = device.findObject(new UiSelector().descriptionContains("@testUser: Happy"));
        assertTrue("Marker should be visible", marker1.exists());

        // Ensuring toast appears
        onView(withText("No follower mood events match the applied filters")).inRoot(withDecorView(Matchers.not(decorView)));
    }

    @Test
    public void testResetFilters() throws Exception{
        Thread.sleep(3000);
        grantPermission();
        onView(withId(R.id.map)).perform(click());
        Thread.sleep(3000);

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Having user select "While using app" when location permission request appears
        UiObject allowButton = device.findObject(new UiSelector().textContains("While using the app"));
        if (allowButton.exists()) {
            allowButton.click();
        }

        Thread.sleep(1000);

        // Creating a mock location to set as test location so the map centers on this
        Context context = ApplicationProvider.getApplicationContext();
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
        Tasks.await(fusedClient.setMockMode(true));
        Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
        mockLocation.setLatitude(37.4219999);
        mockLocation.setLongitude(-122.0840575);
        mockLocation.setAccuracy(1.0f);
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        Tasks.await(fusedClient.setMockLocation(mockLocation));

        Thread.sleep(1000);

        onView(withId(R.id.history_switch)).perform(click());
        onView(withId(R.id.follower_switch)).perform(click());

        // Selecting sad mood so only mood 2 appears
        onView(withId(R.id.mood_spinner)).perform(click());
        onView(withText("Happy")) // Searches for "Happy" state within spinner
                .inRoot(isPlatformPopup()) // Ensure we look in the popup filter window not main screen
                .perform(click());
        onView(withId(R.id.apply_button)).perform(click());

        // Ensuring the mood that fits all criteria appears
        UiObject marker1 = device.findObject(new UiSelector().descriptionContains("@testUser2: Sad"));
        assertTrue("Marker should be visible", marker1.exists());

        // Ensuring toast appears
        onView(withText("No personal mood events match the applied filters")).inRoot(withDecorView(Matchers.not(decorView)));

        //Turn personal mood history viewing off and following on
        onView(withId(R.id.filter_button)).perform(click());
        onView(withId(R.id.reset_button)).perform(click());

        // Ensuring the mood that fits all criteria appears
        UiObject marker3 = device.findObject(new UiSelector().descriptionContains("@testUser: Happy"));
        assertFalse("Marker should not be visible", marker3.exists());
        UiObject marker4 = device.findObject(new UiSelector().descriptionContains("@testUser2: Sad"));
        assertFalse("Marker should not be visible", marker4.exists());
    }

    private void grantPermission() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "pm grant " + InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName() + " " + Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @After
    public void tearDown() {
        clearFirestoreEmulator();
        clearAuthEmulator();
    }

    private void clearFirestoreEmulator() {
        String projectId = BuildConfig.FIREBASE_PROJECT_ID;
        String firestoreUrl = "http://10.0.2.2:8080/emulator/v1/projects/"
                + projectId
                + "/databases/(default)/documents";

        HttpURLConnection connection = null;
        try {
            URL url = new URL(firestoreUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            int responseCode = connection.getResponseCode();
            Log.i("tearDown", "Cleared Firestore emulator, response code: " + responseCode);
        } catch (IOException e) {
            Log.e("tearDown", "Error clearing Firestore emulator", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void clearAuthEmulator() {
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
