package com.example.bread.utils;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;

import java.util.HashMap;

/**
 * EmotionUtils - Utils
 * <p>
 * Role / Purpose
 * Provides mapping between `MoodEvent.EmotionalState` enums and their corresponding visual representations, including emoticons and color resources.
 * Used throughout the application to standardize emotional visuals in the UI.
 * <p>
 * Design Patterns
 * Utility Pattern: Offers static methods and mappings without requiring instantiation.
 * Enum Mapping: Uses a HashMap and switch-case structure to associate enums with resources.
 * <p>
 * Outstanding Issues
 * - Emotion-to-color mapping is tightly coupled to specific resource IDs, reducing flexibility across themes.
 * - Emoticons may not render uniformly across devices due to font differences.
 */

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

    }

    /**
     * Returns the emoticon corresponding to the given emotional state.
     *
     * @param emotion the emotional state
     * @return the emoticon corresponding to the emotional state
     */
    public static String getEmoticon(MoodEvent.EmotionalState emotion) {
        return emotionEmoticonMap.getOrDefault(emotion, "❓");
    }

    /**
     * Returns the color resource corresponding to the given emotional state.
     *
     * @param emotion the emotional state
     * @return the color resource corresponding to the emotional state
     */
    public static int getColorResource(MoodEvent.EmotionalState emotion) {
        switch (emotion) {
            case HAPPY:
                return R.color.happyColor;
            case SAD:
                return R.color.sadColor;
            case ANGRY:
                return R.color.angryColor;
            case ANXIOUS:
                return R.color.anxiousColor;
            case NEUTRAL:
                return R.color.neutralColor;
            case CONFUSED:
                return R.color.confusedColor;
            case FEARFUL:
                return R.color.fearfulColor;
            case SHAMEFUL:
                return R.color.shamefulColor;
            case SURPRISED:
                return R.color.surprisedColor;
            default:
                return R.color.noneColor;
        }
    }
}

