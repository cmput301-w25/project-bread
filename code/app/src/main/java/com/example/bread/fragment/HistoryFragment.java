package com.example.bread.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import android.widget.Button;
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
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class HistoryFragment extends Fragment implements FilterMoodEventFragment.FilterMoodDialogListener{

    private ListView moodEventListView;
    private ArrayList<MoodEvent> moodEventArrayList;
    private ArrayAdapter<MoodEvent> moodArrayAdapter;

    private MoodEventRepository moodsRepo;
    private ParticipantRepository userRepo;
    private Set<MoodEvent> selectedEvents = new HashSet<>();

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

        Button deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        ImageButton filterButton = view.findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> {
            FilterMoodEventFragment filterFragment = new FilterMoodEventFragment(); //new instance of our filter dialog fragment
            filterFragment.setListener(this); //passing HistoryFragment as a listener to dialog fragment
            filterFragment.show(getParentFragmentManager(), "Filter Mood Events"); //displaying Filter Mood Events
        });

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

    //https://stackoverflow.com/questions/17210839/get-last-week-date-range-for-a-date-in-java

    /**
     * Retrieves a date range for the most recent week (7 days).
     * The range starts from 7 days ago and ends at the current date.
     *
     * @return An ArrayList containing two Date objects:
     *         the start date (7 days ago) and the end date (today).
     */
    public ArrayList<Date> getMostRecentWeek(){
        ArrayList<Date> weekRange = new ArrayList<Date>();
        Date date = new Date(); //gets current date
        Calendar c = Calendar.getInstance(); //sets calendar to today
        c.setTime(date);
        Date end = c.getTime();
        c.add(Calendar.DATE, -7);
        Date start = c.getTime();

        weekRange.add(start);
        weekRange.add(end);

        return weekRange;
    }

    /**
     * Filters the displayed mood events to show only those from the most recent week,
     * if the filter switch is enabled.
     * If the switch is off, all mood events are reloaded.
     *
     * @param isChecked True if the "Most Recent Week" filter is enabled, false otherwise.
     */
    @Override
    public void mostRecentWeek(boolean isChecked) {
        if (isChecked){
            moodsRepo.listenForEventsWithParticipantRef(participantRef, moodEvents -> {
                        if (moodEvents != null) {
                            //create functionality so it filters for moods in last 7 days
                            moodEventArrayList.clear();
                            ArrayList<Date> weekRange = getMostRecentWeek();
                            for (int i = 0; i < moodEvents.size(); i++){
                                if ((moodEvents.get(i).getTimestamp().before(weekRange.get(0))) //if mood is in last week
                                    &&
                                    (moodEvents.get(i).getTimestamp().after(weekRange.get(1)))){
                                    moodEventArrayList.add(moodEvents.get(i));
                                }
                            }
                            moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));
                        }
                        moodArrayAdapter.notifyDataSetChanged();
                    },
                    error -> {
                        Log.e("History Fragment", "Failed to listen for mood events", error);
                    });
        }
        else{ //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!??????? might mess with other filters? call it first?
            //call loadMoodEvents() to keep things the same
            loadMoodEvents();
        }

    }

    /**
     * Filters the displayed mood events to show only those with the specified emotional state.
     *
     * @param moodState The emotional state to filter by (e.g., HAPPY, SAD, etc.).
     */
    @Override
    public void filterByMood(MoodEvent.EmotionalState moodState) {
        moodsRepo.listenForEventsWithParticipantRef(participantRef, moodEvents -> {
                    if (moodEvents != null) {
                        //create functionality so it filters for moods in last 7 days
                        moodEventArrayList.clear();
                        for (int i = 0; i < moodEvents.size(); i++){
                            if (moodEvents.get(i).getEmotionalState() == moodState){
                                moodEventArrayList.add(moodEvents.get(i));
                            }
                        }
                        moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));
                    }
                    moodArrayAdapter.notifyDataSetChanged();
                },
                error -> {
                    Log.e("History Fragment", "Failed to listen for mood events", error);
                });
    }

    /**
     *
     * @param reason
     */
    @Override
    public void filterByReason(String reason) {
        // TODO: implement filtering by reason
        //use contains() function
    }
}