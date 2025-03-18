package com.example.bread.fragment;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bread.R;
import com.example.bread.model.Comment;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class EventDetail extends Fragment {

    private static final String TAG = "EventDetail";
    private static final String MOOD_PARAM = "moodEvent";

    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;

    private MoodEvent moodEvent;

    private TextView usernameText, titleText, timestampText, emotionText, socialSituationText, reasonText;
    private ImageView userImage, moodImage;
    private RecyclerView commentRecyclerView;
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
        commentRecyclerView = view.findViewById(R.id.comments_recycler_view);
        addCommentButton = view.findViewById(R.id.add_comment_fab);

        // Set the text views to the mood event's details
        usernameText.setText(moodEvent.getParticipantRef().getId());
        titleText.setText(moodEvent.getTitle());
        timestampText.setText(moodEvent.getTimestamp().toString());
        emotionText.setText(moodEvent.getEmotionalState().toString());
        socialSituationText.setText(moodEvent.getSocialSituation().toString());
        reasonText.setText(moodEvent.getReason());
        if (moodEvent.getAttachedImage() != null) {
            moodImage.setImageBitmap(ImageHandler.base64ToBitmap(moodEvent.getAttachedImage()));
        }

        fetchEventCreator();
        fetchComments();

        return view;
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
            for (Comment comment : comments) {
                Log.d(TAG, "Comment: " + comment);
            }
        }, e -> Log.e(TAG, "Error fetching comments", e));
    }
}