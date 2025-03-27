package com.example.bread.view;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
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
 * Represents the home page of the app, where users can navigate to different fragments.
 */
public class HomePage extends AppCompatActivity {

    private static final String TAG = "HomePage";
    ActivityHomePageBinding binding;

    protected void onResume() {
        super.onResume();

        // Create notification channels (in case they're not already created)
        NotificationUtils.createNotificationChannels(this);

        // Set up notification listener
        setupNotificationListener();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove notification listener when activity is paused
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
    private ListenerRegistration notificationListener;
    @SuppressLint("NonConstantResourceId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //followed the following video for navigation bar implementation, accessed on Feb 27 2025
        //https://www.youtube.com/watch?v=jOFLmKMOcK0
        binding = ActivityHomePageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
        );
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();
    }

    public void selectHomeNavigation() {
        binding.bottomNavigationView.setSelectedItemId(R.id.home);
    }

    // Method to navigate to specific fragments from your branch
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
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.frame_layout, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null) {
            String navigateTo = intent.getStringExtra("navigate_to");

            if ("follow_requests".equals(navigateTo)) {
                // Select profile in bottom navigation
                binding.bottomNavigationView.setSelectedItemId(R.id.profile);

                // once the fragment is setup, we can navgiate to the follow requests fragment and uncomment the line below
                navigateToFragment("FollowRequestsFragment");

                // Get sender username if available
                String senderUsername = intent.getStringExtra(NotificationUtils.EXTRA_SENDER_USERNAME);
                if (senderUsername != null && !senderUsername.isEmpty()) {
                    // You could pass this to the fragment if needed
                    // For example, to highlight this specific request
                }
            }
        }
    }

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
