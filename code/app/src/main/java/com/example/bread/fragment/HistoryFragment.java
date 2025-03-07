package com.example.bread.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.bread.R;
import com.example.bread.controller.MoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.MoodEvent.SocialSituation;
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
import java.util.Set;
import java.text.SimpleDateFormat;


public class HistoryFragment extends Fragment implements FilterMoodEventFragment.FilterMoodDialogListener{

    private ListView moodEventListView;
    private ArrayList<MoodEvent> moodEventArrayList;
    private MoodEventArrayAdapter moodArrayAdapter;

    private MoodEventRepository moodsRepo;
    private ParticipantRepository userRepo;
    private Set<MoodEvent> selectedEvents = new HashSet<>();

    private String username;
    private DocumentReference participantRef;

    //storing user filter choices and setting default values
    //----------------------------------------------------------------------------------------------------
    private boolean savedMostRecent = false;
    private MoodEvent.EmotionalState savedMoodState = null;
    private String savedReasonKeyword = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        moodEventListView = view.findViewById(R.id.historyListView);
        moodEventArrayList = new ArrayList<>();
        moodArrayAdapter = new MoodEventArrayAdapter(getContext(), moodEventArrayList);
        moodEventListView.setAdapter(moodArrayAdapter);

        //setting click listener for mood events, connects fragment to adapter
        moodArrayAdapter.setOnMoodEventClickListener(this::showMoodDetailsDialog);

        moodsRepo = new MoodEventRepository();
        userRepo = new ParticipantRepository();

        //initiates list of mood history events
        fetchParticipantAndLoadEvents();

        Button deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());


        ImageButton filterButton = view.findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> {
            FilterMoodEventFragment filterFragment = new FilterMoodEventFragment(); //new instance of our filter dialog fragment
            filterFragment.setListener(this); //passing HistoryFragment as a listener to dialog fragment
            //----------------------------------------------------------------------------------------------------
            //creating a bundle to pass data from HistoryFragment to FilterMoodEventFragment
            Bundle selections = new Bundle();
            selections.putBoolean("mostRecent", savedMostRecent);
            if (savedMoodState != null) {
                selections.putString("moodState", savedMoodState.name());
            }
            if (savedReasonKeyword != null) {
                selections.putString("reasonKeyword", savedReasonKeyword);
            }
            filterFragment.setArguments(selections); //passing current user selections to filterFragment as a bundle
            filterFragment.show(getParentFragmentManager(), "Filter Mood Events");

        });
        return view;
    }

    /**
     * saves users selected filter options and is called in FilterMoodEventFragment
     * @param mostRecent = boolean value where true = user selected most recent week and false = not only recent week
     * @param reason = reason keyword String user enters when searching for specific events
     * @param moodState = EmotionalState enum value that user selects from Spinner when searching for mood events
     */
    @Override
    public void saveFilterState(boolean mostRecent, String reason, MoodEvent.EmotionalState moodState) {
        savedMostRecent = mostRecent;
        savedReasonKeyword = reason;
        savedMoodState = moodState;
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
        selectedEvents = ((MoodEventArrayAdapter) moodEventListView.getAdapter()).getSelectedEvents();
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
     *
     * @param isChecked = boolean value where true = user wants moods from most recent week & false = user wants all moods not just recent week
     * @param moodState = EmotionalState enum (from MoodEvent) value that user picks if they want to filter by an emotional state
     * @param reason = String keyword(s) that user enters if they want to find a mood event with specific keywords in the reason
     */
    @Override
    public void applyingFilters(boolean isChecked, MoodEvent.EmotionalState moodState, String reason){
        moodsRepo.listenForEventsWithParticipantRef(participantRef, moodEvents -> {
                    moodEventArrayList.clear(); //clears movie array in the beginning
                    if (moodEvents != null) {
                        if (isChecked){ //if user chooses most recent week option
                            ArrayList<Date> weekRange = getMostRecentWeek();
                            for (int i = 0; i < moodEvents.size(); i++){
                                if ((moodEvents.get(i).getTimestamp().before(weekRange.get(0))) //if mood is in last week
                                        &&
                                        (moodEvents.get(i).getTimestamp().after(weekRange.get(1)))){
                                    moodEventArrayList.add(moodEvents.get(i));
                                }
                            }
                        }

                        if (moodState != null){ //if user enters a mood state from Spinner
                            for (int i = 0; i < moodEvents.size(); i++){
                                if (moodEvents.get(i).getEmotionalState() == moodState){
                                    moodEventArrayList.add(moodEvents.get(i));
                                }
                            }
                        }

                        if (reason != null && !reason.isEmpty()){ //if user enters reason keywords
                            for (int i = 0; i < moodEvents.size(); i++){
                                if (moodEvents.get(i).getReason().contains(reason)){
                                    moodEventArrayList.add(moodEvents.get(i));
                                }
                            }
                        }

                        else{ //if user does not enter any filters
                            loadMoodEvents();
                        }
                    }
                    moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));
                    moodArrayAdapter.notifyDataSetChanged();
                },
                error -> {
                    Log.e("History Fragment", "Failed to listen for mood events", error);
                });
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