package com.example.bread.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
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

    // Static cache for participants shared across adapter instances
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
        ImageView moodImage;
        CardView miniImageHolder;
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
            holder.moodImage = convertView.findViewById(R.id.event_home_image);
            holder.miniImageHolder = convertView.findViewById(R.id.event_home_image_holder);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MoodEvent moodEvent = getItem(position);
        if (moodEvent != null) {
            int colorResId = EmotionUtils.getColorResource(moodEvent.getEmotionalState());
            holder.eventLayout.setBackgroundResource(colorResId);

            loadParticipantInfo(moodEvent, holder);

            // Handle mood event images - fix to properly hide the holder when no image
            if (moodEvent.getAttachedImage() != null && !moodEvent.getAttachedImage().isEmpty()) {
                Bitmap imageBitmap = ImageHandler.base64ToBitmap(moodEvent.getAttachedImage());
                if (imageBitmap != null) {
                    holder.moodImage.setImageBitmap(imageBitmap);
                    holder.moodImage.setVisibility(View.VISIBLE);
                    holder.miniImageHolder.setVisibility(View.VISIBLE);
                } else {
                    holder.moodImage.setVisibility(View.GONE);
                    holder.miniImageHolder.setVisibility(View.GONE);
                }
            } else {
                holder.moodImage.setVisibility(View.GONE);
                holder.miniImageHolder.setVisibility(View.GONE);
            }

            holder.title.setText(moodEvent.getTitle());
            holder.date.setText(TimestampUtils.transformTimestamp(moodEvent.getTimestamp()));
            holder.mood.setText(moodEvent.getEmotionalState().toString() + " " + EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));

            if (moodEvent.getSocialSituation() != null && moodEvent.getSocialSituation() != MoodEvent.SocialSituation.NONE) {
                holder.socialSituation.setText(moodEvent.getSocialSituation().toString());
                holder.socialSituation.setVisibility(View.VISIBLE);
            } else {
                holder.socialSituation.setVisibility(View.INVISIBLE);
            }

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
     * This method improves performance by caching participant data and avoiding
     * unnecessary network calls when the same participant data is needed multiple times
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

        Participant cachedParticipant = participantCache.get(refPath);
        if (cachedParticipant != null) {
            holder.username.setText(cachedParticipant.getUsername());

            String base64Image = cachedParticipant.getProfilePicture();
            if (base64Image != null && !base64Image.isEmpty()) {
                Bitmap bitmap = ImageHandler.base64ToBitmap(base64Image);
                if (bitmap != null) {
                    holder.profilePic.setImageBitmap(bitmap);
                } else {
                    holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);
                }
            } else {
                holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);
            }
            return;
        }

        holder.username.setText("Loading...");
        holder.profilePic.setImageResource(R.drawable.ic_baseline_profile_24);

        userRepo.fetchParticipantByRef(moodEvent.getParticipantRef(), participant -> {
            if (participant != null) {
                participantCache.put(refPath, participant);

                holder.username.setText(participant.getUsername());
                String base64Image = participant.getProfilePicture();
                if (base64Image != null && !base64Image.isEmpty()) {
                    Bitmap bitmap = ImageHandler.base64ToBitmap(base64Image);
                    if (bitmap != null) {
                        holder.profilePic.setImageBitmap(bitmap);
                    }
                }
            } else {
                holder.username.setText("Unknown");
            }
        }, e -> {
            holder.username.setText("Unknown");
        });
    }
}