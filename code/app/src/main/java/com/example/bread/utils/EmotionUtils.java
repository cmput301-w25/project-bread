package com.example.bread.utils;

import com.example.bread.model.MoodEvent;
import java.util.HashMap;
import com.example.bread.R;

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
    public static String getEmoticon(MoodEvent.EmotionalState emotion) {
        return emotionEmoticonMap.getOrDefault(emotion, "❓");
    }

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

