package com.example.bread.controller;

import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bread.R;
import com.example.bread.model.Participant;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * UserAdapter - Controller
 *
 * Role / Purpose
 * RecyclerView adapter used to display Participant users
 * Displays profile picture, username, full name, and a dynamic follow button.
 * The adapter allows users to send follow requests, view other users' profiles, and handles different states like already following
 * or follow request already sent.
 *
 * Design Patterns
 * Adapter Pattern: Binds Participant model data to RecyclerView list items.
 * ViewHolder Pattern: Optimizes view reuse for better performance.
 * Repository Pattern: Uses ParticipantRepository to handle data and follow-state checks.
 * Observer Pattern: Responds to user interactions via a listener interface.
 *
 * Outstanding Issues
 * Follow state is updated optimistically, so backend errors may cause UI inconsistency.
 * No support for follow request cancellation or unfollowing.
 */

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<Participant> userList;
    private final UserInteractionListener listener;
    private final ParticipantRepository participantRepository;
    private final String currentUsername;
    private final boolean showFollowButton;

    public UserAdapter(List<Participant> userList, UserInteractionListener listener) {
        this(userList, listener, true);
    }

    public UserAdapter(List<Participant> userList, UserInteractionListener listener, boolean showFollowButton) {
        this.userList = userList;
        this.listener = listener;
        this.participantRepository = new ParticipantRepository();
        this.currentUsername = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "";
        this.showFollowButton = showFollowButton;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Participant participant = userList.get(position);
        holder.bind(participant);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * Interface for listener that handles user interactions (following or viewing profile)
     */
    public interface UserInteractionListener {
        void onFollowClick(Participant participant);
        void onUserClick(Participant participant);
    }

    /**
     * ViewHolder that holds variables related to users (profile picture, username, name, follow status)
     * Assigns/sets values to related variables
     */
    class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView usernameText, nameText;
        Button followButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);

            usernameText = itemView.findViewById(R.id.follow_username_text);
            usernameText.setPaintFlags(usernameText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            nameText = itemView.findViewById(R.id.name_text);
            followButton = itemView.findViewById(R.id.follow_button);
        }

        void bind(Participant participant) {
            usernameText.setText(participant.getUsername());
            nameText.setText(participant.getFirstName() + " " + participant.getLastName());

            // Set profile image if available
            if (participant.getProfilePicture() != null) {
                profileImage.setImageBitmap(ImageHandler.base64ToBitmap(participant.getProfilePicture()));
            } else {
                profileImage.setImageResource(R.drawable.default_avatar);
            }

            // Hide follow button in followers/following list if needed
            if (!showFollowButton || participant.getUsername().equals(currentUsername)) {
                followButton.setVisibility(View.GONE);
            } else {
                followButton.setVisibility(View.VISIBLE);
                updateFollowButtonState(participant);

                // Set follow button click listener
                followButton.setOnClickListener(v -> {
                    if (followButton.getText().toString().equals("Follow")) {
                        listener.onFollowClick(participant);
                        // Update button immediately for better UX
                        followButton.setText("Requested");
                        followButton.setEnabled(false);
                    }
                });
            }

            usernameText.setOnClickListener(v -> {
                listener.onUserClick(participant);
            });
        }

        /**
         * Changes what is displayed on follow button based on whether they are not followed, followed, or requested
         * Takes in selected user
         * @param participant
         */
        private void updateFollowButtonState(Participant participant) {
            // Check if already following
            participantRepository.isFollowing(currentUsername, participant.getUsername(), isFollowing -> {
                if (isFollowing) {
                    followButton.setText("Following");
                    followButton.setEnabled(false);
                } else {
                    // Check if a follow request exists
                    participantRepository.checkFollowRequestExists(currentUsername, participant.getUsername(), requestExists -> {
                        if (requestExists) {
                            followButton.setText("Requested");
                            followButton.setEnabled(false);
                        } else {
                            followButton.setText("Follow");
                            followButton.setEnabled(true);
                        }
                    }, e -> {
                        // Default to Follow if error
                        followButton.setText("Follow");
                        followButton.setEnabled(true);
                    });
                }
            }, e -> {
                // Default to Follow if error
                followButton.setText("Follow");
                followButton.setEnabled(true);
            });
        }
    }
}