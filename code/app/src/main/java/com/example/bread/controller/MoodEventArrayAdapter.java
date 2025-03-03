package com.example.bread.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.utils.EmotionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MoodEventArrayAdapter extends ArrayAdapter<MoodEvent> {
    private ArrayList<MoodEvent> events;
    private Set<MoodEvent> selectedEvents = new HashSet<>();

    public MoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, 0, events);
        this.events = events;
    }

    static class ViewHolder {
        CheckBox checkBox;
        TextView emoticonTextView;
        TextView titleTextView;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_mood_event, parent, false);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkbox);
            holder.emoticonTextView = convertView.findViewById(R.id.emoticon_text_view);
            holder.titleTextView = convertView.findViewById(R.id.title_text_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MoodEvent moodEvent = getItem(position);
        if (moodEvent != null) {
            if (holder.emoticonTextView != null) {
                holder.emoticonTextView.setText(EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));
            }
            if (holder.titleTextView != null) {
                holder.titleTextView.setText(moodEvent.getTitle());
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
        }
        return convertView;
    }

    public Set<MoodEvent> getSelectedEvents() {
        return selectedEvents;
    }
}

