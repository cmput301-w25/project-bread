package com.example.bread.controller;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.example.bread.model.MoodEvent;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public abstract class MoodEventArrayAdapter extends ArrayAdapter<MoodEvent> {
    protected Context context;
    protected ArrayList<MoodEvent> events;
    protected FirebaseAuth mAuth;

    // Add interface for click listener
    public interface OnMoodEventClickListener {
        void onMoodEventClick(MoodEvent moodEvent);
    }

    protected OnMoodEventClickListener clickListener;

    public void setOnMoodEventClickListener(OnMoodEventClickListener listener) {
        this.clickListener = listener;
    }

    public MoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, 0, events);
        this.context = context;
        this.events = events;
        this.mAuth = FirebaseAuth.getInstance();
    }
}