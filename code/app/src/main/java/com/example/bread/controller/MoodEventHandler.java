package com.example.bread.controller;

import com.example.bread.model.MoodEvent;
import java.util.Arrays;
import java.util.List;

public class MoodEventHandler {

    /**
     * Returns a predefined list of emotional states for UI selection.
     */
    public List<MoodEvent.EmotionalState> getPredefinedEmotionalStates() {
        return Arrays.asList(MoodEvent.EmotionalState.values());
    }
}
