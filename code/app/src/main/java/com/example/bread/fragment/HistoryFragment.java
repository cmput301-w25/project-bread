package com.example.bread.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.example.bread.controller.MoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private ListView moodEventListView;
    private ArrayList<MoodEvent> moodEventArrayList;
    private ArrayAdapter<MoodEvent> moodArrayAdapter;

    private MoodEventRepository moodsRepo;
    private ParticipantRepository userRepo;

    private String username;
    private DocumentReference participantRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        moodEventListView = view.findViewById(R.id.historyListView);
        moodEventArrayList = new ArrayList<>();
        moodArrayAdapter = new MoodEventArrayAdapter(getContext(), moodEventArrayList);
        moodEventListView.setAdapter(moodArrayAdapter);

        moodsRepo = new MoodEventRepository();
        userRepo = new ParticipantRepository();

        fetchParticipantAndLoadEvents();

        return view;
    }

    //retrieves
    private void fetchParticipantAndLoadEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser(); //https://firebase.google.com/docs/auth/android/manage-users
        if (currentUser != null) {
            username = currentUser.getDisplayName();
        if (username == null) {
            Log.e("HistoryFragment", "Username is null. Cannot load mood events.");
            return;
        }
            participantRef = userRepo.getParticipantRef(username);
            loadMoodEvents();
        } else {
            Log.e("HistoryFragment", "No authenticated user found.");
        }
    }

    private void loadMoodEvents() {
        moodsRepo.listenForEventsWithParticipantRef(participantRef, moodEvents -> {
                    if (moodEvents != null){
                        moodEventArrayList.clear();
                        moodEventArrayList.addAll(moodEvents);
                        //chatGPT prompt "how can i sort an ArrayList of events by timestamp Date object"
                        moodEventArrayList.sort((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()));
                    }
                    moodArrayAdapter.notifyDataSetChanged();
                },
                error -> {
                    Log.e("MoodHistory", "Failed to listen for mood events", error);
        });
    }
}
