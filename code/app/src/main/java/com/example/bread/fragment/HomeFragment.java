package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.controller.HomeMoodEventArrayAdapter;
import com.example.bread.controller.UserAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.Participant;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.view.LoginPage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeFragment extends Fragment implements UserAdapter.UserInteractionListener {

    private static final String TAG = "HomeFragment";

    // Mood events section
    private ListView moodEventListView;
    private ArrayList<MoodEvent> moodEventArrayList;
    private HomeMoodEventArrayAdapter moodEventArrayAdapter;
    private ProgressBar moodsLoadingIndicator;
    private TextView emptyMoodsView;

    // Search section
    private EditText searchEditText;
    private RecyclerView userRecyclerView;
    private ProgressBar searchProgressBar;
    private TextView searchEmptyView;
    private FloatingActionButton searchButton;
    private View searchContainer;

    // Repositories
    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;

    // User search
    private UserAdapter userAdapter;
    private List<Participant> userList = new ArrayList<>();
    private AtomicBoolean isSearching = new AtomicBoolean(false);

    private FirebaseAuth mAuth;
    private String currentUsername;

    // Handler for delayed searches
    private Runnable searchRunnable;
    private final long SEARCH_DELAY_MS = 500;

    // Filter-related variables
    private FloatingActionButton filterButton;
    private ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    private boolean isFilteringByWeek = false;
    private MoodEvent.EmotionalState selectedEmotionalState = null;
    private String searchKeyword = "";

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize mood list
        moodEventListView = view.findViewById(R.id.homeListView);
        moodsLoadingIndicator = view.findViewById(R.id.moods_loading_indicator);
        emptyMoodsView = view.findViewById(R.id.empty_moods_view);

        moodEventArrayList = new ArrayList<>();
        moodEventArrayAdapter = new HomeMoodEventArrayAdapter(getContext(), moodEventArrayList);
        moodEventListView.setAdapter(moodEventArrayAdapter);

        // Initialize search views
        searchEditText = view.findViewById(R.id.search_edit_text);
        userRecyclerView = view.findViewById(R.id.user_recycler_view);
        searchProgressBar = view.findViewById(R.id.search_progress_bar);
        searchEmptyView = view.findViewById(R.id.search_empty_view);
        searchContainer = view.findViewById(R.id.search_container);

        // Find the search button
        searchButton = view.findViewById(R.id.search_button);

        // Ensure search container is initially hidden and properly configured
        if (searchContainer != null) {
            searchContainer.setVisibility(View.GONE);
        } else {
            Log.e(TAG, "Search container not found in layout");
        }

        // Initialize repositories
        mAuth = FirebaseAuth.getInstance();
        moodEventRepository = new MoodEventRepository();
        participantRepository = new ParticipantRepository();

        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUsername = user.getDisplayName();
        }

        // Setup search
        setupSearch();

        // Setup click listeners
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> {
                Log.d(TAG, "Search button clicked");
                if (searchContainer.getVisibility() == View.VISIBLE) {
                    hideSearchContainer();
                } else {
                    showSearchContainer();
                }
            });
        }

        // Add filter button click listener
        filterButton = view.findViewById(R.id.filter_button_home);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }

        // Set click listener for mood events
        moodEventArrayAdapter.setOnMoodEventClickListener(this::showMoodDetailsDialog);

        // Fetch mood events
        fetchMoodEvents();

        return view;
    }

    private void setupSearch() {
        // Set up RecyclerView for user search
        userAdapter = new UserAdapter(userList, this);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userRecyclerView.setAdapter(userAdapter);

        // Set up search text watcher with debounce
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();

                // Cancel any pending search
                if (searchRunnable != null) {
                    searchEditText.removeCallbacks(searchRunnable);
                }

                if (query.length() >= 2) {
                    // Show loading indicator
                    searchProgressBar.setVisibility(View.VISIBLE);
                    searchEmptyView.setVisibility(View.GONE);

                    // Create a new search with delay to debounce input
                    searchRunnable = () -> searchUsers(query);
                    searchEditText.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                } else {
                    // Clear results when search is cleared
                    clearSearchResults();
                }
            }
        });
    }

    private void clearSearchResults() {
        userList.clear();
        if (userAdapter != null) {
            userAdapter.notifyDataSetChanged();
        }
        searchEmptyView.setVisibility(View.GONE);
        userRecyclerView.setVisibility(View.GONE);
        searchProgressBar.setVisibility(View.GONE);
    }

    private void showSearchContainer() {
        if (searchContainer != null) {
            searchContainer.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();

            // Show keyboard
            try {
                InputMethodManager imm = (InputMethodManager)
                        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            } catch (Exception e) {
                Log.e(TAG, "Error showing keyboard", e);
            }
        }
    }

    private void hideSearchContainer() {
        if (searchContainer != null) {
            searchContainer.setVisibility(View.GONE);
            searchEditText.setText("");

            // Hide keyboard
            try {
                InputMethodManager imm = (InputMethodManager)
                        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                View currentFocus = requireActivity().getCurrentFocus();
                if (currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error hiding keyboard", e);
            }
        }
    }

    private void fetchMoodEvents() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String username = user.getDisplayName();
            if (username != null) {
                // Show loading state
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        moodsLoadingIndicator.setVisibility(View.VISIBLE);
                        emptyMoodsView.setVisibility(View.GONE);
                        moodEventListView.setVisibility(View.GONE);
                    });
                }

                moodEventRepository.listenForEventsFromFollowing(username, moodEvents -> {
                    // Update on UI thread to prevent crashes
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            try {
                                if (moodEventArrayList != null) {
                                    moodEventArrayList.clear();

                                    // Add all events with valid timestamps
                                    ArrayList<MoodEvent> validEvents = new ArrayList<>();
                                    for (MoodEvent event : moodEvents) {
                                        if (event.getTimestamp() != null) {
                                            validEvents.add(event);
                                        }
                                    }

                                    // Sort by date (newest first)
                                    Collections.sort(validEvents);
                                    Collections.reverse(validEvents);

                                    // Update the list
                                    moodEventArrayList.addAll(validEvents);

                                    // Save all mood events for filtering
                                    allMoodEvents.clear();
                                    allMoodEvents.addAll(moodEventArrayList);

                                    // Reapply any existing filters
                                    if (isFilteringByWeek || selectedEmotionalState != null || !searchKeyword.isEmpty()) {
                                        applyFilters();
                                    } else {
                                        if (moodEventArrayAdapter != null) {
                                            moodEventArrayAdapter.notifyDataSetChanged();
                                        }
                                    }

                                    // Hide loading indicator and show appropriate views
                                    moodsLoadingIndicator.setVisibility(View.GONE);

                                    if (moodEventArrayList.isEmpty()) {
                                        emptyMoodsView.setVisibility(View.VISIBLE);
                                        moodEventListView.setVisibility(View.GONE);
                                    } else {
                                        emptyMoodsView.setVisibility(View.GONE);
                                        moodEventListView.setVisibility(View.VISIBLE);
                                    }
                                }
                            } catch (Exception e) {
                                // Handle any exceptions during the UI update
                                Log.e(TAG, "Error updating UI with mood events", e);
                                moodsLoadingIndicator.setVisibility(View.GONE);

                                // Only show error if we couldn't load any events
                                if (moodEventArrayList.isEmpty()) {
                                    Toast.makeText(getContext(), "Failed to load mood events", Toast.LENGTH_SHORT).show();
                                    emptyMoodsView.setVisibility(View.VISIBLE);
                                    moodEventListView.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                }, e -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.e(TAG, "Failed to fetch mood events for user: " + username, e);

                            // Only show error toast if we have no existing events
                            if (moodEventArrayList.isEmpty()) {
                                Toast.makeText(getContext(), "Failed to fetch mood events", Toast.LENGTH_SHORT).show();
                            }

                            // Hide loading indicator and show empty view on error
                            moodsLoadingIndicator.setVisibility(View.GONE);
                            if (moodEventArrayList.isEmpty()) {
                                emptyMoodsView.setVisibility(View.VISIBLE);
                                moodEventListView.setVisibility(View.GONE);
                            } else {
                                emptyMoodsView.setVisibility(View.GONE);
                                moodEventListView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }
        } else {
            Log.e(TAG, "User is not logged in");
            Intent intent = new Intent(getContext(), LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    private void searchUsers(String query) {
        if (isSearching.get() || getContext() == null) {
            return; // Prevent multiple concurrent searches or searches after fragment is detached
        }

        isSearching.set(true);
        searchProgressBar.setVisibility(View.VISIBLE);
        searchEmptyView.setVisibility(View.GONE);

        try {
            participantRepository.searchUsersByUsername(query, participants -> {
                // Update UI on the main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            userList.clear();

                            // Filter out the current user from results
                            if (participants != null) {
                                for (Participant participant : participants) {
                                    if (participant != null && participant.getUsername() != null &&
                                            currentUsername != null &&
                                            !participant.getUsername().toLowerCase().equals(currentUsername.toLowerCase())) {
                                        userList.add(participant);
                                    }
                                }
                            }

                            if (userAdapter != null) {
                                userAdapter.notifyDataSetChanged();
                            }
                            updateSearchEmptyView();
                            searchProgressBar.setVisibility(View.GONE);
                            isSearching.set(false);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating search results UI", e);
                            searchProgressBar.setVisibility(View.GONE);
                            isSearching.set(false);
                        }
                    });
                } else {
                    isSearching.set(false);
                }
            }, e -> {
                // Handle error on the main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Error searching users", e);
                        Toast.makeText(getContext(), "Error searching users", Toast.LENGTH_SHORT).show();
                        searchProgressBar.setVisibility(View.GONE);
                        isSearching.set(false);
                        updateSearchEmptyView();
                    });
                } else {
                    isSearching.set(false);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception during search", e);
            searchProgressBar.setVisibility(View.GONE);
            isSearching.set(false);
            updateSearchEmptyView();
        }
    }

    private void updateSearchEmptyView() {
        if (userList.isEmpty() && searchEditText.getText().length() >= 2) {
            searchEmptyView.setVisibility(View.VISIBLE);
            userRecyclerView.setVisibility(View.GONE);
        } else if (userList.isEmpty()) {
            searchEmptyView.setVisibility(View.GONE);
            userRecyclerView.setVisibility(View.GONE);
        } else {
            searchEmptyView.setVisibility(View.GONE);
            userRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showMoodDetailsDialog(MoodEvent moodEvent) {
        // TODO: launch a new fragment to show more details about the mood event
        Log.d(TAG, "Clicked on mood event: " + moodEvent);
    }

    @Override
    public void onFollowClick(Participant participant) {
        if (getContext() == null) return;

        searchProgressBar.setVisibility(View.VISIBLE);

        // First check if already following
        participantRepository.isFollowing(currentUsername, participant.getUsername(), isFollowing -> {
            if (isFollowing) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "You are already following this user", Toast.LENGTH_SHORT).show();
                        searchProgressBar.setVisibility(View.GONE);
                    });
                }
                return;
            }

            // Then check if a follow request already exists
            participantRepository.checkFollowRequestExists(currentUsername, participant.getUsername(), requestExists -> {
                if (requestExists) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Follow request already sent", Toast.LENGTH_SHORT).show();
                            searchProgressBar.setVisibility(View.GONE);
                        });
                    }
                    return;
                }

                // Send follow request
                participantRepository.sendFollowRequest(currentUsername, participant.getUsername(), unused -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Follow request sent", Toast.LENGTH_SHORT).show();
                            updateFollowButtonState(participant.getUsername());
                            searchProgressBar.setVisibility(View.GONE);
                        });
                    }
                }, e -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.e(TAG, "Error sending follow request", e);
                            Toast.makeText(getContext(), "Error sending follow request", Toast.LENGTH_SHORT).show();
                            searchProgressBar.setVisibility(View.GONE);
                        });
                    }
                });
            }, e -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Error checking follow request", e);
                        Toast.makeText(getContext(), "Error checking follow status", Toast.LENGTH_SHORT).show();
                        searchProgressBar.setVisibility(View.GONE);
                    });
                }
            });
        }, e -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Error checking follow status", e);
                    Toast.makeText(getContext(), "Error checking follow status", Toast.LENGTH_SHORT).show();
                    searchProgressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void updateFollowButtonState(String username) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getUsername().equals(username)) {
                userAdapter.notifyItemChanged(i);
                break;
            }
        }
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
        if (allMoodEvents.isEmpty()) {
            Log.w(TAG, "allMoodEvents was empty, copying current mood events list");
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

        Log.d(TAG, "Filtered list size: " + filteredList.size());

        moodEventArrayList.clear();
        moodEventArrayList.addAll(filteredList);

        if (moodEventArrayAdapter != null) {
            moodEventArrayAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "MoodEventArrayAdapter is null!");
        }

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
        Log.d(TAG, "Resetting filters...");
        Log.d(TAG, "All mood events size: " + allMoodEvents.size());

        if (!allMoodEvents.isEmpty()) {
            moodEventArrayList.clear();
            moodEventArrayList.addAll(allMoodEvents);
            moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));

            Log.d(TAG, "Mood events size after reset: " + moodEventArrayList.size());

            if (moodEventArrayAdapter != null) {
                moodEventArrayAdapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, "MoodEventArrayAdapter is null!");
            }
        } else {
            Log.e(TAG, "No events available to reset!");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Clear search when leaving fragment
        if (searchEditText != null) {
            searchEditText.setText("");
        }
        userList.clear();
        if (userAdapter != null) {
            userAdapter.notifyDataSetChanged();
        }
        if (searchContainer != null) {
            searchContainer.setVisibility(View.GONE);
        }
    }
}