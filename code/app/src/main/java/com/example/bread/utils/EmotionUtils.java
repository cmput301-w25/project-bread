package com.example.bread.utils; // Ensures proper package placement

import java.util.HashMap;

public class EmotionUtils {
    // used a hashmap here to basically store the emotion-to-emoticon mappings
    private static final HashMap<String, String> emotionEmoticonMap = new HashMap<>();

    // Static block for the mappings when the class is first accessed
    static {
        emotionEmoticonMap.put("HAPPY", "😃");      // Happy
        emotionEmoticonMap.put("SAD", "😢");        // Sad
        emotionEmoticonMap.put("ANGRY", "😡");      // Angry
        emotionEmoticonMap.put("ANXIOUS", "😰");    // Anxious
        emotionEmoticonMap.put("NEUTRAL", "😐");    // Neutral
        emotionEmoticonMap.put("CONFUSED", "😕");   // Confused
        emotionEmoticonMap.put("FEARFUL", "😨");    // Fearful
        emotionEmoticonMap.put("SHAMEFUL", "😞");   // Shameful
        emotionEmoticonMap.put("SURPRISED", "😲");  // Surprised
    }


    public static String getEmoticon(String emotion) {
        return emotionEmoticonMap.getOrDefault(emotion, "❓"); // Returns a question mark if the emotion is not mapped
    }
}
