package com.example.bread;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.example.bread.model.MoodEvent;
import com.example.bread.view.HomePage;
import com.example.bread.view.LoginPage;
import com.example.bread.view.MainActivity;
import com.google.firebase.firestore.DocumentReference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;

public class MoodHistoryFragmentTest {

    private ArrayList<MoodEvent> testArray;

    @Rule
    public ActivityScenarioRule<HomePage> scenario = new ActivityScenarioRule<>(HomePage.class);


    @Before
    public void setUp(){

    }

    //test that mood history properly retrieves user moods from database and shows them on history
    @Test
    public void retrieveMoodFromHistoryTest(){
        onView(withId(R.id.history)).perform(click());
    }


    //test that mood history properly sorts events by date

    //test that mood history properly deletes user moods from database

}
