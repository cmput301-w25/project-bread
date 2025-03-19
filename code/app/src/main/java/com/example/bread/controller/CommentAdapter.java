package com.example.bread.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.model.Comment;

import java.util.Date;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private final List<Comment> comments;

    public CommentAdapter(List<Comment> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.username.setText(comment.getParticipantRef().getId());
        holder.commentText.setText(comment.getText());
        holder.timestamp.setText(transformTimestamp(comment.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private String transformTimestamp(Date timestamp) {
        // Show hours ago if less than 24 hours, otherwise show how many days ago
        long diff = new Date().getTime() - timestamp.getTime();
        long hours = diff / (60 * 60 * 1000);
        if (hours < 24) {
            return hours + " hours ago";
        } else {
            long days = hours / 24;
            return days + " days ago";
        }
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public TextView commentText;
        public TextView timestamp;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.comment_username);
            commentText = itemView.findViewById(R.id.comment_text);
            timestamp = itemView.findViewById(R.id.comment_timestamp);
        }
    }
}
