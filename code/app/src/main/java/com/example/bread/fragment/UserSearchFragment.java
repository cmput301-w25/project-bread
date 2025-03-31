package com.example.bread.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.controller.UserAdapter;
import com.example.bread.model.Participant;
import com.example.bread.repository.ParticipantRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UserSearchFragment - Fragment
 *
 * Role / Purpose
 * Enables searching for participants by username and sending follow requests.
 * Filters out the current user and handles request status checking.
 *
 * Design Pattern
 * Fragment Pattern: Modular part of the screen.
 * Observer Pattern: Uses Firebase callbacks to reflect real-time changes.
 *
 * Outstanding Issues / Comments
 * No debouncing logic for search input; repeated queries may be sent quickly.
 */

public class UserSearchFragment extends Fragment implements UserAdapter.UserInteractionListener {

    private static final String TAG = "UserSearchFragment";

    private EditText searchEditText;
    private RecyclerView userRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;

    private UserAdapter userAdapter;
    private ParticipantRepository participantRepository;
    private String currentUsername;
    private List<Participant> userList = new ArrayList<>();
    private AtomicBoolean isSearching = new AtomicBoolean(false);

    private String usernameText;
    private UserProfileFragment userProfileFragment = new UserProfileFragment();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        participantRepository = new ParticipantRepository();

        // Get current username
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUsername = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_search, container, false);

        // Initialize views
        searchEditText = view.findViewById(R.id.search_edit_text);
        userRecyclerView = view.findViewById(R.id.user_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        // Set up RecyclerView
        userAdapter = new UserAdapter(userList, this);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userRecyclerView.setAdapter(userAdapter);

        // Set up search text watcher
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchUsers(query);
                } else {
                    // Clear results when search is cleared
                    userList.clear();
                    userAdapter.notifyDataSetChanged();
                    emptyView.setVisibility(View.GONE);
                    userRecyclerView.setVisibility(View.GONE);
                }
            }
        });

        return view;
    }

    private void searchUsers(String query) {
        if (isSearching.get()) {
            return; // Prevent multiple concurrent searches
        }

        isSearching.set(true);
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        participantRepository.searchUsersByUsername(query, participants -> {
            userList.clear();

            // Filter out the current user from results
            for (Participant participant : participants) {
                if (!participant.getUsername().toLowerCase().equals(currentUsername.toLowerCase())) {
                    userList.add(participant);
                }
            }

            userAdapter.notifyDataSetChanged();
            updateEmptyView();
            progressBar.setVisibility(View.GONE);
            isSearching.set(false);
        }, e -> {
            Log.e(TAG, "Error searching users", e);
            Toast.makeText(getContext(), "Error searching users", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            isSearching.set(false);
            updateEmptyView();
        });
    }

    private void updateEmptyView() {
        if (userList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            userRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            userRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFollowClick(Participant participant) {
        progressBar.setVisibility(View.VISIBLE);

        // First check if already following
        participantRepository.isFollowing(currentUsername, participant.getUsername(), isFollowing -> {
            if (isFollowing) {
                Toast.makeText(getContext(), "You are already following this user", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            // Then check if a follow request already exists
            participantRepository.checkFollowRequestExists(currentUsername, participant.getUsername(), requestExists -> {
                if (requestExists) {
                    Toast.makeText(getContext(), "Follow request already sent", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                // Send follow request
                participantRepository.sendFollowRequest(currentUsername, participant.getUsername(), unused -> {
                    Toast.makeText(getContext(), "Follow request sent", Toast.LENGTH_SHORT).show();
                    updateFollowButtonState(participant.getUsername());
                    progressBar.setVisibility(View.GONE);
                }, e -> {
                    Log.e(TAG, "Error sending follow request", e);
                    Toast.makeText(getContext(), "Error sending follow request", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }, e -> {
                Log.e(TAG, "Error checking follow request", e);
                Toast.makeText(getContext(), "Error checking follow status", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            });
        }, e -> {
            Log.e(TAG, "Error checking follow status", e);
            Toast.makeText(getContext(), "Error checking follow status", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }

    @Override
    public void onUserClick(Participant participant){
        // Navigate to user profile or other actions when clicking on a follower/following
        usernameText = participant.getUsername();

        if (Objects.equals(usernameText, currentUsername)){
            Toast.makeText(getContext(), "Tapped on your profile", Toast.LENGTH_SHORT).show();

            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                    R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
            );
            transaction.add(R.id.frame_layout, new ProfileFragment());
            transaction.commit();
        }
        else{
            Toast.makeText(getContext(), "Tapped on " + participant.getUsername(), Toast.LENGTH_SHORT).show();

            Bundle bundle = new Bundle();
            bundle.putString("text", usernameText);
            bundle.putSerializable("participant", participant);
            userProfileFragment.setArguments(bundle);

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out);
            transaction.replace(R.id.frame_layout, userProfileFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void updateFollowButtonState(String username) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getUsername().equals(username)) {
                userAdapter.notifyItemChanged(i);
                break;
            }
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
    }
}