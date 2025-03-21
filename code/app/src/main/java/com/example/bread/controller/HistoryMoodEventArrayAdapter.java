package com.example.bread.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
        TextView moodText;
        TextView titleText;
        ConstraintLayout eventLayout;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_event, parent, false);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkbox);
            holder.socialSituation = convertView.findViewById(R.id.history_social_situation_text);
            holder.date = convertView.findViewById(R.id.date);
            holder.titleText = convertView.findViewById(R.id.history_title_text);
            holder.moodText = convertView.findViewById(R.id.textMood);
            holder.eventLayout = convertView.findViewById(R.id.historyConstraintLayout);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MoodEvent moodEvent = getItem(position);

        if (moodEvent != null) {
            if (moodEvent.getSocialSituation() != null && moodEvent.getSocialSituation() != MoodEvent.SocialSituation.NONE) {
                holder.socialSituation.setText(moodEvent.getSocialSituation().toString());
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
            int colorResId = EmotionUtils.getColorResource(moodEvent.getEmotionalState());
            holder.eventLayout.setBackgroundResource(colorResId);
            holder.date.setText(TimestampUtils.transformTimestamp(moodEvent.getTimestamp()));
            holder.titleText.setText(moodEvent.getTitle());
            holder.moodText.setText(moodEvent.getEmotionalState().toString() + " " + EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));

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
