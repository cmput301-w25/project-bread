package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
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
import com.example.bread.controller.MoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class HistoryFragment extends Fragment {

    private ListView moodEventListView;
    private ArrayList<MoodEvent> moodEventArrayList;
    private ArrayAdapter<MoodEvent> moodArrayAdapter;

    private MoodEventRepository moodsRepo;
    private ParticipantRepository userRepo;
    private ListView listView;
    private MoodEventArrayAdapter adapter;
    private Set<MoodEvent> selectedEvents = new HashSet<>();

    private String username;
    private DocumentReference participantRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) { //LANDYS
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        moodEventListView = view.findViewById(R.id.historyListView);
        moodEventArrayList = new ArrayList<>();
        moodArrayAdapter = new MoodEventArrayAdapter(getContext(), moodEventArrayList);
        moodEventListView.setAdapter(moodArrayAdapter);

        moodsRepo = new MoodEventRepository();
        userRepo = new ParticipantRepository();

        fetchParticipantAndLoadEvents();

        Button deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        return view;
    }

    //retrieves
    private void fetchParticipantAndLoadEvents() { //LANDYS
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

    private void loadMoodEvents() { //LANDYS
        moodsRepo.listenForEventsWithParticipantRef(participantRef, moodEvents -> {
                    if (moodEvents != null) {
                        moodEventArrayList.clear();
                        moodEventArrayList.addAll(moodEvents);
                        //chatGPT prompt "how can i sort an ArrayList of events by timestamp Date object"
                        moodEventArrayList.sort((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()));
                    }
                    moodArrayAdapter.notifyDataSetChanged();
                },
                error -> {
                    Log.e("History Fragment", "Failed to listen for mood events", error);
                });
        }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete the selected mood events?");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteSelectedMoodEvents());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteSelectedMoodEvents() {
        MoodEventRepository repository = new MoodEventRepository();
        for (MoodEvent event : selectedEvents) {
            repository.deleteMoodEvent(event, new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    getActivity().runOnUiThread(() -> {
                        moodArrayAdapter.remove(event);
                        moodArrayAdapter.notifyDataSetChanged();
                    });
                }
            }, e -> Toast.makeText(getContext(), "Error deleting event", Toast.LENGTH_SHORT).show());
        }
        selectedEvents.clear();  // Clear the selection after deletion
    }
}