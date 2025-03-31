package com.example.bread.fragment;

import android.app.AlertDialog;
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
import com.example.bread.controller.FollowerAdapter;
import com.example.bread.model.Participant;
import com.example.bread.repository.ParticipantRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * FollowersListFragment - Fragment
 * <p>
 * Role / Purpose
 * Displays a list of followers or following users for a given participant.
 * Provides search and interaction features, such as unfollowing or removing a follower.
 * <p>
 * Design Pattern
 * Fragment Pattern: Modular UI component.
 * MVC Pattern: Uses ParticipantRepository (Model) and FollowerAdapter (Controller).
 * <p>
 * Outstanding Issues / Comments
 * User interaction logic is tightly coupled with UI update logic; potential for refactoring for separation of concerns.
 */


public class FollowersListFragment extends Fragment implements FollowerAdapter.OnUserInteractionListener {

    private static final String TAG = "FollowersListFragment";
    private static final String ARG_USERNAME = "username";
    private static final String ARG_TYPE = "type"; // "followers" or "following"

    private String username;
    private ParticipantRepository.ListType listType;

    private TextView titleTextView;
    private RecyclerView usersRecyclerView;
    private EditText searchEditText;
    private ProgressBar progressBar;
    private TextView emptyView;

    private final List<Participant> originalList = new ArrayList<>();
    private final List<Participant> filteredList = new ArrayList<>();
    private FollowerAdapter followerAdapter;
    private ParticipantRepository participantRepository;

    private String usernameText;
    private final UserProfileFragment userProfileFragment = new UserProfileFragment();

    public FollowersListFragment() {
        // Required empty public constructor
    }

    public static FollowersListFragment newInstance(String username, String type) {
        FollowersListFragment fragment = new FollowersListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        participantRepository = new ParticipantRepository();

        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME);
            String type = getArguments().getString(ARG_TYPE);
            listType = "followers".equals(type) ?
                    ParticipantRepository.ListType.FOLLOWERS :
                    ParticipantRepository.ListType.FOLLOWING;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_followers_list, container, false);

        // Initialize views
        titleTextView = view.findViewById(R.id.title_text);
        searchEditText = view.findViewById(R.id.search_edit_text);
        usersRecyclerView = view.findViewById(R.id.users_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        // Set title based on type
        String title = listType == ParticipantRepository.ListType.FOLLOWERS ? "Followers" : "Following";
        titleTextView.setText(title);

        // Set up RecyclerView
        String typeString = listType == ParticipantRepository.ListType.FOLLOWERS ? "followers" : "following";
        followerAdapter = new FollowerAdapter(filteredList, this, typeString);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        usersRecyclerView.setAdapter(followerAdapter);

        // Load data
        loadData();

        // Set up search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterList(s.toString().toLowerCase().trim());
            }
        });

        return view;
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        if (listType == ParticipantRepository.ListType.FOLLOWERS) {
            participantRepository.fetchFollowers(username, followers -> {
                loadParticipants(followers);
            }, e -> {
                Log.e(TAG, "Error fetching followers", e);
                Toast.makeText(getContext(), "Error loading followers", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                updateEmptyView();
            });
        } else { // type is "following"
            participantRepository.fetchFollowing(username, following -> {
                loadParticipants(following);
            }, e -> {
                Log.e(TAG, "Error fetching following", e);
                Toast.makeText(getContext(), "Error loading following", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                updateEmptyView();
            });
        }
    }

    private void loadParticipants(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            updateEmptyView();
            return;
        }

        originalList.clear();
        filteredList.clear();

        final int[] count = {usernames.size()};

        for (String username : usernames) {
            participantRepository.fetchBaseParticipant(username, participant -> {
                if (participant != null) {
                    originalList.add(participant);
                    filteredList.add(participant);
                }

                count[0]--;
                if (count[0] <= 0) {
                    // All participants loaded
                    followerAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                }
            }, e -> {
                Log.e(TAG, "Error fetching participant: " + username, e);

                count[0]--;
                if (count[0] <= 0) {
                    progressBar.setVisibility(View.GONE);
                    updateEmptyView();
                }
            });
        }
    }

    private void filterList(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            for (Participant participant : originalList) {
                if (participant.getUsername().toLowerCase().contains(query) ||
                        participant.getDisplayName().toLowerCase().contains(query)) {
                    filteredList.add(participant);
                }
            }
        }

        followerAdapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredList.isEmpty()) {
            String message = listType == ParticipantRepository.ListType.FOLLOWERS ?
                    "No followers found" : "Not following anyone";
            emptyView.setText(message);
            emptyView.setVisibility(View.VISIBLE);
            usersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            usersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onUserClick(Participant participant) {
        // Navigate to user profile or other actions when clicking on a follower/following
        usernameText = participant.getUsername();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUsername = currentUser.getDisplayName();

        if (Objects.equals(usernameText, currentUsername)) {
            Toast.makeText(getContext(), "Tapped on your profile", Toast.LENGTH_SHORT).show();
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                    R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
            );
            transaction.add(R.id.frame_layout, new ProfileFragment());
            transaction.commit();
        } else {
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

    @Override
    public void onRemoveClick(Participant participant, int position) {
        // Show confirmation dialog
        String action = listType == ParticipantRepository.ListType.FOLLOWERS ? "remove" : "unfollow";
        String message = listType == ParticipantRepository.ListType.FOLLOWERS
                ? "Are you sure you want to remove " + participant.getUsername() + " from your followers?"
                : "Are you sure you want to unfollow " + participant.getUsername() + "?";

        new AlertDialog.Builder(getContext())
                .setTitle("Confirm " + action)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Perform the removal based on the list type
                    if (listType == ParticipantRepository.ListType.FOLLOWERS) {
                        removeFollower(participant, position);
                    } else {
                        unfollowUser(participant, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeFollower(Participant participant, int position) {
        progressBar.setVisibility(View.VISIBLE);

        participantRepository.removeFollower(username, participant.getUsername(), unused -> {
            // Remove from both lists and update UI
            if (position < filteredList.size()) {
                filteredList.remove(position);
                followerAdapter.notifyItemRemoved(position);
            }

            originalList.remove(participant);

            progressBar.setVisibility(View.GONE);
            updateEmptyView();

            Toast.makeText(getContext(), participant.getUsername() + " has been removed from your followers", Toast.LENGTH_SHORT).show();
        }, e -> {
            Log.e(TAG, "Error removing follower", e);
            Toast.makeText(getContext(), "Error removing follower", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }

    private void unfollowUser(Participant participant, int position) {
        progressBar.setVisibility(View.VISIBLE);

        participantRepository.unfollowUser(username, participant.getUsername(), unused -> {
            // Remove from both lists and update UI
            if (position < filteredList.size()) {
                filteredList.remove(position);
                followerAdapter.notifyItemRemoved(position);
            }

            originalList.remove(participant);

            progressBar.setVisibility(View.GONE);
            updateEmptyView();

            Toast.makeText(getContext(), "You have unfollowed " + participant.getUsername(), Toast.LENGTH_SHORT).show();
        }, e -> {
            Log.e(TAG, "Error unfollowing user", e);
            Toast.makeText(getContext(), "Error unfollowing user", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }
}