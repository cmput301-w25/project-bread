package com.example.bread.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MoodHistory extends AppCompatActivity {
    private ListView moodEventListView;
    private ArrayList<MoodEvent> moodEventArrayList;
    private ArrayAdapter<MoodEvent> moodArrayAdapter;
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private MoodEventRepository moodsRepo;
    private ParticipantRepository userRepo;
    private List<MoodEvent> events;
    private String username;
    private DocumentReference participantRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_history);

        moodsRepo = new MoodEventRepository();
        userRepo = new ParticipantRepository();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser(); //retrieving current user, https://stackoverflow.com/questions/35112204/get-current-user-firebase-android
        if (currentUser != null) {
            username = currentUser.getDisplayName();
        }

        participantRef = userRepo.getParticipantRef(username);

        //fetching all mood events for the user and adding to moodEventArrayList
        moodsRepo.fetchEventsWithParticipantRef(
                participantRef, //participant ref
                moodEvents -> { //OnSuccessListener
                    if (moodEvents != null) {
                        Log.d("MoodHistory", "Retrieved " + moodEvents.size() + " mood events");
                        moodEventArrayList.clear();
                        moodEventArrayList.addAll(moodEvents);

                        //chatGPT prompt "how can i sort an ArrayList of events by timestamp Date object"
                        moodEventArrayList.sort((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()));

                        moodArrayAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("MoodHistory", "User has no mood events");
                    }
                }, e -> Log.e("MoodHistory", "Failed to fetch mood events"));
    };
}