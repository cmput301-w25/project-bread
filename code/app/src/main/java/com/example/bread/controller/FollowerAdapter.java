package com.example.bread.controller;

import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.model.Participant;
import com.example.bread.utils.ImageHandler;

import java.util.List;

/**
 * FollowerAdapter - Controller
 *
 * Role / Purpose:
 * Adapter that populates a RecyclerView list with followers or following profiles of current user
 * Handles profile picture, username, and unfollow button
 *
 * Design Pattern:
 * Adapter Pattern: Binds Participant data to the RecyclerView UI.
 * ViewHolder Pattern: Caches view lookups for smooth scrolling and better performance.
 * Listener Pattern: Uses a custom listener interface to decouple interaction logic.
 *
 * Outstanding Issues / Comments:
 * Currently assumes the user list and list type won't change dynamically after instantiation.
 */

public class FollowerAdapter extends RecyclerView.Adapter<FollowerAdapter.FollowerViewHolder> {

    private final List<Participant> userList;
    private final OnUserInteractionListener listener;
    private final String listType; // "followers" or "following"

    public FollowerAdapter(List<Participant> userList, OnUserInteractionListener listener, String listType) {
        this.userList = userList;
        this.listener = listener;
        this.listType = listType;
    }

    @NonNull
    @Override
    public FollowerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follower_with_remove, parent, false);
        return new FollowerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FollowerViewHolder holder, int position) {
        Participant participant = userList.get(position);
        holder.bind(participant);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * Listener interface for handling user interactions within a user-related adapter
     */
    public interface OnUserInteractionListener {
        void onUserClick(Participant participant);
        void onRemoveClick(Participant participant, int position);
    }

    /**
     * ViewHolder for follower details / actions (profile pic, username, unfollow)
     * Assigns / sets values to user information
     */
    class FollowerViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView usernameText, nameText;
        ImageView removeButton;

        public FollowerViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            usernameText = itemView.findViewById(R.id.username_text);
            nameText = itemView.findViewById(R.id.name_text);
            removeButton = itemView.findViewById(R.id.remove_button);

            // Set the appropriate button text based on list type
            String buttonType = listType.equals("followers") ? "Remove" : "Unfollow";

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(userList.get(position));
                }
            });

            removeButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRemoveClick(userList.get(position), position);
                }
            });
        }

        void bind(Participant participant) {
            usernameText.setText(participant.getUsername());
            usernameText.setPaintFlags(usernameText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            nameText.setText(participant.getFirstName() + " " + participant.getLastName());

            // Set profile image if available
            if (participant.getProfilePicture() != null) {
                profileImage.setImageBitmap(ImageHandler.base64ToBitmap(participant.getProfilePicture()));
            } else {
                profileImage.setImageResource(R.drawable.default_avatar);
            }
        }
    }
}