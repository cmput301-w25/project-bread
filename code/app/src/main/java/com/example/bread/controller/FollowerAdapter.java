package com.example.bread.controller;

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

    public interface OnUserInteractionListener {
        void onUserClick(Participant participant);
        void onRemoveClick(Participant participant, int position);
    }

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