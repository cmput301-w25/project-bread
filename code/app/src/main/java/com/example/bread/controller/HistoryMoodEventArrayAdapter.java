package com.example.bread.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.utils.TimestampUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * HistoryMoodEventArrayAdapter
 * <p>
 * Role / Purpose
 * ArrayAdapter that displays MoodEvent items in the HistoryFragment's ListView.
 * Extends MoodEventArrayAdapter to add support for event selection using checkboxes.
 * Displays emotional state, social situation, event title, date, and visibility (public/private).
 * <p>
 * Design Pattern
 * Adapter Pattern: Converts MoodEvent model objects into visual list items.
 * ViewHolder Pattern: Caches child view lookups to optimize performance during scrolling.
 * Observer Pattern (via listeners): Listens for checkbox changes to track selected events.
 * <p>
 * Outstanding Issues
 * Selection state is not preserved across configuration changes (e.g., screen rotation).
 */

public class HistoryMoodEventArrayAdapter extends MoodEventArrayAdapter {

    private final Set<MoodEvent> selectedEvents = new HashSet<>();

    public HistoryMoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, events);
    }

    static class ViewHolder {
        CheckBox toggleButton;
        TextView socialSituation;
        TextView date;
        TextView mood;
        TextView title;
        ImageView visibilityIcon;
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
            holder.toggleButton = convertView.findViewById(R.id.checkbox);
            holder.visibilityIcon = convertView.findViewById(R.id.visibility_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MoodEvent moodEvent = getItem(position);

        if (moodEvent != null) {
            int colorResId = EmotionUtils.getColorResource(moodEvent.getEmotionalState());
            holder.eventLayout.setBackgroundResource(colorResId);

            holder.date.setText(TimestampUtils.transformTimestamp(moodEvent.getTimestamp()));
            holder.title.setText(moodEvent.getTitle());
            holder.mood.setText(moodEvent.getEmotionalState().toString() + " " + EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));

            if (moodEvent.getSocialSituation() != null && moodEvent.getSocialSituation() != MoodEvent.SocialSituation.NONE) {
                holder.socialSituation.setText(moodEvent.getSocialSituation().toString());
                holder.socialSituation.setVisibility(View.VISIBLE);
                // Set the visibility icon's start constraint to be after the social situation
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.visibilityIcon.getLayoutParams();
                params.startToEnd = holder.socialSituation.getId();
                holder.visibilityIcon.setLayoutParams(params);
            } else {
                holder.socialSituation.setVisibility(View.INVISIBLE);
                // Set the visibility icon's start constraint to be after the mood text
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.visibilityIcon.getLayoutParams();
                params.startToEnd = holder.mood.getId();
                holder.visibilityIcon.setLayoutParams(params);
            }

            if (moodEvent.getVisibility() != null) {
                if (moodEvent.getVisibility().toString().equals("PUBLIC")) {
                    holder.visibilityIcon.setImageResource(R.drawable.ic_public); //Create ic_public.xml in drawable
                } else {
                    holder.visibilityIcon.setImageResource(R.drawable.ic_private); //create ic_private.xml in drawable
                }
                holder.visibilityIcon.setVisibility(View.VISIBLE);
            } else {
                holder.visibilityIcon.setVisibility(View.GONE);
            }

            holder.toggleButton.setChecked(selectedEvents.contains(moodEvent));
            holder.toggleButton.setOnClickListener(v -> {
                if (selectedEvents.contains(moodEvent)) {
                    selectedEvents.remove(moodEvent);
                } else {
                    selectedEvents.add(moodEvent);
                }
            });

            convertView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMoodEventClick(moodEvent);
                }
            });
        }
        return convertView;
    }

    /**
     * Getter method that returns selected event when user chooses one in the list
     *
     * @return Set<MoodEvent>
     */
    public Set<MoodEvent> getSelectedEvents() {
        return selectedEvents;
    }

}
