package com.example.bread.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.utils.ImageHandler;
import com.example.bread.utils.TimestampUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Adapter class for the HistoryFragment ListView
 */
public class HistoryMoodEventArrayAdapter extends MoodEventArrayAdapter {

    private final Set<MoodEvent> selectedEvents = new HashSet<>();

    public HistoryMoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, events);
    }

    static class ViewHolder {
        CheckBox checkBox;
        TextView socialSituation;
        TextView date;
        TextView mood;
        TextView title;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_event_history, parent, false);
            holder = new ViewHolder();
            holder.title = convertView.findViewById(R.id.history_title_text);
            holder.date = convertView.findViewById(R.id.date);
            holder.mood = convertView.findViewById(R.id.textMood);
            holder.eventLayout = convertView.findViewById(R.id.historyConstraintLayout);
            holder.socialSituation = convertView.findViewById(R.id.history_social_situation_text);
            holder.checkBox = convertView.findViewById(R.id.checkbox);
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

            holder.date.setText(TimestampUtils.transformTimestamp(moodEvent.getTimestamp()));
            holder.title.setText(moodEvent.getTitle());
            holder.mood.setText(moodEvent.getEmotionalState().toString() + " " + EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));

            if (moodEvent.getSocialSituation() != null && moodEvent.getSocialSituation() != MoodEvent.SocialSituation.NONE) {
                holder.socialSituation.setText(moodEvent.getSocialSituation().toString());
                holder.socialSituation.setVisibility(View.VISIBLE);
            } else {
                holder.socialSituation.setVisibility(View.INVISIBLE);
            }
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
