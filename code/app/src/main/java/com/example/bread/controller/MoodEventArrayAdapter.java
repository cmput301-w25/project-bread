package com.example.bread.controller;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.example.bread.model.MoodEvent;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

/**
 * MoodEventArrayAdapter - Controller
 * <p>
 * Role / Purpose
 * Abstract base adapter class for displaying MoodEvent items in a ListView.
 * Provides logic and variables for implementations like HomeMoodEventArrayAdapter and HistoryMoodEventArrayAdapter.
 * Supports click handling using a listener interface.
 * <p>
 * Design Patterns
 * Adapter Pattern: Inherits from ArrayAdapter to map MoodEvent data to list items.
 * Template Method Pattern: Designed for subclass extension with custom getView() logic.
 * Listener Pattern: Defines OnMoodEventClickListener for handling mood event clicks.
 * Singleton Pattern (indirect): Uses FirebaseAuth singleton for accessing the current user.
 * <p>
 * Outstanding Issues
 * Data list and click listener are exposed to subclasses without encapsulation.
 */

public abstract class MoodEventArrayAdapter extends ArrayAdapter<MoodEvent> {
    protected Context context;
    protected ArrayList<MoodEvent> events;
    protected FirebaseAuth mAuth;

    /**
     * Interface for handling click events on mood events
     */
    public interface OnMoodEventClickListener {
        void onMoodEventClick(MoodEvent moodEvent);
    }

    protected OnMoodEventClickListener clickListener;

    /**
     * Sets the click listener for mood events
     *
     * @param listener the listener to set
     */
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