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

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_mood_event, parent, false);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkbox);
            holder.textView = convertView.findViewById(R.id.textViewMoodDetail);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MoodEvent event = getItem(position);
        holder.textView.setText(event.getTitle());
        holder.checkBox.setChecked(selectedEvents.contains(event));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedEvents.add(event);
            } else {
                selectedEvents.remove(event);
            }
        });

        return convertView;
    }

    static class ViewHolder {
        CheckBox checkBox;
        TextView textView;
    }

    public Set<MoodEvent> getSelectedEvents() {
        return selectedEvents;
    }
}
