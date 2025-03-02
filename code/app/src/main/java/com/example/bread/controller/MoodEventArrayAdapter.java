package com.example.bread.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bread.model.MoodEvent;
import com.example.bread.utils.EmotionUtils;
import com.example.bread.R;

import java.util.ArrayList;

public class MoodEventArrayAdapter extends ArrayAdapter<MoodEvent> {
    public MoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, 0, events);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.mood_event_list_item, parent, false);
        }


        MoodEvent moodEvent = getItem(position);

        if (moodEvent != null) {
            TextView emoticonTextView = convertView.findViewById(R.id.emoticon_text_view);
            TextView titleTextView = convertView.findViewById(R.id.title_text_view);
            emoticonTextView.setText(EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));

            titleTextView.setText(moodEvent.getTitle());


            int colorResId = EmotionUtils.getColorResource(moodEvent.getEmotionalState());
            convertView.setBackgroundResource(colorResId);
        }

        return convertView;
    }
}
