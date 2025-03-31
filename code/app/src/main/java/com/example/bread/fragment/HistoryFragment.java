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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.bread.R;
import com.example.bread.controller.HistoryMoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * HistoryFragment - Fragment
 * <p>
 * Role / Purpose
 * Shows a list of the user's mood events with options to view, filter, and edit or delete them.
 * Supports weekly filtering and analytics navigation.
 * <p>
 * Design Pattern
 * Fragment Pattern: Modular screen element.
 * MVC Pattern: MoodEventRepository (Model), Adapter (Controller), Fragment (View).
 * <p>
 * Outstanding Issues / Comments
 * Filters reset on screen re-entry; state persistence may improve user experience.
 */

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private ArrayList<MoodEvent> moodEventArrayList;
    private HistoryMoodEventArrayAdapter moodArrayAdapter;
    private ListView moodEventListView;
    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;
    private Set<MoodEvent> selectedEvents = new HashSet<>();

    private String currentUsername;
    private DocumentReference participantRef;

    // Filter-related variables
    private FloatingActionButton filterButton;
    private final ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    private final ArrayList<MoodEvent> analyticsMoodEvents = new ArrayList<>();
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

        moodEventRepository = new MoodEventRepository();
        participantRepository = new ParticipantRepository();

        FloatingActionButton deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        ImageView chartButton = view.findViewById(R.id.chartButton);
        chartButton.setOnClickListener(v -> {
            if (moodEventArrayList.isEmpty()) {
                Toast.makeText(getContext(), "No mood events to display", Toast.LENGTH_SHORT).show();
            } else {
                List<MoodEvent> moodEvents = new ArrayList<>(analyticsMoodEvents);
                AnalyticsFragment fragment = AnalyticsFragment.newInstance(moodEvents);
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                        R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
                );
                transaction.add(R.id.frame_layout, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        // Add filter button click listener
        filterButton = view.findViewById(R.id.filter_button);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }
        moodArrayAdapter.setOnMoodEventClickListener(this::showMoodDetailsDialog);
        fetchParticipantAndLoadEvents();

        return view;
    }

    /**
     * Retrieves current user using FirebaseUser and uses to find participant ref.
     * Logs appropriate error messages if username null or user is not found.
     * Uses loadMoodEvents() to find mood events corresponding to user
     */
    private void fetchParticipantAndLoadEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUsername = currentUser.getDisplayName();
            if (currentUsername == null) {
                Log.e(TAG, "Username is null. Cannot load mood events.");
                return;
            }
            participantRef = participantRepository.getParticipantRef(currentUsername);
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
        moodEventRepository.fetchEventsWithParticipantRef(participantRef, moodEvents -> {
                    if (moodEvents != null) {
                        moodEventArrayList.clear();
                        moodEventArrayList.addAll(moodEvents);
                        moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));

                        // Save all mood events for filtering
                        allMoodEvents.clear();
                        allMoodEvents.addAll(moodEventArrayList);

                        // Save mood events for analytics
                        analyticsMoodEvents.clear();
                        analyticsMoodEvents.addAll(moodEventArrayList);

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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_mood, null);
        builder.setView(dialogView);

        Button deleteButton = dialogView.findViewById(R.id.delete_mood_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_delete_mood);

        AlertDialog alertDialog = builder.create();
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        alertDialog.show();

        deleteButton.setOnClickListener(v -> {
            deleteSelectedMoodEvents();
            alertDialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> alertDialog.dismiss());
    }

    /**
     * Deletes all currently selected mood events from Firestore.
     * Removes the events from the list and updates the adapter.
     * Displays a toast if any errors occur during deletion.
     */
    private void deleteSelectedMoodEvents() {
        MoodEventRepository repository = new MoodEventRepository();
        selectedEvents = ((HistoryMoodEventArrayAdapter) moodEventListView.getAdapter()).getSelectedEvents();

        int deleteCount = selectedEvents.size();

        for (MoodEvent event : selectedEvents) {
            // Remove the event from the list before syncing for the ui to update when user is offline
            moodEventArrayList.remove(event);
            allMoodEvents.remove(event);
            repository.deleteMoodEvent(event, new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    getActivity().runOnUiThread(() -> {
                        // This will run when Firebase sync happens, but UI is already updated
                        Log.d(TAG, "Mood event synced with Firebase: " + event.getId());
                    });
                }
            }, e -> Toast.makeText(getContext(), "Error deleting event", Toast.LENGTH_SHORT).show());
        }
        moodArrayAdapter.notifyDataSetChanged();
        Toast.makeText(getContext(), deleteCount + " event deleted", Toast.LENGTH_SHORT).show();// just for logcat check
        selectedEvents.clear();  // Clear the selection after deletion
    }

    // Filter-related methods
    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_history_filter_moods, null);
        builder.setView(dialogView);

        SwitchMaterial recentWeekSwitch = dialogView.findViewById(R.id.recent_week_switch);
        EditText keywordEditText = dialogView.findViewById(R.id.keyword_edit_text);
        Button applyButton = dialogView.findViewById(R.id.apply_button);
        Button resetButton = dialogView.findViewById(R.id.reset_button);
        Spinner emotionalStateText = dialogView.findViewById(R.id.emotional_state_text);

        recentWeekSwitch.setChecked(isFilteringByWeek);
        keywordEditText.setText(searchKeyword);

        Spinner emotionalStateSpinner = dialogView.findViewById(R.id.emotional_state_text);
        ArrayAdapter<MoodEvent.EmotionalState> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                MoodEvent.EmotionalState.values());
        emotionalStateSpinner.setAdapter(adapter);

        if (selectedEmotionalState != null) {
            emotionalStateSpinner.setSelection(adapter.getPosition(selectedEmotionalState));
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        applyButton.setOnClickListener(v -> {
            isFilteringByWeek = recentWeekSwitch.isChecked();
            selectedEmotionalState = (MoodEvent.EmotionalState) emotionalStateSpinner.getSelectedItem();
            searchKeyword = keywordEditText.getText().toString().trim().toLowerCase();
            applyFilters();
            dialog.dismiss();
        });

        resetButton.setOnClickListener(v -> {
            recentWeekSwitch.setChecked(false);
            emotionalStateSpinner.setSelection(0);
            keywordEditText.setText("");

            isFilteringByWeek = false;
            selectedEmotionalState = null;
            searchKeyword = "";

            resetFilters();
            dialog.dismiss();
        });
    }

    /**
     * Applies the currently selected filters to the mood events list.
     * Filters by recent week, emotional state, and keyword.
     * Updates the adapter and notifies the user if no matches are found.
     */
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

    /**
     * Filters a list of mood events to only include those from the past 7 days.
     *
     * @param events The list of mood events to filter.
     * @return A list of mood events that occurred within the past week.
     */
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

    /**
     * Filters a list of mood events to only include those with a matching emotional state.
     *
     * @param events The list of mood events to filter.
     * @param state The emotional state to match.
     * @return A list of mood events with the specified emotional state.
     */
    private ArrayList<MoodEvent> filterByEmotionalState(ArrayList<MoodEvent> events, MoodEvent.EmotionalState state) {
        ArrayList<MoodEvent> filteredList = new ArrayList<>();

        for (MoodEvent event : events) {
            if (event.getEmotionalState() == state) {
                filteredList.add(event);
            }
        }

        return filteredList;
    }

    /**
     * Filters a list of mood events to only include those whose reason field contains a given keyword.
     *
     * @param events The list of mood events to filter.
     * @param keyword The keyword to search for in the reason.
     * @return A list of mood events whose reason contains the keyword.
     */
    private ArrayList<MoodEvent> filterByKeyword(ArrayList<MoodEvent> events, String keyword) {
        ArrayList<MoodEvent> filteredList = new ArrayList<>();

        for (MoodEvent event : events) {
            if (event.getReason() != null && event.getReason().toLowerCase().contains(keyword)) {
                filteredList.add(event);
            }
        }

        return filteredList;
    }

    /**
     * Resets all filters and restores the full list of mood events.
     * Sorts the list by timestamp (most recent first).
     */
    private void resetFilters() {
        if (!allMoodEvents.isEmpty()) {
            moodEventArrayList.clear();
            moodEventArrayList.addAll(allMoodEvents);
            moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));
            moodArrayAdapter.notifyDataSetChanged();
        }
    }
}