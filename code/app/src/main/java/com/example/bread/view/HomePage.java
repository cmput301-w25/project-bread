package com.example.bread.view;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.bread.R;
import com.example.bread.databinding.ActivityHomePageBinding;
import com.example.bread.fragment.AddMoodEventFragment;
import com.example.bread.fragment.FollowRequestsFragment;
import com.example.bread.fragment.HistoryFragment;
import com.example.bread.fragment.HomeFragment;
import com.example.bread.fragment.MapFragment;
import com.example.bread.fragment.ProfileFragment;
import com.example.bread.utils.NotificationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.bread.fragment.UserSearchFragment;

/**
 * HomePage - View
 *
 * Role / Purpose:
 * This is the main landing activity after login, serving as the navigation hub for users to interact
 * with different features of the app like personal mood history, settings , map, etc.
 * on the home page, u get to see the mood events of the users that you are following.(3 recent mood events per user.)
 *
 * Design Pattern:
 * - View Pattern: Manages the navigation and lifecycle of fragments displayed on the screen.
 * - Observer Pattern: Listens to real-time notification updates using Firestore listeners.
 *
 * Outstanding Issues / Comments:
 * -
 * -
 */
public class HomePage extends AppCompatActivity {

    private static final String TAG = "HomePage";
    ActivityHomePageBinding binding;
    private ListenerRegistration notificationListener;


    /**
     * Lifecycle method called when the activity is resumed.
     * Ensures notification channels exist and re-registers the Firestore listener.
     */
    protected void onResume() {
        super.onResume();

        // Create notification channels (in case they're not already created)
        NotificationUtils.createNotificationChannels(this);

        // Set up notification listener
        setupNotificationListener();
    }
    /**
     * Lifecycle method called when the activity is paused.
     * Unregisters Firestore listener to avoid memory leaks.
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Remove notification listener when activity is paused
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
    /**
     * Called when the activity is created.
     * Sets up the layout, bottom navigation bar listener, and initial fragment.
     *
     * @param savedInstanceState previous state of the activity, if any
     */
    @SuppressLint("NonConstantResourceId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //followed the following video for navigation bar implementation, accessed on Feb 27 2025
        //https://www.youtube.com/watch?v=jOFLmKMOcK0
        binding = ActivityHomePageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Create notification channels
        NotificationUtils.createNotificationChannels(this);
        replaceFragment(new HomeFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.map) {
                replaceFragment(new MapFragment());
            } else if (itemId == R.id.add) {
                // Different approaches in the two versions:
                // 1. Your branch: Starts AddMoodEventActivity
                // 2. Main branch: Uses AddMoodEventFragment
                // We'll use the fragment approach from main:
                replaceFragment(new AddMoodEventFragment());

                // If you need the activity approach, uncomment these lines:
                /*
                Intent intent = new Intent(HomePage.this, AddMoodEventActivity.class);
                startActivity(intent);
                return false; // Don't select the tab
                */
            } else if (itemId == R.id.history) {
                replaceFragment(new HistoryFragment());
            } else if (itemId == R.id.profile) {
                replaceFragment(new ProfileFragment());
            }

            return true;  // Important to return true to indicate the item was selected
        });

        // Check if the activity was launched from a notification
        if (getIntent() != null) {
            handleNotificationIntent(getIntent());
        }
    }

    /**
     * Replaces the current fragment inside the main container.
     *
     * @param fragment the new fragment to show
     */

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
        );
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();
    }
    /**
     * Programmatically selects the home item in the bottom navigation bar.
     */
    public void selectHomeNavigation() {
        binding.bottomNavigationView.setSelectedItemId(R.id.home);
    }

    /**
     * Navigates to a specific named fragment based on app-specific tags.
     *
     * @param fragmentName name of the fragment ("followRequests", "userSearch", "profile")
     */
    public void navigateToFragment(String fragmentName) {
        Fragment fragment = null;

        switch (fragmentName) {
            case "followRequests":
                fragment = new FollowRequestsFragment();
                break;
            case "userSearch":
                fragment = new UserSearchFragment();
                break;
            case "profile":
                fragment = new ProfileFragment();
                break;
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            //FragmentTransaction transaction = fragmentManager.beginTransaction();
            FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                    R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out);
            transaction.replace(R.id.frame_layout, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
    /**
     * Called when a new intent is delivered to an already running instance.
     * Used for handling navigation via notifications.
     *
     * @param intent the new intent containing navigation data
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleNotificationIntent(intent);
    }
    /**
     * Handles incoming intents triggered by notifications.
     * Navigates to appropriate fragment based on "navigate_to" extra.
     *
     * @param intent the intent that triggered the activity
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent != null) {
            String navigateTo = intent.getStringExtra("navigate_to");
            Log.d(TAG, "handleNotificationIntent: navigate_to = " + navigateTo);

            if ("follow_requests".equals(navigateTo)) {


                // Select profile in bottom navigation
                binding.bottomNavigationView.setSelectedItemId(R.id.profile);

                // Need to wait briefly for the profile fragment to attach
                binding.bottomNavigationView.postDelayed(() -> {
                    // Navigate to follow requests fragment using the matching case from the switch statement
                    navigateToFragment("followRequests");
                    Log.d(TAG, "handleNotificationIntent: Navigated to followRequests fragment");

                    // Get sender username if available
                    String senderUsername = intent.getStringExtra(NotificationUtils.EXTRA_SENDER_USERNAME);
                    if (senderUsername != null && !senderUsername.isEmpty()) {
                        Log.d(TAG, "handleNotificationIntent: Sender username: " + senderUsername);
                        // You could pass this to the fragment if needed
                        // For example, to highlight this specific request
                    }
                }, 300); // Short delay to ensure fragment transaction completes
            }
        }
    }
    /**
     * Sets up a Firestore snapshot listener for unread notifications for the current user.
     * Triggers system notifications and marks notifications as read.
     */
    private void setupNotificationListener() {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getDisplayName() == null) {
            Log.e(TAG, "Cannot setup notification listener - no current user");
            return;
        }

        String username = currentUser.getDisplayName();
        Log.d(TAG, "Setting up notification listener for user: " + username);

        // Listen for new notifications
        notificationListener = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("recipientUsername", username)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for notifications", e);
                        return;
                    }

                    Log.d(TAG, "Notification listener fired. Checking for new notifications...");

                    if (snapshots == null) {
                        Log.d(TAG, "Notification snapshots is null");
                        return;
                    }

                    if (snapshots.isEmpty()) {
                        Log.d(TAG, "No notifications found in snapshot");
                        return;
                    }

                    Log.d(TAG, "Found " + snapshots.size() + " total notifications");

                    // Process new notifications
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            // Display local notification for this new notification
                            DocumentSnapshot document = dc.getDocument();
                            String docId = document.getId();
                            String type = document.getString("type");
                            String title = document.getString("title");
                            String message = document.getString("message");
                            String senderUsername = document.getString("senderUsername");

                            Log.d(TAG, "Processing new notification: ID=" + docId +
                                    ", Type=" + type +
                                    ", Title=" + title +
                                    ", Message=" + message +
                                    ", Sender=" + senderUsername);

                            try {
                                // Create intent
                                Intent intent = new Intent(this, HomePage.class);
                                if ("follow_request".equals(type)) {
                                    intent.putExtra("navigate_to", "follow_requests");
                                    intent.putExtra(NotificationUtils.EXTRA_SENDER_USERNAME, senderUsername);
                                    Log.d(TAG, "Created intent for follow request navigation");
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                // Create PendingIntent
                                PendingIntent pendingIntent = PendingIntent.getActivity(
                                        this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

                                // Generate a unique notification ID using the document ID
                                int notificationId = document.getId().hashCode();

                                // Show notification
                                NotificationUtils.showNotification(
                                        this,
                                        title != null ? title : "New Notification",
                                        message != null ? message : "",
                                        pendingIntent,
                                        notificationId);

                                // Mark as read
                                document.getReference().update("read", true)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Marked notification as read: " + docId))
                                        .addOnFailureListener(err -> Log.e(TAG, "Failed to mark notification as read: " + docId, err));

                            } catch (Exception ex) {
                                Log.e(TAG, "Error showing notification: " + ex.getMessage(), ex);
                            }
                        }
                    }
                });

        Log.d(TAG, "Notification listener setup complete for user: " + username);
    }
}
