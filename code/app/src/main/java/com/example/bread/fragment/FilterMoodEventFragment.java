package com.example.bread.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;

public class FilterMoodEventFragment extends DialogFragment {
    String reasonKeywordInput;
    boolean mostRecentSwitchInput;

    interface FilterMoodDialogListener {
        void mostRecentWeek(boolean isChecked);
        void filterByMood();
        void filterByReason();
    }

    private FilterMoodDialogListener listener;

    /**
     * Sets the listener that will receive callbacks from this dialog fragment.
     * @param listener The listener, History Fragment, that implements FilterMoodDialogListener.
     */
    public void setListener(FilterMoodDialogListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_filter_moods, null);
        Switch mostRecentSwitch = view.findViewById(R.id.mostRecentWeekSwitch);

        Spinner moodDropdown = view.findViewById(R.id.moodDropdown);
        MoodEvent.EmotionalState[] states = MoodEvent.EmotionalState.values(); //getting mood states from MoodEvent class
        String[] moodStates = new String[states.length + 1];
        moodStates[0] = "Select";


        EditText reasonKeyword = view.findViewById(R.id.reasonKeywordEdit);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("Filter Mood Events")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Filter", (dialog, which) -> {
                    
                    reasonKeywordInput = reasonKeyword.getText().toString();
                    mostRecentSwitchInput = mostRecentSwitch.isChecked();

                    listener.mostRecentWeek(mostRecentSwitchInput);
                })
                .create();
    }
}
