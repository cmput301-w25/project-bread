package com.example.bread.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.LruCache;
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
import com.example.bread.model.Participant;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.utils.ImageHandler;

import java.util.ArrayList;

/**
 * Adapter class for the HomeFragment ListView
 */
public class HomeMoodEventArrayAdapter extends MoodEventArrayAdapter {

    // Cache for participants should be shared across instances
    private static final LruCache<String, Participant> participantCache = new LruCache<>(50);
    private final ParticipantRepository userRepo;

    public HomeMoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, events);
        userRepo = new ParticipantRepository();
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
        if (moodEvent == null) {
            return convertView;
        }

        // Set background color based on emotional state
        int colorResId = EmotionUtils.getColorResource(moodEvent.getEmotionalState());
        holder.eventLayout.setBackgroundResource(colorResId);

        // Set reason text
        holder.reason.setText(moodEvent.getReason() != null ? moodEvent.getReason() : "");

        // Format and set date
        if (moodEvent.getTimestamp() != null) {
            String formattedDate = formatTimeAgo(moodEvent.getTimestamp().getTime());
            holder.date.setText(formattedDate);
        } else {
            holder.date.setText("");
        }

        // Set emoticon
        holder.mood.setText(EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));

        // Set default profile image first
        holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);

        // Set default username while loading
        holder.username.setText("Loading...");

        // Load participant information from cache or network
        loadParticipantInfo(moodEvent, holder);

        // Set click listener
        convertView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMoodEventClick(moodEvent);
            }
        });

        return convertView;
    }

    /**
     * Loads participant information from cache or network
     *
     * @param moodEvent The mood event containing the participant reference
     * @param holder    The ViewHolder to update with participant data
     */
    private void loadParticipantInfo(MoodEvent moodEvent, ViewHolder holder) {
        if (moodEvent.getParticipantRef() == null) {
            holder.username.setText("Unknown");
            return;
        }

        String refPath = moodEvent.getParticipantRef().getPath();

        // Try to get from cache first
        Participant cachedParticipant = participantCache.get(refPath);
        if (cachedParticipant != null) {
            updateViewWithParticipant(holder, cachedParticipant);
            return;
        }

        // Not in cache, load from network (no need for a background thread since Firebase calls are async)
        userRepo.fetchParticipantByRef(moodEvent.getParticipantRef(), participant -> {
            // Cache the result
            if (participant != null) {
                participantCache.put(refPath, participant);
                updateViewWithParticipant(holder, participant);
            }
        }, e -> {
            // Handle error
            holder.username.setText("Unknown");
        });
    }

    /**
     * Updates view with participant data
     *
     * @param holder      The ViewHolder to update
     * @param participant The participant data
     */
    private void updateViewWithParticipant(ViewHolder holder, Participant participant) {
        holder.username.setText(participant.getUsername());

        // Set profile picture if available
        String base64Image = participant.getProfilePicture();
        if (base64Image != null) {
            holder.profilePic.setImageBitmap(ImageHandler.base64ToBitmap(base64Image));
        } else {
            holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);
        }
    }

    /**
     * Formats a timestamp as a relative time string (e.g., "5 minutes ago")
     *
     * @param timeMillis The time in milliseconds
     * @return A formatted relative time string
     */
    private String formatTimeAgo(long timeMillis) {
        CharSequence relativeTime = DateUtils.getRelativeDateTimeString(
                context,
                timeMillis,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0);

        // If very recent, simplify to "Just now"
        long diff = System.currentTimeMillis() - timeMillis;
        if (diff < DateUtils.MINUTE_IN_MILLIS) {
            return "Just now";
        }

        return relativeTime.toString();
    }
}