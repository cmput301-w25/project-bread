package com.example.bread.utils;

import com.example.bread.model.MoodEvent;
import java.util.HashMap;

public class EmotionUtils {
    private static final HashMap<MoodEvent.EmotionalState, String> emotionEmoticonMap = new HashMap<>();
    static {
        emotionEmoticonMap.put(MoodEvent.EmotionalState.HAPPY, "😃");
        emotionEmoticonMap.put(MoodEvent.EmotionalState.SAD, "😢");
        emotionEmoticonMap.put(MoodEvent.EmotionalState.ANGRY, "😡");
        emotionEmoticonMap.put(MoodEvent.EmotionalState.ANXIOUS, "😰");
        emotionEmoticonMap.put(MoodEvent.EmotionalState.NEUTRAL, "😐");
        emotionEmoticonMap.put(MoodEvent.EmotionalState.CONFUSED, "😕");
        emotionEmoticonMap.put(MoodEvent.EmotionalState.FEARFUL, "😨");
        emotionEmoticonMap.put(MoodEvent.EmotionalState.SHAMEFUL, "😞");
        emotionEmoticonMap.put(MoodEvent.EmotionalState.SURPRISED, "😲");
        emotionEmoticonMap.put(MoodEvent.EmotionalState.NONE, "❓");
    }
    public static String getEmoticon(MoodEvent.EmotionalState emotion) {
        return emotionEmoticonMap.getOrDefault(emotion, "❓");
    }
}

