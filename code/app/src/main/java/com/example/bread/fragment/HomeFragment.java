package com.example.bread.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.example.bread.controller.HistoryMoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.view.LoginPage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private ArrayList<MoodEvent> moodEventArrayList;
    private HistoryMoodEventArrayAdapter moodEventArrayAdapter;
    private MoodEventRepository moodEventRepository;
    private FirebaseAuth mAuth;


    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ListView moodEventListView = view.findViewById(R.id.homeListView);
        moodEventArrayList = new ArrayList<>();
        moodEventArrayAdapter = new HistoryMoodEventArrayAdapter(getContext(), moodEventArrayList);
        moodEventListView.setAdapter(moodEventArrayAdapter);

        mAuth = FirebaseAuth.getInstance();
        moodEventRepository = new MoodEventRepository();

        // Note: To use clicking functionality, when you implement the ListView and adapter later,
        // you'll need to add this line:
        // moodArrayAdapter.setOnMoodEventClickListener(this::showMoodDetailsDialog);
        fetchMoodEvents();
        return view;
    }

    private void fetchMoodEvents() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String username = user.getDisplayName();
            if (username != null) {
                moodEventRepository.listenForEventsFromFollowing(username, moodEvents -> {
                    moodEventArrayList.clear();
                    moodEventArrayList.addAll(moodEvents);
                    moodEventArrayAdapter.notifyDataSetChanged();
                }, e -> {
                    Log.e(TAG, "Failed to fetch mood events for user: " + username, e);
                    Toast.makeText(getContext(), "Failed to fetch mood events", Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            Log.e(TAG, "User is not logged in");
            Intent intent = new Intent(getContext(), LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}