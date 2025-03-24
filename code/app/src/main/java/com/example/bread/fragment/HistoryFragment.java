package com.example.bread.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresExtension;
import androidx.fragment.app.Fragment;
import com.example.bread.R;
import com.example.bread.controller.HistoryMoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;
import com.example.bread.model.MoodEvent.EmotionalState;
import com.example.bread.model.MoodEvent.SocialSituation;
import com.example.bread.repository.MoodEventRepository;
import com.example.bread.repository.ParticipantRepository;
import com.example.bread.utils.ImageHandler;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Represents the history page of the app, where users can view their mood events and apply filters.
 */
public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private ListView moodEventListView;
    private ArrayList<MoodEvent> moodEventArrayList;
    private HistoryMoodEventArrayAdapter moodArrayAdapter;

    private MoodEventRepository moodsRepo;
    private ParticipantRepository userRepo;
    private Set<MoodEvent> selectedEvents = new HashSet<>();

    private String username;
    private DocumentReference participantRef;

    // Image related variables
    private ImageButton editImage;
    private ActivityResultLauncher<Intent> resultLauncher;
    private String imageBase64;

    // Filter-related variables
    private FloatingActionButton filterButton;
    private ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    private boolean isFilteringByWeek = false;
    private MoodEvent.EmotionalState selectedEmotionalState = null;
    private String searchKeyword = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        moodEventListView = view.findViewById(R.id.historyListView);
        moodEventArrayList = new ArrayList<>();
        moodArrayAdapter = new HistoryMoodEventArrayAdapter(getContext(), moodEventArrayList);
        moodEventListView.setAdapter(moodArrayAdapter);

        // Set click listener for mood events
        moodArrayAdapter.setOnMoodEventClickListener(this::showMoodDetailsDialog);

        moodsRepo = new MoodEventRepository();
        userRepo = new ParticipantRepository();

        fetchParticipantAndLoadEvents();

        Button deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        // Add filter button click listener
        filterButton = view.findViewById(R.id.filter_button);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }

        // Image editing, required to ensure it is available throughout whole lifecycle
        registerResult();

        return view;
    }

    /**
     * Retrieves current user using FirebaseUser and uses to find participant ref.
     * Logs appropriate error messages if username null or user is not found.
     * Uses loadMoodEvents() to find mood events corresponding to user
     */
    private void fetchParticipantAndLoadEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            username = currentUser.getDisplayName();
            if (username == null) {
                Log.e(TAG, "Username is null. Cannot load mood events.");
                return;
            }
            participantRef = userRepo.getParticipantRef(username);
            loadMoodEvents();
        } else {
            Log.e(TAG, "No authenticated user found.");
        }
    }

    /**
     * Uses listenForEventsWithParticipantRef() from MoodEventRepository class
     * to actively retrieve mood events corresponding to user whenever added.
     * Adds/alters user mood events to moodEventArrayList whenever there are changes.
     * Sorts mood events by date and time added
     */
    private void loadMoodEvents() {
        moodsRepo.fetchEventsWithParticipantRef(participantRef, moodEvents -> {
                    if (moodEvents != null) {
                        moodEventArrayList.clear();
                        moodEventArrayList.addAll(moodEvents);
                        moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));

                        // Save all mood events for filtering
                        allMoodEvents.clear();
                        allMoodEvents.addAll(moodEventArrayList);

                        // Reapply any existing filters
                        if (isFilteringByWeek || selectedEmotionalState != null || !searchKeyword.isEmpty()) {
                            applyFilters();
                        }
                    }
                    moodArrayAdapter.notifyDataSetChanged();
                },
                error -> {
                    Log.e("History Fragment", "Failed to listen for mood events", error);
                });
    }

    /**
     * Displays a confirmation dialog asking the user if they want to delete the selected mood events.
     * If the user confirms, deletion is triggered.
     */
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete the selected mood events?");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteSelectedMoodEvents());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Deletes all currently selected mood events from Firestore.
     * Removes the events from the list and updates the adapter.
     * Displays a toast if any errors occur during deletion.
     */
    private void deleteSelectedMoodEvents() {
        MoodEventRepository repository = new MoodEventRepository();
        selectedEvents = ((HistoryMoodEventArrayAdapter) moodEventListView.getAdapter()).getSelectedEvents();
        for (MoodEvent event : selectedEvents) {
            repository.deleteMoodEvent(event, new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    getActivity().runOnUiThread(() -> {
                        moodArrayAdapter.remove(event);
                        moodArrayAdapter.notifyDataSetChanged();
                    });
                }
            }, e -> Toast.makeText(getContext(), "Error deleting event", Toast.LENGTH_SHORT).show());
        }
        selectedEvents.clear();  // Clear the selection after deletion
    }

    /**
     * Shows a dialog with the details of the selected mood event.
     *
     * @param moodEvent The mood event to show details for
     */
    private void showMoodDetailsDialog(MoodEvent moodEvent) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("View Mood");

        // Inflate a custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_mood_details, null);

        // Set up the views
        TextView emotionTextView = dialogView.findViewById(R.id.detail_emotion);
        TextView dateTextView = dialogView.findViewById(R.id.detail_date);
        TextView reasonTextView = dialogView.findViewById(R.id.detail_reason);
        TextView socialSituationTextView = dialogView.findViewById(R.id.detail_social_situation);
        ImageView moodImageView = dialogView.findViewById(R.id.mood_event_image);
        TextView imageSelection = dialogView.findViewById(R.id.image_selection);

        // Set the data
        emotionTextView.setText(moodEvent.getEmotionalState().toString());

        // Format date
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy hh:mm a");
        String dateString = formatter.format(moodEvent.getTimestamp());
        dateTextView.setText(dateString);

        // Set reason
        reasonTextView.setText(moodEvent.getReason() != null ? moodEvent.getReason() : "No reason provided");

        // Set social situation
        SocialSituation situation = moodEvent.getSocialSituation();
        socialSituationTextView.setText(situation != null ? situation.toString() : "Not specified");

        // Set image
        String imageBase64 = moodEvent.getAttachedImage();
        if (imageBase64 == null || imageBase64.isEmpty()) {
            moodImageView.setVisibility(View.GONE);  // Hide if image not uploaded
            imageSelection.setText("No image uploaded.");
        } else {
            Bitmap bitmap = ImageHandler.base64ToBitmap(imageBase64);
            moodImageView.setImageBitmap(bitmap);
            moodImageView.setVisibility(View.VISIBLE);  // Show when image is uploaded
            imageSelection.setText("");
        }

        builder.setView(dialogView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        // Add an Edit button
        builder.setNeutralButton("Edit", (dialog, which) -> {
            showEditMoodDialog(moodEvent);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Shows a dialog to edit the selected mood event.
     *
     * @param moodEvent The mood event to edit
     */
    private void showEditMoodDialog(MoodEvent moodEvent) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Mood");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_mood, null);
        builder.setView(dialogView);

        EditText titleEditText = dialogView.findViewById(R.id.edit_title);
        EditText reasonEditText = dialogView.findViewById(R.id.edit_reason);
        Spinner emotionSpinner = dialogView.findViewById(R.id.edit_emotion_spinner);
        Spinner socialSituationSpinner = dialogView.findViewById(R.id.edit_social_situation_spinner);
        editImage = dialogView.findViewById(R.id.image_edit_button);
        Button deleteImageButton = dialogView.findViewById(R.id.delete_image_button);

        titleEditText.setText(moodEvent.getTitle() != null ? moodEvent.getTitle() : "");
        reasonEditText.setText(moodEvent.getReason() != null ? moodEvent.getReason() : "");
        imageBase64 = moodEvent.getAttachedImage();

        ArrayAdapter<EmotionalState> emotionAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                EmotionalState.values()
        );
        emotionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionSpinner.setAdapter(emotionAdapter);

        if (moodEvent.getAttachedImage() != null){
            // If image already assigned it is displayed on image button, and blank if not
            editImage.setImageBitmap(ImageHandler.base64ToBitmap(moodEvent.getAttachedImage()));
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

        // Calls pickImage() when user chooses to change their image
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2) {
            editImage.setOnClickListener(v -> pickImage());
        }

        deleteImageButton.setOnClickListener(v -> {
            imageBase64 = null;
            editImage.setImageDrawable(null);
            editImage.setImageResource(R.drawable.camera_icon);
        });

        // **Validation Listeners**
        titleEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String titleText = titleEditText.getText().toString().trim();
                if (titleText.isEmpty()) {
                    titleEditText.setError("Title cannot be empty");
                } else {
                    titleEditText.setError(null);
                }
            }
        });

        reasonEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String reasonText = reasonEditText.getText().toString().trim();
                if (!reasonText.isEmpty()) { // Only validate if reason is provided
                    int charCount = reasonText.length();
                    int wordCount = reasonText.split("\\s+").length;

                    if (charCount > 20 || wordCount > 3) {
                        reasonEditText.setError("Reason must be 20 characters or fewer and 3 words or fewer");
                    } else {
                        reasonEditText.setError(null);
                    }
                } else {
                    reasonEditText.setError(null);
                }
            }
        });

        // **Override Save Button to Enforce Validation**
        builder.setPositiveButton("Save", null); // We override it later to prevent closing
        builder.setNegativeButton("Cancel", (d, which) -> d.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // **Override the Save Button Behavior**
        Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        saveButton.setOnClickListener(v -> {
            boolean isValid = true;

            // Get values from inputs
            String newTitle = titleEditText.getText().toString().trim();
            EmotionalState newEmotionalState = (EmotionalState) emotionSpinner.getSelectedItem();
            String newReason = reasonEditText.getText().toString().trim();
            SocialSituation newSocialSituation = (SocialSituation) socialSituationSpinner.getSelectedItem();

            // **Validation Checks**
            if (newTitle.isEmpty()) {
                titleEditText.setError("Title cannot be empty");
                isValid = false;
            }

            if (!newReason.isEmpty()) { // Validate reason only if provided
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
                // **Do not close the dialog if validation fails**
                return;
            }

            // **Only save if all validations passed**
            moodEvent.setTitle(newTitle);
            moodEvent.setEmotionalState(newEmotionalState);
            moodEvent.setReason(newReason);
            moodEvent.setSocialSituation(newSocialSituation);
            moodEvent.setAttachedImage(imageBase64);

            // Save to Firebase
            moodsRepo.updateMoodEvent(moodEvent,
                    aVoid -> {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Mood updated successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        dialog.dismiss(); // Close the dialog only after a successful save
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
    // Edit / add image related functions
    /**
     *
     */
    private void registerResult(){
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
                            if (imageUri != null){
                                // Changes image on the button if user changes image
                                editImage.setImageURI(imageUri);
                                // Assigns new image to our global variable that is then assigned to moodEvent
                                imageBase64 = ImageHandler.compressImageToBase64(requireContext(), result.getData().getData());
                                Log.d(TAG, "Image selected and converted: " + imageBase64);
                            }
                            else{
                                Log.e(TAG, "No image selected.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "User did not change image.");
                        }
                    }
                }
        );
    }

    /**
     * Allows user to pick an image from camera roll
     * Uses resultLauncher to launch image picking activity
     */
    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    private void pickImage(){
        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        resultLauncher.launch(intent);
    }

    // Filter-related methods
    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_history_filter_moods, null);
        builder.setView(dialogView);

        SwitchMaterial recentWeekSwitch = dialogView.findViewById(R.id.recent_week_switch);
        EditText keywordEditText = dialogView.findViewById(R.id.keyword_edit_text);
        Button applyButton = dialogView.findViewById(R.id.apply_button);
        Button resetButton = dialogView.findViewById(R.id.reset_button);
        Spinner emotionalStateText = dialogView.findViewById(R.id.emotional_state_text);

        recentWeekSwitch.setChecked(isFilteringByWeek);
        keywordEditText.setText(searchKeyword);

        Spinner emotionalStateSpinner = dialogView.findViewById(R.id.emotional_state_text);
        ArrayAdapter<MoodEvent.EmotionalState> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                MoodEvent.EmotionalState.values());
        emotionalStateSpinner.setAdapter(adapter);

        if (selectedEmotionalState != null) {
            emotionalStateSpinner.setSelection(adapter.getPosition(selectedEmotionalState));
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        applyButton.setOnClickListener(v -> {
            isFilteringByWeek = recentWeekSwitch.isChecked();
            selectedEmotionalState = (MoodEvent.EmotionalState) emotionalStateSpinner.getSelectedItem();
            searchKeyword = keywordEditText.getText().toString().trim().toLowerCase();
            applyFilters();
            dialog.dismiss();
        });

        resetButton.setOnClickListener(v -> {
            recentWeekSwitch.setChecked(false);
            emotionalStateSpinner.setSelection(0);
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
        moodArrayAdapter.notifyDataSetChanged();

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
        if (!allMoodEvents.isEmpty()) {
            moodEventArrayList.clear();
            moodEventArrayList.addAll(allMoodEvents);
            moodEventArrayList.sort((e1, e2) -> e2.compareTo(e1));
            moodArrayAdapter.notifyDataSetChanged();
        }
    }
}