package com.example.bread.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.controller.EventDetailAdapter;
import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.Comment;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Comparator;
import java.util.Objects;

/**
 * EventDetail - Fragment
 * <p>
 * Role / Purpose
 * Displays a detailed view of a specific MoodEvent and its comments.
 * Allows users to view, add, and interact with comments related to the event.
 * <p>
 * Design Pattern
 * Fragment Pattern: Encapsulates UI and behavior for a modular part of the screen.
 * MVC Pattern: Acts as the View, working with MoodEventRepository (Model) and Adapter (Controller).
 * <p>
 * Outstanding Issues / Comments
 * Comments are re-fetched on each new addition, which could be optimized with live updates or incremental loading.
 */

public class EventDetail extends Fragment {

    private static final String TAG = "EventDetail";
    private static final String MOOD_PARAM = "moodEvent";

    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;

    private MoodEvent moodEvent;

    private ImageView closeImage;
    private RecyclerView eventRecyclerView;
    private EventDetailAdapter eventDetailAdapter;
    private FloatingActionButton addCommentButton;

    public EventDetail() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param event Parameter 1.
     * @return A new instance of fragment EventDetail.
     */
    public static EventDetail newInstance(MoodEvent event) {
        EventDetail fragment = new EventDetail();
        Bundle args = new Bundle();
        args.putSerializable(MOOD_PARAM, event);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                moodEvent = getArguments().getSerializable(MOOD_PARAM, MoodEvent.class);
            } else {
                moodEvent = (MoodEvent) getArguments().getSerializable(MOOD_PARAM);
            }
        }
        moodEventRepository = new MoodEventRepository();
        participantRepository = new ParticipantRepository();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_detail, container, false);
        closeImage = view.findViewById(R.id.close_button);
        eventRecyclerView = view.findViewById(R.id.main_recycler_view);
        addCommentButton = view.findViewById(R.id.add_comment_fab);

        closeImage.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().setCustomAnimations(
                    R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
            ).remove(EventDetail.this).commit();
        });

        addCommentButton.setOnClickListener(v -> {
            launchAddCommentDialog();
        });

        // Check if device is offline
        boolean offline = !FirebaseService.isNetworkConnected();
        if (offline) {
            Log.d(TAG, "Device is offline - disabling Firestore network");
            // Disable network for faster cache access
            FirebaseFirestore.getInstance().disableNetwork()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Firestore network disabled for offline mode");
                        fetchComments();
                    });

        } else {
            // JUST FETCH FROM THE FIREBASE IF ONLINE
            fetchComments();
        }

        return view;
    }

    /**
     * Launches a dialog for the user to add a comment to the current mood event.
     * Validates the input, ensures comment is under character limit, and syncs it with Firestore.
     * Provides immediate UI feedback by updating the comment list.
     */
    private void launchAddCommentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_comment, null);
        builder.setView(dialogView);

        EditText commentText = dialogView.findViewById(R.id.comment_edit_text);
        Button addButton = dialogView.findViewById(R.id.add_comment_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_comment_button);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        addButton.setOnClickListener(v -> {
            String comment = commentText.getText().toString();
            comment = comment.trim();
            if (!comment.isEmpty()) {
                if (comment.length() > 300) {
                    commentText.setError("Comment must be less than 400 characters");
                    return;
                }
                addButton.setEnabled(false);  // Prevent multiple submissions

                Comment newComment = new Comment(Objects.requireNonNull(currentUserRef()), comment);

                //  For both online and offline, show immediate UI feedback
                dialog.dismiss();

                // Add the comment immediately in the UI first, then sync with Firebase
                moodEventRepository.addComment(moodEvent, newComment, (x) -> {
                    Log.d(TAG, "Comment synced with Firebase: " + newComment.getId());
                }, e -> {
                    Log.e(TAG, "Error adding comment", e);
                    addButton.setEnabled(true);
                });

                // fetch comments to update the UI
                fetchComments();
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });
    }

    /**
     * Retrieves a DocumentReference to the currently authenticated Firebase user’s participant document.
     *
     * @return DocumentReference pointing to the user's participant document or null if the user is not authenticated or the display name is unavailable.
     */
    private DocumentReference currentUserRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            return participantRepository.getParticipantRef(user.getDisplayName());
        }
        return null;
    }

    /**
     * Fetches all comments associated with the current mood event from Firestore.
     * Updates the RecyclerView UI with a sorted list of comments using EventDetailAdapter.
     */
    private void fetchComments() {
        moodEventRepository.fetchComments(moodEvent, comments -> {
            comments.sort(Comparator.reverseOrder());
            eventDetailAdapter = new EventDetailAdapter(moodEvent, comments, participantRepository);
            eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            eventRecyclerView.setAdapter(eventDetailAdapter);
        }, e -> Log.e(TAG, "Error fetching comments", e));
    }



}