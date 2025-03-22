package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Intent;
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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.bread.R;
import com.example.bread.controller.HomeMoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.view.LoginPage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Represents the home page of the app, where users can view mood events from users they follow.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private ArrayList<MoodEvent> moodEventArrayList;
    private HomeMoodEventArrayAdapter moodEventArrayAdapter;
    private MoodEventRepository moodEventRepository;
    private FirebaseAuth mAuth;

    private ListView moodEventListView;

    // Filter-related variables
    private FloatingActionButton filterButton;
    private final ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    private boolean isFilteringByWeek = false;
    private MoodEvent.EmotionalState selectedEmotionalState = null;
    private String searchKeyword = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        moodEventListView = view.findViewById(R.id.homeListView);
        moodEventArrayList = new ArrayList<>();
        moodEventArrayAdapter = new HomeMoodEventArrayAdapter(getContext(), moodEventArrayList);
        moodEventListView.setAdapter(moodEventArrayAdapter);

        mAuth = FirebaseAuth.getInstance();
        moodEventRepository = new MoodEventRepository();

        // Add filter button click listener
        filterButton = view.findViewById(R.id.filter_button_home);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }

        moodEventArrayAdapter.setOnMoodEventClickListener(this::showMoodDetailsDialog);
        fetchMoodEvents();
        return view;
    }

    private void fetchMoodEvents() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String username = user.getDisplayName();
            if (username != null) {
                moodEventRepository.fetchForEventsFromFollowing(username, moodEvents -> {
                    if (moodEvents != null) {
                        moodEventArrayList.clear();
                        moodEventArrayList.addAll(moodEvents);
                        moodEventArrayList.sort(Comparator.reverseOrder());

                        // Save all mood events for filtering
                        allMoodEvents.clear();
                        allMoodEvents.addAll(moodEventArrayList);

                        // Reapply any existing filters
                        if (isFilteringByWeek || selectedEmotionalState != null || !searchKeyword.isEmpty()) {
                            applyFilters();
                        } else {
                            moodEventArrayAdapter.notifyDataSetChanged();
                        }
                    }
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

    private void showMoodDetailsDialog(MoodEvent moodEvent) {
        EventDetail fragment = EventDetail.newInstance(moodEvent);
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
        );
        transaction.add(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
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
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
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
        moodEventArrayAdapter.notifyDataSetChanged();

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
            if (event.getTimestamp() != null && event.getTimestamp().after(oneWeekAgo)) {
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
            moodEventArrayList.sort(Comparator.reverseOrder());
            moodEventArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchMoodEvents();
    }
}