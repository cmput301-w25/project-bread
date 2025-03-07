package com.example.bread.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bread.R;
import com.example.bread.model.FollowRequest;
import com.example.bread.repository.ParticipantRepository;

import java.util.List;

public class FollowRequestAdapter extends ArrayAdapter<FollowRequest> {
    private ParticipantRepository repository = new ParticipantRepository();

    public FollowRequestAdapter(Context context, List<FollowRequest> requests) {
        super(context, 0, requests);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_follow_request, parent, false);
        }

        TextView senderName = convertView.findViewById(R.id.username);
        Button acceptButton = convertView.findViewById(R.id.buttonAccept);
        Button declineButton = convertView.findViewById(R.id.buttonDecline);

        FollowRequest request = getItem(position);
        senderName.setText(request.getSenderUsername());

        acceptButton.setOnClickListener(v -> {
            repository.acceptFollowRequest(request.getId(), request.getSenderId(), request.getReceiverId(),
                    aVoid -> {
                        remove(request); // Update UI on successful acceptance
                        notifyDataSetChanged();
                        Toast.makeText(getContext(), "Request accepted.", Toast.LENGTH_SHORT).show();
                    }, e -> {
                        Toast.makeText(getContext(), "Failed to accept request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        declineButton.setOnClickListener(v -> {
            repository.declineFollowRequest(request.getId(),
                    aVoid -> {
                        remove(request); // Update UI on successful decline
                        notifyDataSetChanged();
                        Toast.makeText(getContext(), "Request declined.", Toast.LENGTH_SHORT).show();
                    }, e -> {
                        Toast.makeText(getContext(), "Failed to decline request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        // TODO: Add unfollow button functionality
        // Example usage:
//        unfollowButton.setOnClickListener(v -> {
//            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//            String unfollowUserId = request.getSenderId(); // or getReceiverId based on context
//            repository.unfollowUser(currentUserId, unfollowUserId,
//                    aVoid -> Toast.makeText(getContext(), "Unfollowed successfully", Toast.LENGTH_SHORT).show(),
//                    e -> Toast.makeText(getContext(), "Failed to unfollow: " + e.getMessage(), Toast.LENGTH_LONG).show());
//        });


        return convertView;
    }

}
