package com.example.bread;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.example.bread.utils.EmotionUtils;

import org.junit.Test;

import static org.junit.Assert.*;

public class EmotionUtilsTest {

    @Test
    public void testGetEmoticon_ReturnsCorrectEmoji() {
        assertEquals("😃", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.HAPPY));
        assertEquals("😢", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.SAD));
        assertEquals("😡", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.ANGRY));
        assertEquals("😰", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.ANXIOUS));
        assertEquals("😐", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.NEUTRAL));
        assertEquals("😕", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.CONFUSED));
        assertEquals("😨", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.FEARFUL));
        assertEquals("😞", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.SHAMEFUL));
        assertEquals("😲", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.SURPRISED));
    }

    @Test
    public void testGetEmoticon_UnknownEmotion_ReturnsQuestionMark() {
        assertEquals("❓", EmotionUtils.getEmoticon(MoodEvent.EmotionalState.NONE));
    }

    @Test
    public void testGetColorResource_ReturnsCorrectColorResource() {
        assertEquals(R.color.happyColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.HAPPY));
        assertEquals(R.color.sadColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.SAD));
        assertEquals(R.color.angryColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.ANGRY));
        assertEquals(R.color.anxiousColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.ANXIOUS));
        assertEquals(R.color.neutralColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.NEUTRAL));
        assertEquals(R.color.confusedColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.CONFUSED));
        assertEquals(R.color.fearfulColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.FEARFUL));
        assertEquals(R.color.shamefulColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.SHAMEFUL));
        assertEquals(R.color.surprisedColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.SURPRISED));
    }

    @Test
    public void testGetColorResource_UnknownEmotion_ReturnsDefaultColor() {
        assertEquals(R.color.noneColor, EmotionUtils.getColorResource(MoodEvent.EmotionalState.NONE));
    }
}
