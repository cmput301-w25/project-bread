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
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;

import java.util.ArrayList;
import java.util.List;

public class FilterMoodEventFragment extends DialogFragment {
    String reasonKeywordInput;
    MoodEvent.EmotionalState selectedMood;
    boolean mostRecentButtonInput;

    interface FilterMoodDialogListener {
        //----------------------------------------------------------------------------------------------------
        void applyingFilters(boolean isChecked, MoodEvent.EmotionalState moodState, String reason);
        void saveFilterState(boolean mostRecent, String reason, MoodEvent.EmotionalState moodState);
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
        ToggleButton mostRecentButton = view.findViewById(R.id.mostRecentWeekButton);

        //adding mood states to the spinner
        Spinner moodDropdown = view.findViewById(R.id.moodDropdown);
        List<String> moodOptions = new ArrayList<>(); //array of mood options
        moodOptions.add("");  // adding blank first option incase no mood wanted
        for (MoodEvent.EmotionalState state : MoodEvent.EmotionalState.values()) {
            moodOptions.add(state.name());  //looping through emotional states and adding to array
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, moodOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodDropdown.setAdapter(adapter);

        EditText reasonKeyword = view.findViewById(R.id.reasonKeywordEdit);

        Bundle args = getArguments();
        if (args != null) {
            // Set toggle state
            mostRecentButton.setChecked(args.getBoolean("mostRecent", false));

            // Set reason keyword (if present)
            if (args.containsKey("reasonKeyword")) {
                reasonKeyword.setText(args.getString("reasonKeyword"));
            }

            // Set mood spinner (if present)
            if (args.containsKey("moodState")) {
                String moodStateName = args.getString("moodState");
                if (moodStateName != null) {
                    int spinnerPosition = adapter.getPosition(moodStateName.toUpperCase());
                    moodDropdown.setSelection(spinnerPosition);
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("Filter Mood Events")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Filter", (dialog, which) -> {
                    //boolean testing whether user selected yes or no to most recent week !!!!!FIX SWITCH
                    mostRecentButtonInput = mostRecentButton.isChecked();
                    if (mostRecentButtonInput){
                    }

                    //retrieving reason keywords and calling function if filtered
                    if (reasonKeyword.getText().toString().isBlank()){
                        reasonKeywordInput = null; //set keyword to null if user does not want to filter by reason
                    }
                    else{
                        reasonKeywordInput = reasonKeyword.getText().toString(); //set keyword to string if user filters by reason
                    }

                    // get selected mood from spinner and convert to string to pass to function
                    String selectedMoodString = (String) moodDropdown.getSelectedItem();
                    if (!selectedMoodString.isEmpty()) {
                        selectedMood = MoodEvent.EmotionalState.valueOf(selectedMoodString);
                    }
                    else{
                        selectedMood = null; //if user does not want to filter by mood state
                    }
                    listener.applyingFilters(mostRecentButtonInput, selectedMood, reasonKeywordInput);
                    listener.saveFilterState(mostRecentButtonInput, reasonKeywordInput, selectedMood);
                })
                .create();
    }
}
