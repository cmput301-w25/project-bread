package com.example.bread.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.example.bread.controller.HistoryMoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.MoodEvent.SocialSituation;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class HistoryFragment extends Fragment {

    private ListView moodEventListView;
    private ArrayList<MoodEvent> moodEventArrayList;
    private HistoryMoodEventArrayAdapter moodArrayAdapter;

    private MoodEventRepository moodsRepo;
    private ParticipantRepository userRepo;
    private final Set<MoodEvent> selectedEvents = new HashSet<>();

    private String username;
    private DocumentReference participantRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) { //LANDYS
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        moodEventListView = view.findViewById(R.id.historyListView);
        moodEventArrayList = new ArrayList<>();
        moodArrayAdapter = new HistoryMoodEventArrayAdapter(getContext(), moodEventArrayList);
        moodEventListView.setAdapter(moodArrayAdapter);

        // Set click listener for mood events
        moodArrayAdapter.setOnMoodEventClickListener(this::showMoodDetailsDialog);

        moodsRepo = new MoodEventRepository();
        userRepo = new ParticipantRepository();

        fetchParticipantAndLoadEvents();

        Button deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        return view;
    }

    /**
     * Retrieves current user using FirebaseUser and uses to find participant ref.
     * Logs appropriate error messages if username null or user is not found.
     * Uses loadMoodEvents() to find mood events corresponding to user
     */
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

    /**
     * Uses listenForEventsWithParticipantRef() from MoodEventRepository class
     * to actively retrieve mood events corresponding to user whenever added.
     * Adds/alters user mood events to moodEventArrayList whenever there are changes.
     * Sorts mood events by date and time added
     */
    private void loadMoodEvents() {
        moodsRepo.listenForEventsWithParticipantRef(participantRef, moodEvents -> {
                    if (moodEvents != null) {
                        moodEventArrayList.clear();
                        moodEventArrayList.addAll(moodEvents);
                        //chatGPT prompt "how can i sort an ArrayList of events by timestamp Date object"
                        moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));
                    }
                    moodArrayAdapter.notifyDataSetChanged();
                },
                error -> {
                    Log.e("History Fragment", "Failed to listen for mood events", error);
                });
    }

    /**
     * Displays a confirmation dialog asking the user if they want to delete the selected mood events.
     * If the user confirms, deletion is triggered.
     */
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete the selected mood events?");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteSelectedMoodEvents());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Deletes all currently selected mood events from Firestore.
     * Removes the events from the list and updates the adapter.
     * Displays a toast if any errors occur during deletion.
     */
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

    /**
     * Shows a dialog with the details of the selected mood event.
     *
     * @param moodEvent The mood event to show details for
     */
    private void showMoodDetailsDialog(MoodEvent moodEvent) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("View Mood");

        // Inflate a custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_mood_details, null);

        // Set up the views
        TextView emotionTextView = dialogView.findViewById(R.id.detail_emotion);
        TextView dateTextView = dialogView.findViewById(R.id.detail_date);
        TextView reasonTextView = dialogView.findViewById(R.id.detail_reason);
        TextView socialSituationTextView = dialogView.findViewById(R.id.detail_social_situation);

        // Set the data
        emotionTextView.setText(moodEvent.getEmotionalState().toString());

        // Format date
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy hh:mm a");
        String dateString = formatter.format(moodEvent.getTimestamp());
        dateTextView.setText(dateString);

        // Set reason
        reasonTextView.setText(moodEvent.getReason() != null ? moodEvent.getReason() : "No reason provided");

        // Set social situation
        SocialSituation situation = moodEvent.getSocialSituation();
        socialSituationTextView.setText(situation != null ? situation.toString() : "Not specified");

        builder.setView(dialogView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        // Add an Edit button
        builder.setNeutralButton("Edit", (dialog, which) -> {
            // TODO: Implement edit functionality in future updates
            Toast.makeText(getContext(), "Edit functionality to be implemented", Toast.LENGTH_SHORT).show();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}