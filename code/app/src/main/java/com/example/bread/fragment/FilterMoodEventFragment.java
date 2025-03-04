package com.example.bread.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.bread.R;

public class FilterMoodEventFragment extends DialogFragment {
    String reasonKeywordInput;

    interface FilterMoodDialogListener {
        void mostRecentWeek();
        void filterByMood();
        void filterByReason();
    }

    private FilterMoodDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FilterMoodDialogListener) {
            listener = (FilterMoodDialogListener) context;
        } else {
            throw new RuntimeException(context + " must implement FilterMoodDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_filter_moods, null);
        Switch mostRecentSwitch = view.findViewById(R.id.mostRecentWeekSwitch);
        Spinner moodDropdown = view.findViewById(R.id.moodDropdown);
        EditText reasonKeyword = view.findViewById(R.id.reasonKeywordEdit);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        return builder
                .setView(view)
                .setTitle("Filter Mood Events")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Filter", (dialog, which) -> {
                    
                    String reasonKeywordInput = reasonKeyword.getText().toString();
//                    listener.addCity(new City(cityName, provinceName)); //create new city using our strings
                })
                .create();
    }
}
