package com.example.bread.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.utils.ImageHandler;

import java.util.ArrayList;
import java.util.Date;

/**
 * Adapter class for the HomeFragment ListView
 */
public class HomeMoodEventArrayAdapter extends MoodEventArrayAdapter {
    public HomeMoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, events);
    }

    static class ViewHolder {
        TextView username;
        TextView reason;
        TextView date;
        TextView mood;
        ImageView profilePic;
        ConstraintLayout eventLayout;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_event_home, parent, false);
            holder = new ViewHolder();
            holder.username = convertView.findViewById(R.id.textUsername);
            holder.reason = convertView.findViewById(R.id.textReason);
            holder.date = convertView.findViewById(R.id.textDate);
            holder.mood = convertView.findViewById(R.id.textMood);
            holder.profilePic = convertView.findViewById(R.id.imageProfile);
            holder.eventLayout = convertView.findViewById(R.id.homeConstraintLayout);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MoodEvent moodEvent = getItem(position);
        ParticipantRepository userRepo = new ParticipantRepository();
        if (moodEvent != null) {
            int colorResId = EmotionUtils.getColorResource(moodEvent.getEmotionalState());
            holder.eventLayout.setBackgroundResource(colorResId);
            userRepo.fetchParticipantByRef(moodEvent.getParticipantRef(), participant -> {
                holder.username.setText(participant.getUsername());
                String base64Image = participant.getProfilePicture();
                if (base64Image != null) {
                    holder.profilePic.setImageBitmap(ImageHandler.base64ToBitmap(base64Image));
                } else {
                    holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);
                }
            }, e -> {
                holder.username.setText("Unknown");
                holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);
            });
            holder.reason.setText(moodEvent.getReason());
            holder.date.setText(transformTimestamp(moodEvent.getTimestamp()));
            holder.mood.setText(EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));

            convertView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMoodEventClick(moodEvent);
                }
            });
        }
        return convertView;
    }

    private String transformTimestamp(Date timestamp) {
        // Show hours ago if less than 24 hours, otherwise show how many days ago
        if (timestamp == null) {
            return "";
        }
        long diff = new Date().getTime() - timestamp.getTime();
        long hours = diff / (60 * 60 * 1000);
        if (hours < 24) {
            return hours + " hours ago";
        } else {
            long days = hours / 24;
            return days + " days ago";
        }
    }
}
