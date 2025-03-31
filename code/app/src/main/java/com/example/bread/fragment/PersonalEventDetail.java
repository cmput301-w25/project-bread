package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresExtension;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bread.model.MoodEvent.EmotionalState;
import com.example.bread.model.MoodEvent.SocialSituation;
import com.example.bread.R;
import com.example.bread.controller.PersonalEventDetailAdapter;
import com.example.bread.firebase.FirebaseService;
import com.example.bread.model.Comment;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Comparator;
import java.util.Objects;


public class PersonalEventDetail extends Fragment implements PersonalEventDetailAdapter.OnEditMoodEventClickListener {

    private static final String TAG = "PersonalEventDetail";
    private static final String MOOD_PARAM = "moodEvent";

    private MoodEventRepository moodEventRepository;
    private ParticipantRepository participantRepository;

    private MoodEvent moodEvent;
    private ImageView closeImage;
    private RecyclerView eventRecyclerView;
    private PersonalEventDetailAdapter eventDetailAdapter;
    private FloatingActionButton addCommentButton;
    private ActivityResultLauncher<Intent> resultLauncher;
    private ImageButton editImage;
    private String imageBase64;

    public PersonalEventDetail() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param event Parameter 1.
     * @return A new instance of fragment PersonalEventDetail.
     */
    public static PersonalEventDetail newInstance(MoodEvent event) {
        PersonalEventDetail fragment = new PersonalEventDetail();
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
        // Image editing, required to ensure it is available throughout whole lifecycle
        registerResult();
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
            ).remove(PersonalEventDetail.this).commit();
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

    private void fetchComments() {
        moodEventRepository.fetchComments(moodEvent, comments -> {
            comments.sort(Comparator.reverseOrder());
            eventDetailAdapter = new PersonalEventDetailAdapter(moodEvent, comments, participantRepository);
            // Set the listener (this fragment implements the interface)
            eventDetailAdapter.setOnEditMoodEventClickListener(this);
            eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            eventRecyclerView.setAdapter(eventDetailAdapter);
        }, e -> Log.e(TAG, "Error fetching comments", e));
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

    private DocumentReference currentUserRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            return participantRepository.getParticipantRef(user.getDisplayName());
        }
        return null;
    }

    /**
     * Allows user to pick an image from the gallery (for Android 13 or with the extension version check).
     */
    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    private void pickImage() {
        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        resultLauncher.launch(intent);
    }

    private void registerResult() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getData() == null) {
                            Log.e(TAG, "No image selected.");
                            return;
                        }
                        try {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                // Changes image on the button if user changes image
                                editImage.setImageURI(imageUri);
                                // Assigns new image to our global variable that is then assigned to moodEvent
                                imageBase64 = ImageHandler.compressImageToBase64(requireContext(), result.getData().getData());
                                Log.d(TAG, "Image selected and converted: " + imageBase64);
                            } else {
                                Log.e(TAG, "No image selected.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "User did not change image.");
                        }
                    }
                }
        );
    }

    @Override
    public void onEditMoodEventClick(MoodEvent moodEvent) {
        showEditMoodDialog(moodEvent);
    }

    /**
     * Shows a dialog to edit the selected mood event.
     *
     * @param moodEvent The mood event to edit
     */
    private void showEditMoodDialog(MoodEvent moodEvent) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        ViewGroup outer = (ViewGroup) LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_edit_mood, null, false);
        MaterialCardView cardView = outer.findViewById(R.id.editMoodCard);
        outer.removeView(cardView);
        builder.setView(cardView);

        EditText titleEditText = cardView.findViewById(R.id.edit_title);
        EditText reasonEditText = cardView.findViewById(R.id.edit_reason);
        Spinner emotionSpinner = cardView.findViewById(R.id.edit_emotion_spinner);
        Spinner socialSituationSpinner = cardView.findViewById(R.id.edit_social_situation_spinner);
        Chip privateChip = cardView.findViewById(R.id.privateChip_editmood);
        editImage = cardView.findViewById(R.id.image_edit_button);
        ImageButton deleteImageButton = cardView.findViewById(R.id.delete_image_button);

        titleEditText.setText(moodEvent.getTitle() != null ? moodEvent.getTitle() : "");
        reasonEditText.setText(moodEvent.getReason() !=
                null ? moodEvent.getReason() : "");
        imageBase64 = moodEvent.getAttachedImage();

        ArrayAdapter<EmotionalState> emotionAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                EmotionalState.values()
        );
        emotionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionSpinner.setAdapter(emotionAdapter);

        if (moodEvent.getAttachedImage() != null && !moodEvent.getAttachedImage().isEmpty()) {
            editImage.setImageBitmap(ImageHandler.base64ToBitmap(moodEvent.getAttachedImage()));
        } else {
            editImage.setImageResource(R.drawable.camera_icon);
        }

        if (moodEvent.getEmotionalState() != null) {
            emotionSpinner.setSelection(emotionAdapter.getPosition(moodEvent.getEmotionalState()));
        }

        ArrayAdapter<SocialSituation> socialAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                SocialSituation.values()
        );
        socialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(socialAdapter);

        if (moodEvent.getSocialSituation() != null) {
            socialSituationSpinner.setSelection(socialAdapter.getPosition(moodEvent.getSocialSituation()));
        }

        privateChip.setChecked(moodEvent.getVisibility() == MoodEvent.Visibility.PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2) {
            editImage.setOnClickListener(v -> pickImage());
        }

        deleteImageButton.setOnClickListener(v -> {
            imageBase64 = null;
            editImage.setImageDrawable(null);
            editImage.setImageResource(R.drawable.camera_icon);
        });

        Button updateButton = cardView.findViewById(R.id.saveButton);
        Button cancelButton = cardView.findViewById(R.id.cancelButton);

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        updateButton.setOnClickListener(v -> {
            boolean isValid = true;
            String newTitle = titleEditText.getText().toString().trim();
            EmotionalState newEmotionalState = (EmotionalState) emotionSpinner.getSelectedItem();
            String newReason = reasonEditText.getText().toString().trim();
            SocialSituation newSocialSituation = (SocialSituation) socialSituationSpinner.getSelectedItem();

            if (newTitle.isEmpty()) {
                titleEditText.setError("Title cannot be empty");
                isValid = false;
            }
            if (!newReason.isEmpty()) {
                int charCount = newReason.length();
                int wordCount = newReason.split("\\s+").length;
                if (charCount > 20 || wordCount > 3) {
                    reasonEditText.setError("Reason must be 20 characters or fewer and 3 words or fewer");
                    isValid = false;
                }
            }
            if (newEmotionalState == EmotionalState.NONE) {
                Toast.makeText(getContext(), "Emotional state cannot be None", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
            if (!isValid) {
                return;
            }

            moodEvent.setTitle(newTitle);
            moodEvent.setEmotionalState(newEmotionalState);
            moodEvent.setReason(newReason);
            moodEvent.setSocialSituation(newSocialSituation);
            moodEvent.setAttachedImage(imageBase64);
            moodEvent.setVisibility(privateChip.isChecked() ? MoodEvent.Visibility.PRIVATE : MoodEvent.Visibility.PUBLIC);

            // Update your local lists and notify changes as needed
            // (Assuming moodEventArrayList, allMoodEvents, and moodArrayAdapter are managed in HistoryFragment)
            // Here you might need to update these lists via a callback or shared ViewModel

            moodEventRepository.updateMoodEvent(moodEvent,
                    aVoid -> {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Mood updated successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        dialog.dismiss();
                    },
                    e -> {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Failed to update mood", Toast.LENGTH_SHORT).show();
                                    Log.e("HistoryFragment", "Error updating mood", e);
                                }
                            });
                        }
                    }
            );
        });
    }
}