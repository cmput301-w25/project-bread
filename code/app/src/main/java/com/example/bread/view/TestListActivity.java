package com.example.bread.view;

import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bread.R;
import com.example.bread.controller.MoodEventArrayAdapter;
import com.example.bread.model.MoodEvent;

import java.util.ArrayList;

public class TestListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_list);

        ListView listView = findViewById(R.id.list_view);

        // Create a test MoodEvent
        MoodEvent testEvent = new MoodEvent();
        testEvent.setTitle("Test Mood");
        testEvent.setEmotionalState(MoodEvent.EmotionalState.HAPPY);

        ArrayList<MoodEvent> moodEvents = new ArrayList<>();
        moodEvents.add(testEvent);

        MoodEventArrayAdapter adapter = new MoodEventArrayAdapter(this, moodEvents);
        listView.setAdapter(adapter);
    }
}
