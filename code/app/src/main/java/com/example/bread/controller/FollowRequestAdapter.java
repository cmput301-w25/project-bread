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
import com.example.bread.model.FollowRequest;
import com.example.bread.model.Participant;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;

import java.util.List;

public class FollowRequestAdapter extends RecyclerView.Adapter<FollowRequestAdapter.RequestViewHolder> {

    private final List<FollowRequest> requestsList;
    private final RequestActionListener listener;
    private final ParticipantRepository participantRepository;

    public FollowRequestAdapter(List<FollowRequest> requestsList, RequestActionListener listener, ParticipantRepository participantRepository) {
        this.requestsList = requestsList;
        this.listener = listener;
        this.participantRepository = participantRepository;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follow_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        FollowRequest request = requestsList.get(position);
        holder.bind(request, position);
    }

    @Override
    public int getItemCount() {
        return requestsList.size();
    }

    public interface RequestActionListener {
        void onAccept(String requestorUsername, int position);
        void onDecline(String requestorUsername, int position);
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView usernameText, nameText;
        Button acceptButton, declineButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            usernameText = itemView.findViewById(R.id.username_text);
            nameText = itemView.findViewById(R.id.name_text);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
        }

        void bind(FollowRequest request, int position) {
            String requestorUsername = request.getFromUsername();
            usernameText.setText(requestorUsername);

            // Load user details
            participantRepository.fetchBaseParticipant(requestorUsername, participant -> {
                if (participant != null) {
                    // Set full name
                    nameText.setText(participant.getDisplayName());

                    // Set profile image if available
                    if (participant.getProfilePicture() != null) {
                        profileImage.setImageBitmap(ImageHandler.base64ToBitmap(participant.getProfilePicture()));
                    } else {
                        profileImage.setImageResource(R.drawable.default_avatar);
                    }
                }
            }, null);

            // Set button listeners
            acceptButton.setOnClickListener(v -> listener.onAccept(requestorUsername, position));
            declineButton.setOnClickListener(v -> listener.onDecline(requestorUsername, position));
        }
    }
}