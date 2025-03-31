package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.bread.model.FollowRequest;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.utils.ImageHandler;
import com.example.bread.utils.TimestampUtils;
import com.example.bread.view.LoginPage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ProfileFragment - Fragment
 * <p>
 * Role / Purpose
 * Shows the current user's profile, recent mood event, and a preview of follow requests.
 * Provides navigation to followers, following, settings, and full request list.
 * <p>
 * Design Pattern
 * Fragment Pattern: Modular UI unit.
 * MVC Pattern: Interfaces with repository and adapters to manage state and interactions.
 * <p>
 * Outstanding Issues / Comments
 * Recent mood event reloads on every resume; caching could improve performance.
 */

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private TextView usernameText, followersCountText, followingCountText;
    private ImageView profileImageView;
    private LinearLayout followersLayout, followingLayout, allRequestsLayout;
    private RecyclerView requestsRecyclerView;
    private View recentMoodEventView;
    private TextView emptyRequestsText;
    private TextView emptyMoodText;
    private ImageView settingsButton;

    private ParticipantRepository participantRepository;
    private MoodEventRepository moodEventRepository;
    private String currentUsername;
    private ListenerRegistration participantListener;

    private FollowRequestAdapter requestAdapter;
    private final List<FollowRequest> requestsList = new ArrayList<>();

    private final ArrayList<MoodEvent> userMoodEvents = new ArrayList<>();

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

        // Initialize settings button
        settingsButton = view.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                    R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
            );
            transaction.add(R.id.frame_layout, new SettingsFragment());
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

    /**
     * Initializes the RecyclerView for displaying incoming follow requests.
     * Sets up the adapter with accept/decline listeners and triggers the initial request load.
     */
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

    /**
     * Loads the list of follow requests for the current user from the repository.
     * Displays a maximum of 3 requests for preview on the profile page.
     * Updates the adapter and view visibility accordingly.
     */
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

    /**
     * Updates the visibility of the follow requests section based on the current request list.
     * Shows a placeholder message if there are no requests.
     */
    private void updateRequestsVisibility() {
        if (requestsList.isEmpty()) {
            emptyRequestsText.setVisibility(View.VISIBLE);
            requestsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyRequestsText.setVisibility(View.GONE);
            requestsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Accepts a follow request from another user.
     * Removes the request from the list and checks if the user should be prompted to follow back.
     *
     * @param requestorUsername The username of the user who sent the follow request.
     * @param position          The position of the request in the list to remove.
     */
    private void handleAcceptRequest(String requestorUsername, int position) {
        participantRepository.acceptFollowRequest(currentUsername, requestorUsername, unused -> {
            // Remove from list and update UI
            if (position < requestsList.size()) {
                requestsList.remove(position);
                requestAdapter.notifyItemRemoved(position);
            }
            updateRequestsVisibility();

            // Check if the current user is already following the requestor
            // or has a pending follow request to the requestor before showing follow-back dialog
            checkFollowRelationship(requestorUsername);

        }, e -> {
            Log.e(TAG, "Error accepting follow request", e);
        });
    }

    /**
     * Check if the current user is already following or has a pending request to the requestor
     * before showing the follow back dialog
     */
    private void checkFollowRelationship(String requestorUsername) {
        // First check if already following
        participantRepository.isFollowing(currentUsername, requestorUsername, isAlreadyFollowing -> {
            if (isAlreadyFollowing) {
                // Already following this user, no need for follow back dialog
                return;
            }

            // Then check if a follow request already exists from current user to requestor
            participantRepository.checkFollowRequestExists(currentUsername, requestorUsername, requestExists -> {
                if (requestExists) {
                    // A follow request already exists, no need for follow back dialog
                } else {
                    // No existing relationship, show follow back dialog
                    showFollowBackDialog(requestorUsername);
                }
            }, e -> {
                Log.e(TAG, "Error checking follow request status", e);
                // In case of error, default to showing dialog
                showFollowBackDialog(requestorUsername);
            });

        }, e -> {
            Log.e(TAG, "Error checking following status", e);
            // In case of error, default to showing dialog
            showFollowBackDialog(requestorUsername);
        });
    }

    /**
     * Declines a follow request from another user.
     * Removes the request from the UI and updates the RecyclerView.
     *
     * @param requestorUsername The username of the user who sent the follow request.
     * @param position          The index of the request to remove from the list.
     */
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

    /**
     * Displays a dialog prompting the user to follow back someone who just sent a follow request.
     *
     * @param username The username of the user to potentially follow back.
     */
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

    /**
     * Sends a follow-back request to a user.
     *
     * @param username The username of the user to follow back.
     */
    private void sendFollowBackRequest(String username) {
        participantRepository.sendFollowRequest(currentUsername, username, unused -> {
            // Success
        }, e -> {
            Log.e(TAG, "Error sending follow back request", e);
        });
    }

    /**
     * Loads the most recent mood event of the current user.
     * Fetches data from Firestore and updates the view.
     */
    private void loadRecentMoodEvent() {
        if (currentUsername == null) return;

        DocumentReference participantRef = participantRepository.getParticipantRef(currentUsername);

        // Changed from listenForEventsWithParticipantRef to fetchEventsWithParticipantRef
        moodEventRepository.fetchEventsWithParticipantRef(participantRef, moodEvents -> {
            userMoodEvents.clear();
            userMoodEvents.addAll(moodEvents);

            // Sort by date (newest first)
            userMoodEvents.sort(Comparator.reverseOrder());

            // Display most recent mood event
            updateRecentMoodEvent();
        }, e -> {
            Log.e(TAG, "Failed to fetch mood events", e);
            updateRecentMoodEvent();
        });
    }

    /**
     * Updates the UI with the most recent mood event details.
     * Handles layout visibility, data binding, and conditional styling.
     */
    private void updateRecentMoodEvent() {
        if (userMoodEvents.isEmpty()) {
            recentMoodEventView.setVisibility(View.GONE);
            emptyMoodText.setVisibility(View.VISIBLE);
        } else {
            // Show most recent mood event
            MoodEvent recentMood = userMoodEvents.get(0);

            // Update UI with mood event details
            TextView usernameView = recentMoodEventView.findViewById(R.id.textUsername);
            TextView titleView = recentMoodEventView.findViewById(R.id.textTitle);
            TextView dateView = recentMoodEventView.findViewById(R.id.textDate);
            TextView moodView = recentMoodEventView.findViewById(R.id.textMood);
            TextView socialView = recentMoodEventView.findViewById(R.id.textSocialSituation);
            ImageView profileImageView = recentMoodEventView.findViewById(R.id.profile_image_home);
            ImageView moodImageView = recentMoodEventView.findViewById(R.id.event_home_image);
            View cardBackground = recentMoodEventView.findViewById(R.id.moodCard);
            View imageContainer = recentMoodEventView.findViewById(R.id.event_home_image_holder);
            View constraintLayout = recentMoodEventView.findViewById(R.id.homeConstraintLayout);

            usernameView.setText(currentUsername);
            titleView.setText(recentMood.getTitle());
            dateView.setText(TimestampUtils.transformTimestamp(recentMood.getTimestamp()));
            moodView.setText(recentMood.getEmotionalState().toString() + " " + EmotionUtils.getEmoticon(recentMood.getEmotionalState()));
            if (recentMood.getSocialSituation() != null && recentMood.getSocialSituation() != MoodEvent.SocialSituation.NONE) {
                socialView.setText(recentMood.getSocialSituation().toString());
            } else {
                socialView.setVisibility(View.INVISIBLE);
            }

            // Handle image visibility
            if (recentMood.getAttachedImage() != null && !recentMood.getAttachedImage().isEmpty()) {
                if (moodImageView != null) {
                    moodImageView.setImageBitmap(ImageHandler.base64ToBitmap(recentMood.getAttachedImage()));
                    moodImageView.setVisibility(View.VISIBLE);
                }
                if (imageContainer != null) {
                    imageContainer.setVisibility(View.VISIBLE);
                }
            } else {
                if (moodImageView != null) {
                    moodImageView.setVisibility(View.GONE);
                }
                if (imageContainer != null) {
                    imageContainer.setVisibility(View.GONE);
                }
            }

            // Set profile picture for the recent mood event
            if (profileImageView != null) {
                participantRepository.fetchBaseParticipant(currentUsername, participant -> {
                    if (participant != null && participant.getProfilePicture() != null) {
                        profileImageView.setImageBitmap(ImageHandler.base64ToBitmap(participant.getProfilePicture()));
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_baseline_profile_24);
                    }
                }, e -> Log.e(TAG, "Error loading profile image", e));
            }

            // Set background color based on emotional state
            int colorResId = EmotionUtils.getColorResource(recentMood.getEmotionalState());

            // Apply color to the card or constraint layout
            if (cardBackground != null) {
                cardBackground.setBackgroundResource(colorResId);
            } else if (constraintLayout != null) {
                constraintLayout.setBackgroundResource(colorResId);
            }

            // Add click listener to navigate to the event detail page
            recentMoodEventView.setOnClickListener(v -> {
                // Navigate to the EventDetail fragment
                navigateToEventDetail(recentMood);
            });

            recentMoodEventView.setVisibility(View.VISIBLE);
            emptyMoodText.setVisibility(View.GONE);
        }
    }

    /**
     * Navigates to the EventDetail fragment to display full details of the given mood event.
     *
     * @param moodEvent The mood event to view in detail.
     */
    private void navigateToEventDetail(MoodEvent moodEvent) {
        EventDetail fragment = EventDetail.newInstance(moodEvent);
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
        );
        transaction.add(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Sets up the real-time participant data listener.
     */
    private void setupParticipantListener() {
        // Replace with fetchBaseParticipant
        fetchParticipantData();
    }

    /**
     * Fetches the latest participant data for the current user and updates the UI.
     */
    private void fetchParticipantData() {
        participantRepository.fetchBaseParticipant(currentUsername, participant -> {
            if (participant != null) {
                updateUI(participant);
            }
        }, e -> Log.e(TAG, "Error fetching participant data", e));
    }

    /**
     * Updates the UI with the provided participant’s follower/following counts and profile picture.
     *
     * @param participant The participant object containing updated user data.
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
            Bitmap bitmap = ImageHandler.base64ToBitmap(participant.getProfilePicture());
            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap);
            } else {
                profileImageView.setImageResource(R.drawable.ic_baseline_profile_24);
            }
        }
    }

    /**
     * Navigates to the followers or following list fragment based on the selected type.
     *
     * @param listType The list type to view (followers or following).
     */
    private void navigateToFollowersList(ParticipantRepository.ListType listType) {
        String type = listType == ParticipantRepository.ListType.FOLLOWERS ? "followers" : "following";
        FollowersListFragment fragment = FollowersListFragment.newInstance(currentUsername, type);
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction().setCustomAnimations(
                R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
        );
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Navigates to the full follow requests management screen.
     */
    private void navigateToFollowRequests() {
        FollowRequestsFragment fragment = new FollowRequestsFragment();
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction().setCustomAnimations(
                R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
        );
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Logs the user out, clears preferences, and navigates back to the login page.
     */
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
        // Refresh data when returning to this fragment
        fetchParticipantData();
        loadFollowRequests();
        loadRecentMoodEvent();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up listener
        if (participantListener != null) {
            participantListener.remove();
            participantListener = null;
        }
    }
}