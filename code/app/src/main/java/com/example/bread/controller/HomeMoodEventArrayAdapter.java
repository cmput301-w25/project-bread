package com.example.bread.controller;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.bread.model.MoodEvent;

import java.util.ArrayList;

public class HomeMoodEventArrayAdapter extends MoodEventArrayAdapter {
    public HomeMoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, events);
    }
}
