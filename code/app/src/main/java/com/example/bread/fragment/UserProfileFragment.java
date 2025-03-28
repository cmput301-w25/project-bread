package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.example.bread.model.FollowRequest;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.utils.ImageHandler;
import com.example.bread.view.LoginPage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.bread.utils.TimestampUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the profile page of the app, where users can view their profile information.
 */
public class UserProfileFragment extends Fragment {

    private static final String TAG = "UserProfileFragment";

    private TextView usernameText, followersCountText, followingCountText;
    private ImageView profileImageView;
    private LinearLayout followersLayout, followingLayout;
    private View recentMoodEventView;
    private TextView emptyMoodText;
    private ImageView settingsButton;

    private ParticipantRepository participantRepository;
    private MoodEventRepository moodEventRepository;
    private String currentUsername;
    private ListenerRegistration participantListener;

    private ArrayList<MoodEvent> userMoodEvents = new ArrayList<>();

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Bundle to pass username
        // Use listener to see if they follow or not
        // Change display according to following or not

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        usernameText = view.findViewById(R.id.profile_username);
        followersCountText = view.findViewById(R.id.followers_count);
        followingCountText = view.findViewById(R.id.following_count);
        profileImageView = view.findViewById(R.id.profile_image);

        followersLayout = view.findViewById(R.id.followers_layout);
        followingLayout = view.findViewById(R.id.following_layout);
        recentMoodEventView = view.findViewById(R.id.recent_mood_container);
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

        return view;
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
            ImageView profileImageView = recentMoodEventView.findViewById(R.id.profile_image_home);
            ImageView moodImageView = recentMoodEventView.findViewById(R.id.event_home_image);
            View cardBackground = recentMoodEventView.findViewById(R.id.moodCard);
            View imageContainer = recentMoodEventView.findViewById(R.id.event_home_image_holder);
            View constraintLayout = recentMoodEventView.findViewById(R.id.homeConstraintLayout);

            usernameView.setText(currentUsername);
            titleView.setText(recentMood.getTitle());
            dateView.setText(TimestampUtils.transformTimestamp(recentMood.getTimestamp()));
            moodView.setText(recentMood.getEmotionalState().toString() + " " + EmotionUtils.getEmoticon(recentMood.getEmotionalState()));

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

            recentMoodEventView.setVisibility(View.VISIBLE);
            emptyMoodText.setVisibility(View.GONE);
        }
    }

    private void setupParticipantListener() {
        // Replace with fetchBaseParticipant
        fetchParticipantData();
    }

    private void fetchParticipantData() {
        participantRepository.fetchBaseParticipant(currentUsername, participant -> {
            if (participant != null) {
                updateUI(participant);
            }
        }, e -> Log.e(TAG, "Error fetching participant data", e));
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
            Bitmap bitmap = ImageHandler.base64ToBitmap(participant.getProfilePicture());
            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap);
            } else {
                profileImageView.setImageResource(R.drawable.ic_baseline_profile_24);
            }
        }
    }

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