package com.example.bread.view;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bread.R;
import com.example.bread.controller.MoodEventHandler;
import com.example.bread.model.MoodEvent;

import java.util.List;

public class HomePage extends AppCompatActivity {

    private Spinner emotionalStateSpinner;
    private MoodEventHandler moodEventHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        emotionalStateSpinner = findViewById(R.id.emotionalStateSpinner);
        moodEventHandler = new MoodEventHandler();

        // Fetch predefined emotions
        List<MoodEvent.EmotionalState> emotions = moodEventHandler.getPredefinedEmotionalStates();

        // Populate Spinner with emotions
        ArrayAdapter<MoodEvent.EmotionalState> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, emotions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emotionalStateSpinner.setAdapter(adapter);
    }
}

