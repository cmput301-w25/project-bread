package com.example.bread;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.bread.model.Comment;
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
import com.google.android.gms.maps.model.LatLng;

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
import java.util.List;
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

    @Rule
    public ActivityScenarioRule<HomePage> activityScenarioRule = new ActivityScenarioRule<>(HomePage.class);

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
//        LatLng mockUserLocation = new LatLng(37.4219999, -122.0840575);
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mockUserLocation, 15));

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

        double latitude = 37.4220936;
        double longitude = -122.083922;
        String geohash = "9q9hvumpqr";

        Map<String, Object> geoInfo = new HashMap<>();
        geoInfo.put("latitude", latitude);
        geoInfo.put("longitude", longitude);
        geoInfo.put("geohash", geohash);

        MoodEvent moodEvent1 = new MoodEvent("Test Event 1", "test reason", MoodEvent.EmotionalState.HAPPY, p2Ref);
        moodEvent1.setSocialSituation(MoodEvent.SocialSituation.ALONE);
        MoodEvent moodEvent2 = new MoodEvent("Test Event 2", "test reason", MoodEvent.EmotionalState.ANGRY, p2Ref);
        moodEvent2.setSocialSituation(MoodEvent.SocialSituation.WITH_FRIENDS);
        MoodEvent moodEvent3 = new MoodEvent("Test Event 3", "test reason", MoodEvent.EmotionalState.SAD, p1Ref);
        moodEvent3.setSocialSituation(MoodEvent.SocialSituation.ALONE);
        MoodEvent moodEvent4 = new MoodEvent("Test Event 4", "test reason", MoodEvent.EmotionalState.ANXIOUS, p1Ref);
        moodEvent4.setSocialSituation(MoodEvent.SocialSituation.WITH_FRIENDS);
        moodEvent4.setGeoInfo(geoInfo);

        CollectionReference moodEvents = db.collection("moodEvents");
        MoodEvent[] events = {
                moodEvent1, moodEvent2, moodEvent3, moodEvent4
        };

        for (MoodEvent event : events) {
            moodEvents.document(event.getId()).set(event);
        }

        Comment comment1 = new Comment(p1Ref, "test comment 1");
        Comment comment2 = new Comment(p1Ref, "test comment 2");

        for (Comment comment : List.of(comment1, comment2)) {
            moodEvents.document(moodEvent1.getId()).collection("comments")
                    .document(comment.getId()).set(comment);
        }

        for (Comment comment : List.of(comment1, comment2)) {
            moodEvents.document(moodEvent2.getId()).collection("comments")
                    .document(comment.getId()).set(comment);
        }
    }

    @Test
    //https://developer.android.com/training/testing/other-components/ui-automator
    public void testMarkerAppears() throws Exception {
        Thread.sleep(2000);
        grantPermission();
        onView(withId(R.id.map)).perform(click());
        Thread.sleep(2000);

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject allowButton = device.findObject(new UiSelector().textContains("While using the app"));

        if (allowButton.exists()) {
            allowButton.click();
        }

        Thread.sleep(2000);

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

        Thread.sleep(3000);

        onView(withId(R.id.history_switch)).perform(click());

        onView(withId(R.id.apply_button)).perform(click());

        Thread.sleep(3000);

        UiObject marker = device.findObject(new UiSelector().descriptionContains("@testUser: Anxious"));

        assertTrue("Marker should be visible", marker.exists());
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
