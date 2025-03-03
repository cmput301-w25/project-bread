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
import com.google.firebase.auth.FirebaseUser;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MoodEventArrayAdapter extends ArrayAdapter<MoodEvent> {
    private Context context;
    private ArrayList<MoodEvent> events;
    private FirebaseAuth mAuth;
    private String participantUsername;

    public MoodEventArrayAdapter(@NonNull Context context, ArrayList<MoodEvent> events) {
        super(context, 0, events);
        this.context = context;
        this.events = events;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context).inflate(R.layout.layout_event, parent, false);
        }

        MoodEvent event = events.get(position);
        TextView username = view.findViewById(R.id.username);
        TextView reason = view.findViewById(R.id.reason);
        TextView date = view.findViewById(R.id.date);
        ImageView profile = view.findViewById(R.id.profilePic);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser(); //retrieving current user, https://stackoverflow.com/questions/35112204/get-current-user-firebase-android
        if (currentUser != null) {
            participantUsername = currentUser.getDisplayName();
        }
        username.setText(participantUsername);

        reason.setText(event.getReason());

        Date eventDate = event.getTimestamp(); //https://stackoverflow.com/questions/5683728/convert-java-util-date-to-string
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String s = formatter.format(eventDate);
        date.setText(s);

        profile.setImageResource(R.drawable.default_avatar);

        return view;
    }
}
