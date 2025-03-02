package com.example.bread;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

import com.example.bread.view.TestListActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MoodEventArrayAdapterInstrumentedTest {

    @Rule
    public ActivityScenarioRule<TestListActivity> activityRule =
            new ActivityScenarioRule<>(TestListActivity.class);

    @Test
    public void testListItemDisplaysCorrectContent() {

        onData(anything())
                .inAdapterView(withId(R.id.list_view))
                .atPosition(0)
                .onChildView(withId(R.id.title_text_view))
                .check(matches(withText("Test Mood")));

        onData(anything())
                .inAdapterView(withId(R.id.list_view))
                .atPosition(0)
                .onChildView(withId(R.id.emoticon_text_view))
                .check(matches(withText("😃")));
    }
}
