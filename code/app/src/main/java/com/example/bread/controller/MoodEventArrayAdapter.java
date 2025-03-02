package com.example.bread.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class MoodEventArrayAdapter extends ArrayAdapter<MoodEvent> {
    private Context context;
    private ArrayList<MoodEvent> events;
    private FirebaseAuth mAuth;
    public MoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, 0, events);
        this.context = context;
        this.events = events;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
//        View view = convertView;
//        if (view == null){
//            view = LayoutInflater.from(context).inflate(R.layout.layout_event, parent, false);
//        }
//        MoodEvent event = events.get(position);
//        TextView username = view.findViewById(R.id.username);
//        TextView reason = view.findViewById(R.id.reason);
//        TextView date = view.findViewById(R.id.date);
//        ImageView profile = view.findViewById(R.id.profilePic);
//
//        username.setText();
//        reason.setText(event.getReason());
////        reason.setText(event.getTimestamp());
//
//
//        return view;
        return super.getView(position, convertView, parent);
    }
}
