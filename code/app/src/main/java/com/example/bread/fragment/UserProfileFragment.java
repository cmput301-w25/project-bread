package com.example.bread.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.utils.ImageHandler;
import com.example.bread.utils.TimestampUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * UserProfileFragment - Fragment
 * <p>
 * Role / Purpose
 * Displays another user's profile information including follower/following count, profile picture,
 * and most recent mood event. Allows current user to send a follow request.
 * <p>
 * Design Pattern
 * Fragment Pattern: Represents a UI screen within the navigation flow.
 * MVC Pattern: Uses repositories to retrieve and bind participant and mood event data.
 * <p>
 * Outstanding Issues / Comments
 * Does not currently support loading the full list of mood events or followers.
 * Real-time updates are limited to initial fetch; no active listeners on mood changes.
 */

public class UserProfileFragment extends Fragment {

    private static final String TAG = "UserProfileFragment";

    private TextView usernameText, followersCountText, followingCountText;
    private ImageView profileImageView;
    private LinearLayout followersLayout, followingLayout;
    private View recentMoodEventView;
    private TextView emptyMoodText, recentMoodHeader;
    private Button followRequest;

    private ParticipantRepository participantRepository;
    private MoodEventRepository moodEventRepository;
    private String currentUsername;

    private String followedUsername;

    private ListenerRegistration participantListener;

    private final ArrayList<MoodEvent> userMoodEvents = new ArrayList<>();

    public UserProfileFragment() {
        // Required empty public constructor
    }

    public static UserProfileFragment newInstance() {
        return new UserProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        participantRepository = new ParticipantRepository();
        moodEventRepository = new MoodEventRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUsername = currentUser.getDisplayName();
        // Get followed user
        Bundle data = getArguments();
        if (data != null) {
            followedUsername = data.getString("text");  // "text" is the key used when setting the bundle
            // Set up real-time listener for participant data
            setupParticipantListener();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        // Initialize views
        usernameText = view.findViewById(R.id.profile_username);
        usernameText.setText(followedUsername);

        recentMoodHeader = view.findViewById(R.id.recent_mood_header);

        followersCountText = view.findViewById(R.id.followers_count);
        followingCountText = view.findViewById(R.id.following_count);

        // Show loading state
        followersCountText.setText("...");
        followingCountText.setText("...");

        profileImageView = view.findViewById(R.id.profile_image);

        followersLayout = view.findViewById(R.id.followers_layout);
        followingLayout = view.findViewById(R.id.following_layout);
        recentMoodEventView = view.findViewById(R.id.recent_mood_container);
        emptyMoodText = view.findViewById(R.id.empty_mood_text);

        followRequest = view.findViewById(R.id.follow_button);

        // Set up click listeners
        followersLayout.setOnClickListener(v -> navigateToFollowersList(ParticipantRepository.ListType.FOLLOWERS));
        followingLayout.setOnClickListener(v -> navigateToFollowersList(ParticipantRepository.ListType.FOLLOWING));

        // Check if user is following them or not
        participantRepository.isFollowing(currentUsername, followedUsername, isFollowing -> {
            if (isFollowing) {
                followRequest.setVisibility(View.GONE);
                loadRecentMoodEvent();
            } else {
                recentMoodEventView.setVisibility(View.GONE);
                emptyMoodText.setVisibility(View.GONE);
                recentMoodHeader.setVisibility(View.GONE);

                updateFollowButtonState(followedUsername);

                // Set follow button click listener
                followRequest.setOnClickListener(v -> {
                    if (followRequest.getText().toString().equals("Follow")) {
                        onFollowClickProfile(followedUsername);
                        // Update button immediately for better UX
                        followRequest.setText("Requested");
                        followRequest.setEnabled(false);
                    }
                });
            }
        }, e -> {
            Log.e(TAG, "Error checking follow status", e);
            Toast.makeText(getContext(), "Error checking follow status", Toast.LENGTH_SHORT).show();
        });

        ImageView closeButton = view.findViewById(R.id.analytics_close_button);
        closeButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                    .remove(this)
                    .commit();
            getParentFragmentManager().popBackStack();
        });

        return view;
    }

    /**
     * Loads the most recent mood event of the followed user from Firestore.
     * Clears the current list and updates the UI after sorting events by date.
     */
    private void loadRecentMoodEvent() {
        if (followedUsername == null) return;

        DocumentReference participantRef = participantRepository.getParticipantRef(followedUsername);

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
     * Updates the UI to display the most recent mood event of the followed user.
     * Handles setting text, mood details, images, and color based on emotional state.
     * If no events are found, shows an empty message.
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

            usernameView.setText(followedUsername);
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
                participantRepository.fetchBaseParticipant(followedUsername, participant -> {
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

            recentMoodEventView.setVisibility(View.VISIBLE);
            emptyMoodText.setVisibility(View.GONE);
        }
    }

    /**
     * Initializes the participant data listener by fetching profile information.
     * Intended to be called during setup or when profile data should refresh.
     */
    private void setupParticipantListener() {
        // Replace with fetchBaseParticipant
        fetchParticipantData();
    }

    /**
     * Fetches base profile data of the followed user from Firestore and updates the UI.
     * Used to display follower/following counts and profile picture.
     */
    private void fetchParticipantData() {
        participantRepository.fetchBaseParticipant(followedUsername, participant -> {
            if (participant != null) {
                updateUI(participant);
            }
        }, e -> Log.e(TAG, "Error fetching participant data", e));
    }

    /**
     * Updates the profile UI with participant data including profile picture and social metrics.
     *
     * @param participant The participant object containing display information.
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
     * Navigates to a FollowersListFragment for the specified list type (followers or following).
     *
     * @param listType The type of list to display.
     */
    private void navigateToFollowersList(ParticipantRepository.ListType listType) {
        String type = listType == ParticipantRepository.ListType.FOLLOWERS ? "followers" : "following";
        FollowersListFragment fragment = FollowersListFragment.newInstance(followedUsername, type);
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction().setCustomAnimations(
                R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
        );
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Handles follow button click logic for a user.
     * Checks if the current user already follows the target user or has sent a request.
     * Sends a follow request if no relationship exists.
     *
     * @param username The username of the user to follow.
     */
    private void onFollowClickProfile(String username) {
        // First check if already following
        participantRepository.isFollowing(currentUsername, username, isFollowing -> {
            if (isFollowing) {
                Toast.makeText(getContext(), "You are already following this user", Toast.LENGTH_SHORT).show();
                return;
            }

            // Then check if a follow request already exists
            participantRepository.checkFollowRequestExists(currentUsername, username, requestExists -> {
                if (requestExists) {
                    Toast.makeText(getContext(), "Follow request already sent", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Send follow request
                participantRepository.sendFollowRequest(currentUsername, username, unused -> {
                    Toast.makeText(getContext(), "Follow request sent", Toast.LENGTH_SHORT).show();
                }, e -> {
                    Log.e(TAG, "Error sending follow request", e);
                    Toast.makeText(getContext(), "Error sending follow request", Toast.LENGTH_SHORT).show();
                });
            }, e -> {
                Log.e(TAG, "Error checking follow request", e);
                Toast.makeText(getContext(), "Error checking follow status", Toast.LENGTH_SHORT).show();
            });
        }, e -> {
            Log.e(TAG, "Error checking follow status", e);
            Toast.makeText(getContext(), "Error checking follow status", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Updates the follow button text and state based on the current follow relationship.
     * Displays "Following", "Requested", or "Follow" based on relationship status.
     *
     * @param followedUser The username of the user whose relationship status is being checked.
     */
    private void updateFollowButtonState(String followedUser) {
        // Check if already following
        participantRepository.isFollowing(currentUsername, followedUser, isFollowing -> {
            if (isFollowing) {
                followRequest.setText("Following");
                followRequest.setEnabled(false);
            } else {
                // Check if a follow request exists
                participantRepository.checkFollowRequestExists(currentUsername, followedUser, requestExists -> {
                    if (requestExists) {
                        followRequest.setText("Requested");
                        followRequest.setEnabled(false);
                    } else {
                        followRequest.setText("Follow");
                        followRequest.setEnabled(true);
                    }
                }, e -> {
                    // Default to Follow if error
                    followRequest.setText("Follow");
                    followRequest.setEnabled(true);
                });
            }
        }, e -> {
            // Default to Follow if error
            followRequest.setText("Follow");
            followRequest.setEnabled(true);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        fetchParticipantData();
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