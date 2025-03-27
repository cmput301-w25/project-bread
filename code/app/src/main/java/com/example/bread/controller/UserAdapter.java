package com.example.bread.controller;

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

    public interface UserInteractionListener {
        void onFollowClick(Participant participant);
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView usernameText, nameText;
        Button followButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            usernameText = itemView.findViewById(R.id.username_text);
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
        }

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