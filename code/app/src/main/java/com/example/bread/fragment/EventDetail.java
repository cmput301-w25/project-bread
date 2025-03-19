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
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.controller.CommentAdapter;
import com.example.bread.model.Comment;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.util.Date;
import java.util.Objects;


public class EventDetail extends Fragment {

    private static final String TAG = "EventDetail";
    private static final String MOOD_PARAM = "moodEvent";

    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;

    private MoodEvent moodEvent;

    private TextView usernameText, titleText, timestampText, emotionText, socialSituationText, reasonText;
    private ImageView userImage, moodImage, closeImage;
    private RecyclerView commentRecyclerView;
    private CommentAdapter commentAdapter;
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
            }
        }
        moodEventRepository = new MoodEventRepository();
        participantRepository = new ParticipantRepository();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_event_detail, container, false);
        usernameText = view.findViewById(R.id.username_text);
        titleText = view.findViewById(R.id.event_title);
        timestampText = view.findViewById(R.id.timestamp_text);
        emotionText = view.findViewById(R.id.emotional_state_text);
        socialSituationText = view.findViewById(R.id.social_situation_text);
        reasonText = view.findViewById(R.id.reason_text);
        userImage = view.findViewById(R.id.profile_image);
        moodImage = view.findViewById(R.id.event_image);
        closeImage = view.findViewById(R.id.close_button);
        commentRecyclerView = view.findViewById(R.id.comments_recycler_view);
        addCommentButton = view.findViewById(R.id.add_comment_fab);

        // Set the text views to the mood event's details
        usernameText.setText(moodEvent.getParticipantRef().getId());
        titleText.setText(moodEvent.getTitle());
        timestampText.setText(transformTimestamp(moodEvent.getTimestamp()));
        emotionText.setText(moodEvent.getEmotionalState().toString());
        socialSituationText.setText(moodEvent.getSocialSituation().toString());
        reasonText.setText(moodEvent.getReason());
        if (moodEvent.getAttachedImage() != null) {
            moodImage.setImageBitmap(ImageHandler.base64ToBitmap(moodEvent.getAttachedImage()));
        }

        closeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().beginTransaction().remove(EventDetail.this).commit();
            }
        });

        addCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAddCommentDialog();
            }
        });

        fetchEventCreator();
        fetchComments();

        return view;
    }

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
            if (!comment.isEmpty()) {
                moodEventRepository.addComment(moodEvent, new Comment(Objects.requireNonNull(currentUserRef()), comment), (x) -> {
                    fetchComments();
                    dialog.dismiss();
                }, e -> Log.e(TAG, "Error adding comment", e));
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });
    }

    private DocumentReference currentUserRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            return participantRepository.getParticipantRef(user.getDisplayName());
        }
        return null;
    }

    private String transformTimestamp(Date timestamp) {
        // Show hours ago if less than 24 hours, otherwise show how many days ago
        long diff = new Date().getTime() - timestamp.getTime();
        long hours = diff / (60 * 60 * 1000);
        if (hours < 24) {
            return hours + " hours ago";
        } else {
            long days = hours / 24;
            return days + " days ago";
        }
    }

    private void fetchEventCreator() {
        participantRepository.fetchParticipantByRef(moodEvent.getParticipantRef(), participant -> {
            // Set the user's image
            if (participant.getProfilePicture() != null) {
                userImage.setImageBitmap(ImageHandler.base64ToBitmap(participant.getProfilePicture()));
            }
        }, e -> Log.e(TAG, "Error fetching participant", e));
    }

    private void fetchComments() {
        moodEventRepository.fetchComments(moodEvent, comments -> {
            // Set the comments to the recycler view
            commentAdapter = new CommentAdapter(comments);
            commentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            commentRecyclerView.setAdapter(commentAdapter);
        }, e -> Log.e(TAG, "Error fetching comments", e));
    }
}