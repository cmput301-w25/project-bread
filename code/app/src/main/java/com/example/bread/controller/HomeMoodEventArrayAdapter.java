\package com.example.bread.controller;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.example.bread.utils.TimestampUtils;

import java.util.ArrayList;

/**
 * Adapter class for the HomeFragment ListView
 */
public class HomeMoodEventArrayAdapter extends MoodEventArrayAdapter {

    // Cache for participants should be shared across instances to address the senior's comment
    private static final LruCache<String, Participant> participantCache = new LruCache<>(50);
    private final ParticipantRepository userRepo;

    public HomeMoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, events);
        userRepo = new ParticipantRepository();
    }

    static class ViewHolder {
        TextView username;
        TextView title;
        TextView date;
        TextView mood;
        TextView socialSituation;
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
            holder.title = convertView.findViewById(R.id.textTitle);
            holder.date = convertView.findViewById(R.id.textDate);
            holder.mood = convertView.findViewById(R.id.textMood);
            holder.profilePic = convertView.findViewById(R.id.profile_image_home);
            holder.eventLayout = convertView.findViewById(R.id.homeConstraintLayout);
            holder.socialSituation = convertView.findViewById(R.id.textSocialSituation);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MoodEvent moodEvent = getItem(position);
        if (moodEvent != null) {
            // Set background color based on emotional state
            int colorResId = EmotionUtils.getColorResource(moodEvent.getEmotionalState());
            holder.eventLayout.setBackgroundResource(colorResId);
            
            // Load participant info with caching (the improvement requested by senior)
            loadParticipantInfo(moodEvent, holder);
            
            // Set other mood event data
            holder.title.setText(moodEvent.getTitle());
            
            // Use TimestampUtils from main branch but handle null timestamps
            if (moodEvent.getTimestamp() != null) {
                holder.date.setText(TimestampUtils.transformTimestamp(moodEvent.getTimestamp()));
            } else {
                holder.date.setText(""); // Handle null timestamp case
            }
            
            holder.mood.setText(moodEvent.getEmotionalState().toString().toLowerCase() + " " + 
                                EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));
            
            // Handle social situation display
            if (moodEvent.getSocialSituation() != null && moodEvent.getSocialSituation() != MoodEvent.SocialSituation.NONE) {
                holder.socialSituation.setText(moodEvent.getSocialSituation().toString());
                holder.socialSituation.setVisibility(View.VISIBLE);
            } else {
                holder.socialSituation.setVisibility(View.INVISIBLE);
            }

            // Set click listener
            convertView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMoodEventClick(moodEvent);
                }
            });
        }

        return convertView;
    }

    /**
     * Loads participant information from cache or network
     * This method addresses the senior's comment about caching and avoiding
     * unnecessary network calls
     *
     * @param moodEvent The mood event containing the participant reference
     * @param holder    The ViewHolder to update with participant data
     */
    private void loadParticipantInfo(MoodEvent moodEvent, ViewHolder holder) {
        if (moodEvent.getParticipantRef() == null) {
            holder.username.setText("Unknown");
            holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);
            return;
        }

        String refPath = moodEvent.getParticipantRef().getPath();

        // Try to get from cache first
        Participant cachedParticipant = participantCache.get(refPath);
        if (cachedParticipant != null) {
            // Use cached data
            holder.username.setText(cachedParticipant.getUsername());
            
            // Set profile picture if available
            String base64Image = cachedParticipant.getProfilePicture();
            if (base64Image != null) {
                holder.profilePic.setImageBitmap(ImageHandler.base64ToBitmap(base64Image));
            } else {
                holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);
            }
            return;
        }

        // Set defaults while loading
        holder.username.setText("Loading...");
        holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);

        // Not in cache, load from network (Firebase calls are already async)
        userRepo.fetchParticipantByRef(moodEvent.getParticipantRef(), participant -> {
            if (participant != null) {
                // Cache the participant for future use
                participantCache.put(refPath, participant);
                
                // Update UI with participant data
                holder.username.setText(participant.getUsername());
                String base64Image = participant.getProfilePicture();
                if (base64Image != null) {
                    holder.profilePic.setImageBitmap(ImageHandler.base64ToBitmap(base64Image));
                }
            } else {
                holder.username.setText("Unknown");
            }
        }, e -> {
            holder.username.setText("Unknown");
        });
    }
}