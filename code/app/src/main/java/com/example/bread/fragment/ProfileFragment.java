package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.controller.FollowRequestAdapter;
import com.example.bread.controller.HomeMoodEventArrayAdapter;
import com.example.bread.model.FollowRequest;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.MoodEvent.SocialSituation;
import com.example.bread.model.Participant;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.example.bread.view.LoginPage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private TextView usernameText, followersCountText, followingCountText;
    private ImageView profileImageView;
    private LinearLayout followersLayout, followingLayout, allRequestsLayout;
    private RecyclerView requestsRecyclerView;
    private View recentMoodEventView;
    private TextView emptyRequestsText;
    private TextView emptyMoodText;
    private ImageButton settingsButton;

    private ParticipantRepository participantRepository;
    private MoodEventRepository moodEventRepository;
    private String currentUsername;
    private ListenerRegistration participantListener;

    private FollowRequestAdapter requestAdapter;
    private List<FollowRequest> requestsList = new ArrayList<>();

    private ArrayList<MoodEvent> userMoodEvents = new ArrayList<>();

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
        moodEventRepository = new MoodEventRepository();
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
        allRequestsLayout = view.findViewById(R.id.all_requests_layout);
        requestsRecyclerView = view.findViewById(R.id.request_recycler_view);
        recentMoodEventView = view.findViewById(R.id.recent_mood_container);
        emptyRequestsText = view.findViewById(R.id.empty_requests_text);
        emptyMoodText = view.findViewById(R.id.empty_mood_text);

        // Initialize settings button from main branch
        settingsButton = view.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.frame_layout, new SettingsFragment());
            transaction.commit();
        });

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUsername = currentUser.getDisplayName();
            usernameText.setText(currentUsername);

            // Show loading state
            followersCountText.setText("...");
            followingCountText.setText("...");

            // Set up follow requests
            setupFollowRequests();

            // Set up recent mood event
            loadRecentMoodEvent();

            // Set up real-time listener for participant data
            setupParticipantListener();
        } else {
            navigateToLogin();
        }

        // Set up click listeners
        followersLayout.setOnClickListener(v -> navigateToFollowersList(ParticipantRepository.ListType.FOLLOWERS));
        followingLayout.setOnClickListener(v -> navigateToFollowersList(ParticipantRepository.ListType.FOLLOWING));
        allRequestsLayout.setOnClickListener(v -> navigateToFollowRequests());

        return view;
    }

    private void setupFollowRequests() {
        // Set up RecyclerView
        requestAdapter = new FollowRequestAdapter(requestsList, new FollowRequestAdapter.RequestActionListener() {
            @Override
            public void onAccept(String requestorUsername, int position) {
                handleAcceptRequest(requestorUsername, position);
            }

            @Override
            public void onDecline(String requestorUsername, int position) {
                handleDeclineRequest(requestorUsername, position);
            }
        }, participantRepository);

        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsRecyclerView.setAdapter(requestAdapter);

        // Load requests
        loadFollowRequests();
    }

    private void loadFollowRequests() {
        if (currentUsername == null || currentUsername.isEmpty()) {
            updateRequestsVisibility();
            return;
        }

        participantRepository.fetchFollowRequests(currentUsername, requests -> {
            requestsList.clear();

            // Only show up to 3 requests in profile
            int count = Math.min(requests.size(), 3);
            for (int i = 0; i < count; i++) {
                requestsList.add(requests.get(i));
            }

            requestAdapter.notifyDataSetChanged();
            updateRequestsVisibility();
        }, e -> {
            Log.e(TAG, "Error loading follow requests", e);
            updateRequestsVisibility();
        });
    }

    private void updateRequestsVisibility() {
        if (requestsList.isEmpty()) {
            emptyRequestsText.setVisibility(View.VISIBLE);
            requestsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyRequestsText.setVisibility(View.GONE);
            requestsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void handleAcceptRequest(String requestorUsername, int position) {
        participantRepository.acceptFollowRequest(currentUsername, requestorUsername, unused -> {
            // Remove from list and update UI
            if (position < requestsList.size()) {
                requestsList.remove(position);
                requestAdapter.notifyItemRemoved(position);
            }
            updateRequestsVisibility();

            // Show follow back dialog
            showFollowBackDialog(requestorUsername);

        }, e -> {
            Log.e(TAG, "Error accepting follow request", e);
        });
    }

    private void handleDeclineRequest(String requestorUsername, int position) {
        participantRepository.declineFollowRequest(currentUsername, requestorUsername, unused -> {
            if (position < requestsList.size()) {
                requestsList.remove(position);
                requestAdapter.notifyItemRemoved(position);
            }
            updateRequestsVisibility();
        }, e -> {
            Log.e(TAG, "Error declining follow request", e);
        });
    }

    private void showFollowBackDialog(String username) {
        if (getContext() != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Follow Back")
                    .setMessage("Do you want to follow " + username + " back?")
                    .setPositiveButton("Follow", (dialog, which) -> {
                        sendFollowBackRequest(username);
                    })
                    .setNegativeButton("Not Now", null)
                    .setCancelable(true)
                    .show();
        }
    }

    private void sendFollowBackRequest(String username) {
        participantRepository.sendFollowRequest(currentUsername, username, unused -> {
            // Success
        }, e -> {
            Log.e(TAG, "Error sending follow back request", e);
        });
    }

    private void loadRecentMoodEvent() {
        if (currentUsername == null) return;

        DocumentReference participantRef = participantRepository.getParticipantRef(currentUsername);
        moodEventRepository.listenForEventsWithParticipantRef(participantRef, moodEvents -> {
            userMoodEvents.clear();

            for (MoodEvent event : moodEvents) {
                if (event.getTimestamp() != null) {
                    userMoodEvents.add(event);
                }
            }

            // Sort by date (newest first)
            Collections.sort(userMoodEvents);
            Collections.reverse(userMoodEvents);

            // Display most recent mood event
            updateRecentMoodEvent();

        }, e -> {
            Log.e(TAG, "Failed to fetch mood events", e);
            updateRecentMoodEvent();
        });
    }

    private void updateRecentMoodEvent() {
        if (userMoodEvents.isEmpty()) {
            recentMoodEventView.setVisibility(View.GONE);
            emptyMoodText.setVisibility(View.VISIBLE);
        } else {
            // Show most recent mood event
            MoodEvent recentMood = userMoodEvents.get(0);

            // Update UI with mood event details
            TextView usernameView = recentMoodEventView.findViewById(R.id.textUsername);
            TextView reasonView = recentMoodEventView.findViewById(R.id.textReason);
            TextView dateView = recentMoodEventView.findViewById(R.id.textDate);
            TextView moodView = recentMoodEventView.findViewById(R.id.textMood);
            View cardBackground = recentMoodEventView.findViewById(R.id.moodCard);
            View constraintLayout = recentMoodEventView.findViewById(R.id.homeConstraintLayout);

            usernameView.setText(currentUsername);
            reasonView.setText(recentMood.getReason());
            dateView.setText(recentMood.getTimestamp().toString());
            moodView.setText(com.example.bread.utils.EmotionUtils.getEmoticon(recentMood.getEmotionalState()));

            // Set background color based on emotional state
            int colorResId = com.example.bread.utils.EmotionUtils.getColorResource(recentMood.getEmotionalState());

            // Apply color to the card or constraint layout
            if (cardBackground != null) {
                cardBackground.setBackgroundResource(colorResId);
            } else if (constraintLayout != null) {
                constraintLayout.setBackgroundResource(colorResId);
            }

            recentMoodEventView.setVisibility(View.VISIBLE);
            emptyMoodText.setVisibility(View.GONE);
        }
    }

    private void setupParticipantListener() {
        // Remove any existing listener
        if (participantListener != null) {
            participantListener.remove();
        }

        // Set up real-time listener for participant data
        participantListener = participantRepository.listenForParticipantUpdates(currentUsername, participant -> {
            if (participant != null) {
                updateUI(participant);
            }
        });
    }

    /**
     * Update UI with participant data
     */
    private void updateUI(Participant participant) {
        // Update follower and following counts
        if (followersCountText != null) {
            followersCountText.setText(String.valueOf(participant.getFollowerCount()));
        }
        if (followingCountText != null) {
            followingCountText.setText(String.valueOf(participant.getFollowingCount()));
        }

        // Set profile picture if available
        if (participant.getProfilePicture() != null && profileImageView != null) {
            profileImageView.setImageBitmap(ImageHandler.base64ToBitmap(participant.getProfilePicture()));
        }
    }

    private void navigateToFollowersList(ParticipantRepository.ListType listType) {
        String type = listType == ParticipantRepository.ListType.FOLLOWERS ? "followers" : "following";
        FollowersListFragment fragment = FollowersListFragment.newInstance(currentUsername, type);
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
        if (participantListener == null && currentUsername != null) {
            setupParticipantListener();
        }
        loadFollowRequests();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (participantListener != null) {
            participantListener.remove();
            participantListener = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (participantListener != null) {
            participantListener.remove();
            participantListener = null;
        }
    }
}