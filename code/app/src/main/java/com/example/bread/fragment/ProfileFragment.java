package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.bread.R;
import com.example.bread.model.Participant;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.example.bread.view.LoginPage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private TextView usernameText, followersCountText, followingCountText;
    private ImageView profileImageView;
    private LinearLayout followersLayout, followingLayout, requestsLayout, searchUsersLayout, settingsLayout;

    private ParticipantRepository participantRepository;
    private String currentUsername;
    private ListenerRegistration participantListener;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        participantRepository = new ParticipantRepository();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        usernameText = view.findViewById(R.id.profile_username);
        followersCountText = view.findViewById(R.id.followers_count);
        followingCountText = view.findViewById(R.id.following_count);
        profileImageView = view.findViewById(R.id.profile_image);

        followersLayout = view.findViewById(R.id.followers_layout);
        followingLayout = view.findViewById(R.id.following_layout);
        requestsLayout = view.findViewById(R.id.requests_layout);
        searchUsersLayout = view.findViewById(R.id.search_users_layout);
        settingsLayout = view.findViewById(R.id.settings_layout);

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUsername = currentUser.getDisplayName();
            usernameText.setText(currentUsername);

            // Show loading state
            followersCountText.setText("...");
            followingCountText.setText("...");

            // Set up real-time listener for participant data
            setupParticipantListener();
        } else {
            navigateToLogin();
        }

        // Set up click listeners
        followersLayout.setOnClickListener(v -> navigateToFollowersList());
        followingLayout.setOnClickListener(v -> navigateToFollowingList());
        requestsLayout.setOnClickListener(v -> navigateToFollowRequests());
        searchUsersLayout.setOnClickListener(v -> navigateToUserSearch());
        settingsLayout.setOnClickListener(v -> showSettingsOptions());

        return view;
    }

    private void setupParticipantListener() {
        // Remove any existing listener
        if (participantListener != null) {
            participantListener.remove();
        }

        // Set up real-time listener for participant data
        participantListener = participantRepository.getParticipantCollRef()
                .document(currentUsername)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed for participant data", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // Get the participant data
                        reloadFollowerCounts();
                    }
                });
    }

    private void reloadFollowerCounts() {
        // Directly load the follower and following counts
        participantRepository.fetchFollowers(currentUsername, followers -> {
            int count = followers != null ? followers.size() : 0;
            if (followersCountText != null) {
                followersCountText.setText(String.valueOf(count));
            }
        }, e -> {
            Log.e(TAG, "Error fetching followers", e);
            if (followersCountText != null) {
                followersCountText.setText("0");
            }
        });

        participantRepository.fetchFollowing(currentUsername, following -> {
            int count = following != null ? following.size() : 0;
            if (followingCountText != null) {
                followingCountText.setText(String.valueOf(count));
            }
        }, e -> {
            Log.e(TAG, "Error fetching following", e);
            if (followingCountText != null) {
                followingCountText.setText("0");
            }
        });

        // Load profile picture
        participantRepository.fetchBaseParticipant(currentUsername, participant -> {
            if (participant != null && participant.getProfilePicture() != null && profileImageView != null) {
                profileImageView.setImageBitmap(ImageHandler.base64ToBitmap(participant.getProfilePicture()));
            }
        }, null);
    }

    private void navigateToFollowersList() {
        FollowersListFragment fragment = FollowersListFragment.newInstance(currentUsername, "followers");
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void navigateToFollowingList() {
        FollowersListFragment fragment = FollowersListFragment.newInstance(currentUsername, "following");
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void navigateToFollowRequests() {
        FollowRequestsFragment fragment = new FollowRequestsFragment();
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void navigateToUserSearch() {
        UserSearchFragment fragment = new UserSearchFragment();
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showSettingsOptions() {
        // Show a dialog with setting options
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Settings");

        String[] options = {"Edit Profile", "Privacy Settings", "Log out"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // Edit profile (for future implementation)
                    Toast.makeText(getContext(), "Edit Profile - Coming soon", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    // Privacy settings (for future implementation)
                    Toast.makeText(getContext(), "Privacy Settings - Coming soon", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    logoutUser();
                    break;
            }
        });

        builder.show();
    }

    private void logoutUser() {
        // Clear SharedPreferences
        SharedPreferences preferences = getActivity().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
        preferences.edit().clear().apply();

        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // Go back to login page
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(getContext(), LoginPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Force a refresh of follower/following counts
        if (currentUsername != null) {
            reloadFollowerCounts();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Clean up listener when paused
        if (participantListener != null) {
            participantListener.remove();
            participantListener = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Ensure listener is removed
        if (participantListener != null) {
            participantListener.remove();
            participantListener = null;
        }
    }
}