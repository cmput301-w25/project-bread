package com.example.bread.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.utils.EmotionUtils;
import com.google.firebase.auth.FirebaseUser;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Adapter class for the HistoryFragment ListView
 */
public class HistoryMoodEventArrayAdapter extends MoodEventArrayAdapter {

    private final Set<MoodEvent> selectedEvents = new HashSet<>();
    private String participantUsername;

    public HistoryMoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, events);
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
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MoodEvent moodEvent = getItem(position);

        FirebaseUser currentUser = mAuth.getCurrentUser(); //retrieving current user, https://stackoverflow.com/questions/35112204/get-current-user-firebase-android
        if (currentUser != null) {
            participantUsername = currentUser.getDisplayName();
        }
        if (moodEvent != null) {
            if (holder.emoticonTextView != null) {
                holder.emoticonTextView.setText(EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));
            }
            int colorResId = EmotionUtils.getColorResource(moodEvent.getEmotionalState());
            convertView.setBackgroundResource(colorResId);
            if (holder.checkBox != null) {
                holder.checkBox.setOnCheckedChangeListener(null);
                holder.checkBox.setChecked(selectedEvents.contains(moodEvent));
                holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedEvents.add(moodEvent);
                    } else {
                        selectedEvents.remove(moodEvent);
                    }
                });
            }
            if (holder.username != null) {
                holder.username.setText(participantUsername);
            }
            if (holder.date != null) {
                Date eventDate = moodEvent.getTimestamp();
                if (eventDate != null) {
                    Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String s = formatter.format(eventDate);
                    holder.date.setText(s);
                } else {
                    holder.date.setText("Pending"); // Fallback for null timestamp
                }
            }
            if (holder.reason != null) {
                holder.reason.setText(moodEvent.getReason());
            }
            if (holder.profilePic != null) {
                holder.profilePic.setImageResource(R.drawable.default_avatar);
            }
            if (currentUser != null) {
                participantUsername = currentUser.getDisplayName();
            }

            // Add click listener for the item
            convertView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMoodEventClick(moodEvent);
                }
            });
        }
        return convertView;
    }

    public Set<MoodEvent> getSelectedEvents() {
        return selectedEvents;
    }

}
