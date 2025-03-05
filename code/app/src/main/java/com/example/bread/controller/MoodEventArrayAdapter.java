package com.example.bread.controller;

import android.content.Context;
import android.util.Log; // Added for logging
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.bread.utils.EmotionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MoodEventArrayAdapter extends ArrayAdapter<MoodEvent> {
    private static final String TAG = "MoodEventArrayAdapter"; // Log tag
    private Context context;
    private ArrayList<MoodEvent> events;
    private FirebaseAuth mAuth;
    private String participantUsername;
    private Set<MoodEvent> selectedEvents = new HashSet<>();

    public MoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, 0, events);
        this.context = context;
        this.events = events;
    }

    static class ViewHolder {
        CheckBox checkBox;
        TextView emoticonTextView;
        TextView username;
        TextView date;
        TextView reason;
        ImageView profilePic;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_event, parent, false);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkbox);
            holder.emoticonTextView = convertView.findViewById(R.id.emoticon_text_view);
            holder.username = convertView.findViewById(R.id.username);
            holder.date = convertView.findViewById(R.id.date);
            holder.reason = convertView.findViewById(R.id.reason);
            holder.profilePic = convertView.findViewById(R.id.profilePic);
            convertView.setTag(holder);
            Log.v(TAG, "Created new ViewHolder for position " + position);
        } else {
            holder = (ViewHolder) convertView.getTag();
            Log.v(TAG, "Reusing ViewHolder for position " + position);
        }

        MoodEvent moodEvent = getItem(position);
        Log.d(TAG, "Rendering MoodEvent at position " + position + ": " + (moodEvent != null ? moodEvent.toString() : "null"));

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser(); //retrieving current user, https://stackoverflow.com/questions/35112204/get-current-user-firebase-android
        if (currentUser != null) {
            participantUsername = currentUser.getDisplayName();
            Log.d(TAG, "Current user display name: " + participantUsername);
        } else {
            Log.w(TAG, "No current Firebase user found");
            participantUsername = "Unknown";
        }

        if (moodEvent != null) {
            if (holder.emoticonTextView != null) {
                String emoticon = EmotionUtils.getEmoticon(moodEvent.getEmotionalState());
                holder.emoticonTextView.setText(emoticon);
                Log.v(TAG, "Set emoticon: " + emoticon);
            }
            int colorResId = EmotionUtils.getColorResource(moodEvent.getEmotionalState());
            convertView.setBackgroundResource(colorResId);
            Log.v(TAG, "Set background color resource: " + colorResId);

            if (holder.checkBox != null) {
                holder.checkBox.setOnCheckedChangeListener(null);
                holder.checkBox.setChecked(selectedEvents.contains(moodEvent));
                holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedEvents.add(moodEvent);
                        Log.d(TAG, "Added to selected events: " + moodEvent.getTitle());
                    } else {
                        selectedEvents.remove(moodEvent);
                        Log.d(TAG, "Removed from selected events: " + moodEvent.getTitle());
                    }
                });
                Log.v(TAG, "Checkbox state set: " + holder.checkBox.isChecked());
            }

            if (holder.username != null) {
                holder.username.setText(participantUsername);
                Log.v(TAG, "Set username: " + participantUsername);
            }

            if (holder.date != null) { //https://stackoverflow.com/questions/5683728/convert-java-util-date-to-string
                Date eventDate = moodEvent.getTimestamp();
                Log.d(TAG, "Timestamp for " + moodEvent.getTitle() + ": " + (eventDate != null ? eventDate.getClass().getName() + " - " + eventDate.toString() : "null"));
                if (eventDate != null) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String formattedDate = formatter.format(eventDate);
                        holder.date.setText(formattedDate);
                        Log.v(TAG, "Formatted date: " + formattedDate);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Failed to format timestamp: " + e.getMessage(), e);
                        holder.date.setText("Invalid Date");
                    }
                } else {
                    Log.w(TAG, "Timestamp is null for " + moodEvent.getTitle());
                    holder.date.setText("No Date");
                }
            }

            if (holder.reason != null) {
                String reason = moodEvent.getReason() != null ? moodEvent.getReason() : "No Reason";
                holder.reason.setText(reason);
                Log.v(TAG, "Set reason: " + reason);
            }

            if (holder.profilePic != null) {
                holder.profilePic.setImageResource(R.drawable.default_avatar);
                Log.v(TAG, "Set default profile picture");
            }
        } else {
            Log.w(TAG, "MoodEvent is null at position " + position);
            if (holder.emoticonTextView != null) holder.emoticonTextView.setText("");
            if (holder.username != null) holder.username.setText("Unknown");
            if (holder.date != null) holder.date.setText("No Event");
            if (holder.reason != null) holder.reason.setText("");
            if (holder.profilePic != null) holder.profilePic.setImageResource(R.drawable.default_avatar);
        }

        return convertView;
    }

    public Set<MoodEvent> getSelectedEvents() {
        return selectedEvents;
    }
}

