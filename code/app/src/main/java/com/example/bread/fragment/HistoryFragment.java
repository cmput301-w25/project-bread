package com.example.bread.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private ListView moodEventListView;
    private ArrayList<MoodEvent> moodEventArrayList;
    private HistoryMoodEventArrayAdapter moodArrayAdapter;

    private MoodEventRepository moodsRepo;
    private ParticipantRepository userRepo;
    private Set<MoodEvent> selectedEvents = new HashSet<>();

    private String username;
    private DocumentReference participantRef;

    // Filter-related variables
    private FloatingActionButton filterButton;
    private ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    private boolean isFilteringByWeek = false;
    private MoodEvent.EmotionalState selectedEmotionalState = null;
    private String searchKeyword = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        // Add filter button click listener
        filterButton = view.findViewById(R.id.filter_button);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }

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
                Log.e(TAG, "Username is null. Cannot load mood events.");
                return;
            }
            participantRef = userRepo.getParticipantRef(username);
            loadMoodEvents();
        } else {
            Log.e(TAG, "No authenticated user found.");
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
                        //moodEventArrayList.addAll(moodEvents);
                        moodEvents.stream()
                                .filter(event -> event.getTimestamp() != null)
                                .forEach(moodEventArrayList::add);
                        //chatGPT prompt "how can i sort an ArrayList of events by timestamp Date object"
                        moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));

                        // Save all mood events for filtering
                        allMoodEvents.clear();
                        allMoodEvents.addAll(moodEventArrayList);

                        // Reapply any existing filters
                        if (isFilteringByWeek || selectedEmotionalState != null || !searchKeyword.isEmpty()) {
                            applyFilters();
                        }
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
        selectedEvents = ((HistoryMoodEventArrayAdapter) moodEventListView.getAdapter()).getSelectedEvents();
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
                    Log.d(TAG, "Fixed null ID: " + moodEvent.getId());
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
                                    Log.e(TAG, "Error updating mood", e);
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

    // Filter-related methods
    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_moods, null);
        builder.setView(dialogView);

        SwitchMaterial recentWeekSwitch = dialogView.findViewById(R.id.recent_week_switch);
        Spinner moodSpinner = dialogView.findViewById(R.id.mood_spinner);
        EditText keywordEditText = dialogView.findViewById(R.id.keyword_edit_text);
        Button applyButton = dialogView.findViewById(R.id.apply_button);
        Button resetButton = dialogView.findViewById(R.id.reset_button);

        List<String> moodOptions = new ArrayList<>();
        moodOptions.add("All Moods");
        for (MoodEvent.EmotionalState state : MoodEvent.EmotionalState.values()) {
            if (state != MoodEvent.EmotionalState.NONE) {
                moodOptions.add(state.toString());
            }
        }

        ArrayAdapter<String> moodAdapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_item,
                moodOptions
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                text.setPadding(16, 16, 16, 16);
                return view;
            }
        };

        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(moodAdapter);

        recentWeekSwitch.setChecked(isFilteringByWeek);
        if (selectedEmotionalState != null) {
            int position = moodOptions.indexOf(selectedEmotionalState.toString());
            if (position >= 0) {
                moodSpinner.setSelection(position);
            }
        }
        keywordEditText.setText(searchKeyword);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        applyButton.setOnClickListener(v -> {
            isFilteringByWeek = recentWeekSwitch.isChecked();

            int moodPosition = moodSpinner.getSelectedItemPosition();
            if (moodPosition > 0) {
                String selectedMood = moodOptions.get(moodPosition);
                selectedEmotionalState = MoodEvent.EmotionalState.valueOf(selectedMood);
            } else {
                selectedEmotionalState = null;
            }

            searchKeyword = keywordEditText.getText().toString().trim().toLowerCase();

            applyFilters();
            dialog.dismiss();
        });

        resetButton.setOnClickListener(v -> {
            recentWeekSwitch.setChecked(false);
            moodSpinner.setSelection(0);
            keywordEditText.setText("");

            isFilteringByWeek = false;
            selectedEmotionalState = null;
            searchKeyword = "";

            resetFilters();
            dialog.dismiss();
        });
    }

    private void applyFilters() {
        if (allMoodEvents.isEmpty() && !moodEventArrayList.isEmpty()) {
            allMoodEvents.addAll(moodEventArrayList);
        }

        ArrayList<MoodEvent> filteredList = new ArrayList<>(allMoodEvents);

        if (isFilteringByWeek) {
            filteredList = filterByRecentWeek(filteredList);
        }

        if (selectedEmotionalState != null) {
            filteredList = filterByEmotionalState(filteredList, selectedEmotionalState);
        }

        if (!searchKeyword.isEmpty()) {
            filteredList = filterByKeyword(filteredList, searchKeyword);
        }

        moodEventArrayList.clear();
        moodEventArrayList.addAll(filteredList);
        moodArrayAdapter.notifyDataSetChanged();

        if (filteredList.isEmpty() && (isFilteringByWeek || selectedEmotionalState != null || !searchKeyword.isEmpty())) {
            Toast.makeText(getContext(), "No mood events match the applied filters", Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayList<MoodEvent> filterByRecentWeek(ArrayList<MoodEvent> events) {
        ArrayList<MoodEvent> filteredList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date oneWeekAgo = calendar.getTime();

        for (MoodEvent event : events) {
            if (event.getTimestamp().after(oneWeekAgo)) {
                filteredList.add(event);
            }
        }

        return filteredList;
    }

    private ArrayList<MoodEvent> filterByEmotionalState(ArrayList<MoodEvent> events, MoodEvent.EmotionalState state) {
        ArrayList<MoodEvent> filteredList = new ArrayList<>();

        for (MoodEvent event : events) {
            if (event.getEmotionalState() == state) {
                filteredList.add(event);
            }
        }

        return filteredList;
    }

    private ArrayList<MoodEvent> filterByKeyword(ArrayList<MoodEvent> events, String keyword) {
        ArrayList<MoodEvent> filteredList = new ArrayList<>();

        for (MoodEvent event : events) {
            if (event.getReason() != null && event.getReason().toLowerCase().contains(keyword)) {
                filteredList.add(event);
            }
        }

        return filteredList;
    }

    private void resetFilters() {
        if (!allMoodEvents.isEmpty()) {
            moodEventArrayList.clear();
            moodEventArrayList.addAll(allMoodEvents);
            moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));
            moodArrayAdapter.notifyDataSetChanged();
        }
    }
}