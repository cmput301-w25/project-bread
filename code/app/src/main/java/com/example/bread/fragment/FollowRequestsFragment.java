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
import com.example.bread.controller.FollowRequestAdapter;
import com.example.bread.model.FollowRequest;
import com.example.bread.repository.ParticipantRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class FollowRequestsFragment extends Fragment implements FollowRequestAdapter.RequestActionListener {

    private static final String TAG = "FollowRequestsFragment";

    private RecyclerView requestsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;

    private FollowRequestAdapter requestAdapter;
    private ParticipantRepository participantRepository;
    private String currentUsername;
    private List<FollowRequest> requestsList = new ArrayList<>();

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

    private void updateEmptyView() {
        if (requestsList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            requestsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            requestsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAccept(String requestorUsername, int position) {
        progressBar.setVisibility(View.VISIBLE);

        participantRepository.acceptFollowRequest(currentUsername, requestorUsername, unused -> {
            // Remove from list and update UI
            requestsList.remove(position);
            requestAdapter.notifyItemRemoved(position);
            updateEmptyView();
            progressBar.setVisibility(View.GONE);

            // Check if already following this user before showing follow back dialog
            participantRepository.isFollowing(currentUsername, requestorUsername, isAlreadyFollowing -> {
                if (isAlreadyFollowing) {
                    // Already following this user, no need for follow back dialog
                    Toast.makeText(getContext(), "Follow request accepted", Toast.LENGTH_SHORT).show();
                } else {
                    // Show follow back dialog only if not already following
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
                Log.e(TAG, "Error checking following status", e);
            });
        }, e -> {
            Log.e(TAG, "Error accepting follow request", e);
            Toast.makeText(getContext(), "Error accepting follow request", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }

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

    @Override
    public void onDecline(String requestorUsername, int position) {
        progressBar.setVisibility(View.VISIBLE);

        participantRepository.declineFollowRequest(currentUsername, requestorUsername, unused -> {
            Toast.makeText(getContext(), "Follow request declined", Toast.LENGTH_SHORT).show();
            requestsList.remove(position);
            requestAdapter.notifyItemRemoved(position);
            updateEmptyView();
            progressBar.setVisibility(View.GONE);
        }, e -> {
            Log.e(TAG, "Error declining follow request", e);
            Toast.makeText(getContext(), "Error declining follow request", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }
}
