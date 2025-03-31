package com.example.bread.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.controller.FollowRequestAdapter;
import com.example.bread.model.FollowRequest;
import com.example.bread.repository.ParticipantRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * FollowRequestsFragment - Fragment
 * <p>
 * Role / Purpose
 * Displays incoming follow requests for the current user and allows accepting or declining them.
 * Provides option to follow back upon acceptance.
 * <p>
 * Design Pattern
 * Fragment Pattern: Encapsulates UI logic for request handling.
 * Observer Pattern: Listens for follow request data changes.
 * <p>
 * Outstanding Issues / Comments
 * UI may briefly flicker on data reload due to full refresh; could benefit from a diff-based adapter update.
 */


public class FollowRequestsFragment extends Fragment implements FollowRequestAdapter.RequestActionListener {

    private static final String TAG = "FollowRequestsFragment";

    private RecyclerView requestsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;

    private FollowRequestAdapter requestAdapter;
    private ParticipantRepository participantRepository;
    private String currentUsername;
    private final List<FollowRequest> requestsList = new ArrayList<>();

    /**
     * Initialize the fragment, setting up the repository and retrieving current username.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        participantRepository = new ParticipantRepository();

        // Get current username
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUsername = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        }
    }

    /**
     * Create the fragment view, initialize UI components, and load data.
     *
     * @param inflater           Layout inflater to inflate the fragment layout
     * @param container          Parent view that the fragment UI attaches to
     * @param savedInstanceState Saved instance state for fragment recreation
     * @return The inflated fragment view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_requests, container, false);

        // Initialize views
        requestsRecyclerView = view.findViewById(R.id.requests_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        // Set up RecyclerView
        requestAdapter = new FollowRequestAdapter(requestsList, this, participantRepository);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsRecyclerView.setAdapter(requestAdapter);

        // Load follow requests
        loadFollowRequests();

        return view;
    }

    /**
     * Fetches follow requests from the repository and updates the UI.
     * Shows progress indicator during loading and handles empty state.
     */
    private void loadFollowRequests() {
        if (currentUsername == null || currentUsername.isEmpty()) {
            updateEmptyView();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        participantRepository.fetchFollowRequests(currentUsername, requests -> {
            requestsList.clear();
            requestsList.addAll(requests);
            requestAdapter.notifyDataSetChanged();

            progressBar.setVisibility(View.GONE);
            updateEmptyView();
        }, e -> {
            Log.e(TAG, "Error loading follow requests", e);
            Toast.makeText(getContext(), "Error loading follow requests", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            updateEmptyView();
        });
    }

    /**
     * Updates the visibility of UI elements based on whether there are follow requests.
     * Shows empty view when there are no requests, shows the RecyclerView otherwise.
     */
    private void updateEmptyView() {
        if (requestsList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            requestsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            requestsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Handles accepting a follow request.
     * Updates the database, refreshes the UI, and offers to follow back if appropriate.
     *
     * @param requestorUsername Username of the person who sent the follow request
     * @param position          Position of the request in the list
     */
    @Override
    public void onAccept(String requestorUsername, int position) {
        progressBar.setVisibility(View.VISIBLE);

        participantRepository.acceptFollowRequest(currentUsername, requestorUsername, unused -> {
            // First check if the current user is already following the requestor
            // or has a pending follow request to the requestor
            checkFollowRelationship(requestorUsername);
            if (getView() != null) {
                getView().post(() -> {
                    loadFollowRequests();
                });
            }
        }, e -> {
            if (getView() != null) {
                getView().post(() -> {
                    Log.e(TAG, "Error accepting follow request", e);
                    Toast.makeText(getContext(), "Error accepting follow request", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    /**
     * Check if the current user is already following or has a pending request to the requestor
     * before showing the follow back dialog
     */
    private void checkFollowRelationship(String requestorUsername) {
        // First check if already following
        participantRepository.isFollowing(currentUsername, requestorUsername, isAlreadyFollowing -> {
            if (isAlreadyFollowing) {
                // Already following this user, no need for follow back dialog
                Toast.makeText(getContext(), "Follow request accepted", Toast.LENGTH_SHORT).show();
                return;
            }

            // Then check if a follow request already exists from current user to requestor
            participantRepository.checkFollowRequestExists(currentUsername, requestorUsername, requestExists -> {
                if (requestExists) {
                    // A follow request already exists, no need for follow back dialog
                    Toast.makeText(getContext(), "Follow request accepted", Toast.LENGTH_SHORT).show();
                } else {
                    // No existing relationship, show follow back dialog
                    if (getContext() != null) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Follow Back")
                                .setMessage("Do you want to follow " + requestorUsername + " back?")
                                .setPositiveButton("Follow", (dialog, which) -> {
                                    sendFollowBackRequest(requestorUsername);
                                })
                                .setNegativeButton("Not Now", null)
                                .setCancelable(true)
                                .show();

                        Toast.makeText(getContext(), "Follow request accepted", Toast.LENGTH_SHORT).show();
                    }
                }
            }, e -> {
                // In case of error, still show toast for acceptance
                Toast.makeText(getContext(), "Follow request accepted", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error checking follow request status", e);
            });

        }, e -> {
            // In case of error, still show toast for acceptance
            Toast.makeText(getContext(), "Follow request accepted", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error checking following status", e);
        });
    }

    /**
     * Sends a follow request back to the user whose request was accepted.
     *
     * @param username Username to send the follow request to
     */
    private void sendFollowBackRequest(String username) {
        progressBar.setVisibility(View.VISIBLE);

        // Send follow request
        participantRepository.sendFollowRequest(currentUsername, username, unused -> {
            Toast.makeText(getContext(), "Follow request sent", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }, e -> {
            Log.e(TAG, "Error sending follow back request", e);
            Toast.makeText(getContext(), "Error sending follow request", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }

    /**
     * Handles declining a follow request.
     * Updates the database and refreshes the UI.
     *
     * @param requestorUsername Username of the person who sent the follow request
     * @param position          Position of the request in the list
     */
    @Override
    public void onDecline(String requestorUsername, int position) {
        progressBar.setVisibility(View.VISIBLE);

        participantRepository.declineFollowRequest(currentUsername, requestorUsername, unused -> {
            if (getView() != null) {
                getView().post(() -> {
                    // Simply reload all requests
                    loadFollowRequests();
                    Toast.makeText(getContext(), "Follow request declined", Toast.LENGTH_SHORT).show();
                });
            }
        }, e -> {
            if (getView() != null) {
                getView().post(() -> {
                    Log.e(TAG, "Error declining follow request", e);
                    Toast.makeText(getContext(), "Error declining follow request", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }
}