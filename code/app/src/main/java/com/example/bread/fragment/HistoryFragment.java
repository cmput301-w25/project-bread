package com.example.bread.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.example.bread.controller.HistoryMoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.MoodEvent.EmotionalState;
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
//                        moodEvents.stream()
//                                .filter(event -> event.getTimestamp() != null)
//                                .forEach(moodEventArrayList::add);
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
            showEditMoodDialog(moodEvent);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Shows a dialog to edit the selected mood event.
     *
     * @param moodEvent The mood event to edit
     */
    private void showEditMoodDialog(MoodEvent moodEvent) {
        if (getContext() == null) return;


        // Check and try to fix the ID if it's null
        if (moodEvent.getId() == null) {
            // Try to find the mood event in our list that matches this one
            for (MoodEvent event : moodEventArrayList) {
                if (event.getTimestamp() != null && moodEvent.getTimestamp() != null &&
                        event.getTimestamp().equals(moodEvent.getTimestamp()) &&
                        event.getReason() != null && moodEvent.getReason() != null &&
                        event.getReason().equals(moodEvent.getReason())) {
                    // This is likely the same event, copy its ID
                    moodEvent.setId(event.getId());
                    Log.d("HistoryFragment", "Fixed null ID: " + moodEvent.getId());
                    break;
                }
            }

            // If still null, we can't edit
            if (moodEvent.getId() == null) {
                Toast.makeText(getContext(), "Cannot edit this mood: no ID available", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Mood");

        // Inflate a custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_mood, null);

        // Set up the spinners and EditText fields
        EditText titleEditText = dialogView.findViewById(R.id.edit_title);
        EditText reasonEditText = dialogView.findViewById(R.id.edit_reason);
        Spinner emotionSpinner = dialogView.findViewById(R.id.edit_emotion_spinner);
        Spinner socialSituationSpinner = dialogView.findViewById(R.id.edit_social_situation_spinner);

        // Set current title
        titleEditText.setText(moodEvent.getTitle() != null ? moodEvent.getTitle() : "");

        // Set up emotion spinner
        ArrayAdapter<EmotionalState> emotionAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                EmotionalState.values()
        );
        emotionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionSpinner.setAdapter(emotionAdapter);

        // Set current emotional state
        if (moodEvent.getEmotionalState() != null) {
            int emotionPosition = emotionAdapter.getPosition(moodEvent.getEmotionalState());
            emotionSpinner.setSelection(emotionPosition);
        }

        // Set current reason
        reasonEditText.setText(moodEvent.getReason() != null ? moodEvent.getReason() : "");

        // Set up social situation spinner
        ArrayAdapter<SocialSituation> socialAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                SocialSituation.values()
        );
        socialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(socialAdapter);

        // Set current social situation
        if (moodEvent.getSocialSituation() != null) {
            int socialPosition = socialAdapter.getPosition(moodEvent.getSocialSituation());
            socialSituationSpinner.setSelection(socialPosition);
        }

        // Set current trigger

        builder.setView(dialogView);

        // Add save button
        builder.setPositiveButton("Save", (dialog, which) -> {
            // Get updated values
            String newTitle = titleEditText.getText().toString().trim();
            EmotionalState newEmotionalState = (EmotionalState) emotionSpinner.getSelectedItem();
            String newReason = reasonEditText.getText().toString().trim();
            SocialSituation newSocialSituation = (SocialSituation) socialSituationSpinner.getSelectedItem();

            // Validate the mood ID
            if (moodEvent.getId() == null) {
                Toast.makeText(getContext(), "Cannot update: Mood has no ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update the mood event
            moodEvent.setTitle(newTitle);
            moodEvent.setEmotionalState(newEmotionalState);
            moodEvent.setReason(newReason);
            moodEvent.setSocialSituation(newSocialSituation);


            // Save to Firebase
            moodsRepo.updateMoodEvent(moodEvent,
                    aVoid -> {
                        // This callback might be running on a background thread
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Mood updated successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    },
                    e -> {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to update mood", Toast.LENGTH_SHORT).show();
                                    Log.e("HistoryFragment", "Error updating mood", e);
                                }
                            });
                        }
                    }
            );
        });

        // Add cancel button
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}