package com.example.bread.view;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.repository.MoodEventRepository;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddMoodEventActivity extends AppCompatActivity {
    private Spinner emotionalStateSpinner, socialSituationSpinner;
    private EditText reasonEditText;
    private Button saveButton;
    private MoodEventRepository moodEventRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood_event);

        // Tie UI elements to variables
        emotionalStateSpinner = findViewById(R.id.emotionalStateSpinner);
        reasonEditText = findViewById(R.id.reasonEditText);
        socialSituationSpinner = findViewById(R.id.socialSituationSpinner);
        saveButton = findViewById(R.id.saveButton);

        // Set up repository
        moodEventRepository = new MoodEventRepository();

        // Populate emotional state spinner
        ArrayAdapter<MoodEvent.EmotionalState> moodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MoodEvent.EmotionalState.values());
        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionalStateSpinner.setAdapter(moodAdapter);

        // Populate social situation spinner (add a "None" option for optional)
        ArrayAdapter<MoodEvent.SocialSituation> socialAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MoodEvent.SocialSituation.values());
        socialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(socialAdapter);

        // Save button logic
        saveButton.setOnClickListener(v -> saveMoodEvent());
    }

    private void saveMoodEvent() {
        // Get selected emotional state (required)
        MoodEvent.EmotionalState emotionalState = (MoodEvent.EmotionalState) emotionalStateSpinner.getSelectedItem();
        if (emotionalState == null) {
            Toast.makeText(this, "Please select a mood!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get reason (optional)
        String reason = reasonEditText.getText().toString().trim();
        if (reason.isEmpty()) reason = null;

        // Get social situation (optional)
        MoodEvent.SocialSituation socialSituation = (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem();

        // Get current user (based on assumptions about sharedprefs)
        String username = getSharedPreferences("sharedPrefs", MODE_PRIVATE).getString("username", "");
        DocumentReference participantRef = FirebaseFirestore.getInstance().collection("participants").document(username);

        // Create the mood event
        MoodEvent moodEvent = new MoodEvent(reason, emotionalState, participantRef, socialSituation, null);

        // Save to Firebase
        //TODO: yet to be tested (firebase configuration remains)
//        moodEventRepository.addMoodEvent(
//                moodEvent,
//                aVoid -> {
//                    Toast.makeText(this, "Mood saved!", Toast.LENGTH_SHORT).show();
//                    finish(); // Close the screen
//                },
//                e -> Toast.makeText(this, "Failed to save mood: " + e.getMessage(), Toast.LENGTH_SHORT).show()
//        );

        // temporary test block for issue #41/42

    }
}