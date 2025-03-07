package com.example.bread.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.bread.utils.EmotionUtils;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.HashSet;
import java.util.Set;

public class MoodEventArrayAdapter extends ArrayAdapter<MoodEvent> { //LANDYS
    private Context context;
    private ArrayList<MoodEvent> events;
    private FirebaseAuth mAuth;
    private String participantUsername;
    private Set<MoodEvent> selectedEvents = new HashSet<>();

    // Add interface for click listener
    public interface OnMoodEventClickListener {
        void onMoodEventClick(MoodEvent moodEvent);
    }

    private OnMoodEventClickListener clickListener;

    public void setOnMoodEventClickListener(OnMoodEventClickListener listener) {
        this.clickListener = listener;
    }

    public MoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, 0, events);
        this.context = context;
        this.events = events;
    }

    static class ViewHolder {
        CheckBox checkBox;
        TextView emoticonTextView;
        TextView username;
        TextView date;
        TextView reason;
        ImageView profilePic;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_event, parent, false);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkbox);
            holder.emoticonTextView = convertView.findViewById(R.id.emoticon_text_view);
            holder.username = convertView.findViewById(R.id.username);
            holder.date = convertView.findViewById(R.id.date);
            holder.reason = convertView.findViewById(R.id.reason);
            holder.profilePic = convertView.findViewById(R.id.profilePic);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MoodEvent moodEvent = getItem(position);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser(); //retrieving current user, https://stackoverflow.com/questions/35112204/get-current-user-firebase-android
        if (currentUser != null) {
            participantUsername = currentUser.getDisplayName();
        }
        if (moodEvent != null) {
            if (holder.emoticonTextView != null) {
                holder.emoticonTextView.setText(EmotionUtils.getEmoticon(moodEvent.getEmotionalState()));
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
            if (holder.username != null) {
                holder.username.setText(participantUsername);
            }
            if (holder.date != null) {
                Date eventDate = moodEvent.getTimestamp(); //https://stackoverflow.com/questions/5683728/convert-java-util-date-to-string
                Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String s = formatter.format(eventDate);
                holder.date.setText(s);
            }
            if (holder.reason != null) {
                holder.reason.setText(moodEvent.getReason());
            }
            if (holder.profilePic != null) {
                holder.profilePic.setImageResource(R.drawable.default_avatar);
            }
            if (currentUser != null) {
                participantUsername = currentUser.getDisplayName();
            }

            // Add click listener for the item
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
