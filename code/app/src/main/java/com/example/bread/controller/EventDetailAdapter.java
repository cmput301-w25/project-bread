package com.example.bread.controller;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.model.Comment;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.utils.ImageHandler;
import com.example.bread.utils.TimestampUtils;

import java.util.List;

public class EventDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "EventDetailAdapter";
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_COMMENT = 1;

    private final MoodEvent event;
    private final List<Comment> comments;
    private static ParticipantRepository participantRepository;

    public EventDetailAdapter(MoodEvent event, List<Comment> comments, ParticipantRepository participantRepository) {
        this.event = event;
        this.comments = comments;
        EventDetailAdapter.participantRepository = participantRepository;
    }

    @Override
    public int getItemCount() {
        // +1 for the header
        return comments.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_HEADER : VIEW_TYPE_COMMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.event_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.comment_item, parent, false);
            return new CommentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            ((HeaderViewHolder) holder).bind(event);
        } else {
            // Position - 1 because header occupies the first slot
            Comment comment = comments.get(position - 1);
            ((CommentViewHolder) holder).bind(comment);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, timestampText, eventTitle, emotionalStateText, socialSituationText, reasonText;
        ImageView profileImage, eventImage;
        ConstraintLayout cardLayout;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.username_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
            eventTitle = itemView.findViewById(R.id.event_title);
            emotionalStateText = itemView.findViewById(R.id.emotional_state_text);
            socialSituationText = itemView.findViewById(R.id.social_situation_text);
            reasonText = itemView.findViewById(R.id.reason_text);
            profileImage = itemView.findViewById(R.id.profile_image);
            eventImage = itemView.findViewById(R.id.event_image);
            cardLayout = itemView.findViewById(R.id.event_layout);
        }

        @SuppressLint("SetTextI18n")
        public void bind(MoodEvent event) {
            usernameText.setText(event.getParticipantRef().getId());
            eventTitle.setText(event.getTitle());
            timestampText.setText(TimestampUtils.transformTimestamp(event.getTimestamp()));
            emotionalStateText.setText(event.getEmotionalState().toString().toLowerCase() + " " + EmotionUtils.getEmoticon(event.getEmotionalState()));
            socialSituationText.setText(event.getSocialSituation().toString());
            reasonText.setText(event.getReason());
            if (event.getAttachedImage() != null) {
                eventImage.setImageBitmap(ImageHandler.base64ToBitmap(event.getAttachedImage()));
            }
            int colorResId = EmotionUtils.getColorResource(event.getEmotionalState());
            cardLayout.setBackgroundResource(colorResId);
            participantRepository.fetchParticipantByRef(event.getParticipantRef(), participant -> {
                if (participant.getProfilePicture() != null) {
                    profileImage.setImageBitmap(ImageHandler.base64ToBitmap(participant.getProfilePicture()));
                }
            }, e -> {
                // Handle errors if needed
                Log.e(TAG, "Error fetching participant", e);
            });
        }
    }

    // ViewHolder for comments
    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, timestampText, commentText;

        public CommentViewHolder(View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.comment_username);
            timestampText = itemView.findViewById(R.id.comment_timestamp);
            commentText = itemView.findViewById(R.id.comment_text);
        }

        public void bind(Comment comment) {
            usernameText.setText(comment.getParticipantRef().getId());
            timestampText.setText(TimestampUtils.transformTimestamp(comment.getTimestamp()));
            commentText.setText(comment.getText());
        }
    }
}

