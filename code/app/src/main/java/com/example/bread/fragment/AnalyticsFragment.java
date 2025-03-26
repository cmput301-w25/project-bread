package com.example.bread.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsFragment extends Fragment {

    private static final String ARG_PARAM1 = "moodEvents";

    private List<MoodEvent> moodEvents;
    private TextView streakTextView, longestStreakTextView;
    PieChart pieChart;
    BarChart barChart;
    private final Map<String, Map<String, Integer>> monthMoodMap = new HashMap<>();
    private final List<String> sortedMonthKeys = new ArrayList<>();

    public AnalyticsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param events List of MoodEvent.
     * @return A new instance of fragment AnalyticsFragment.
     */
    public static AnalyticsFragment newInstance(List<MoodEvent> events) {
        AnalyticsFragment fragment = new AnalyticsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, (Serializable) events);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            moodEvents = (List<MoodEvent>) getArguments().getSerializable(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        streakTextView = view.findViewById(R.id.streak_text);
        longestStreakTextView = view.findViewById(R.id.longest_streak_text);
        pieChart = view.findViewById(R.id.pie_chart);
        barChart = view.findViewById(R.id.bar_monthly_chart);

        calculateStreak();
        calculateLongestStreak();

        drawPieChart();
        drawBarChart();

        return view;
    }

    private void calculateStreak() {
        if (moodEvents == null || moodEvents.isEmpty()) {
            streakTextView.setText("0");
            return;
        }

        moodEvents.sort(Comparator.naturalOrder());

        MoodEvent lastEvent = moodEvents.get(moodEvents.size() - 1);
        long diffToNow = System.currentTimeMillis() - lastEvent.getTimestamp().getTime();
        if (diffToNow > 86400000) {
            streakTextView.setText("0");
            return;
        }

        int streakCount = 1;
        for (int i = moodEvents.size() - 1; i > 0; i--) {
            MoodEvent currentEvent = moodEvents.get(i);
            MoodEvent previousEvent = moodEvents.get(i - 1);
            long diff = currentEvent.getTimestamp().getTime() - previousEvent.getTimestamp().getTime();
            if (diff <= 86400000) {
                streakCount++;
            } else {
                break;
            }
        }

        streakTextView.setText(String.valueOf(streakCount));
    }

    private void calculateLongestStreak() {
        if (moodEvents == null || moodEvents.isEmpty()) {
            longestStreakTextView.setText("0");
            return;
        }

        moodEvents.sort(Comparator.naturalOrder());

        int longestStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < moodEvents.size(); i++) {
            long currentTime = moodEvents.get(i).getTimestamp().getTime();
            long previousTime = moodEvents.get(i - 1).getTimestamp().getTime();

            if (currentTime - previousTime <= 86400000) {
                currentStreak++;
            } else {
                longestStreak = Math.max(longestStreak, currentStreak);
                currentStreak = 1;
            }
        }

        longestStreak = Math.max(longestStreak, currentStreak);
        longestStreakTextView.setText(String.valueOf(longestStreak));
    }

    private PieData generatePieData() {
        ArrayList<PieEntry> entries = new ArrayList<>();

        if (moodEvents != null && !moodEvents.isEmpty()) {
            Map<String, Integer> freqMap = new HashMap<>();
            for (MoodEvent event : moodEvents) {
                String mood = event.getEmotionalState().toString();
                if (mood != null) {
                    freqMap.put(mood, freqMap.getOrDefault(mood, 0) + 1);
                }
            }
            for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }
        } else {
            entries.add(new PieEntry(1f, "No Data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.2f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueLineColor(Color.WHITE);
        dataSet.setValueLineWidth(2f);

        return new PieData(dataSet);
    }

    private void drawPieChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(60f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterTextSize(14f);
        pieChart.setDrawEntryLabels(false);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setTextColor(Color.WHITE);

        PieData data = generatePieData();
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private BarData generateBarData() {
        if (moodEvents == null || moodEvents.isEmpty()) {
            return new BarData();
        }

        monthMoodMap.clear();
        sortedMonthKeys.clear();

        List<String> allMoods = new ArrayList<>();

        for (MoodEvent event : moodEvents) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(event.getTimestamp());
            int year = cal.get(java.util.Calendar.YEAR);
            int month = cal.get(java.util.Calendar.MONTH) + 1;

            String yearMonthKey = String.format("%04d-%02d", year, month);

            monthMoodMap.putIfAbsent(yearMonthKey, new HashMap<>());
            Map<String, Integer> moodCountMap = monthMoodMap.get(yearMonthKey);

            String moodStr = event.getEmotionalState().toString();
            if (moodStr != null) {
                moodCountMap.put(moodStr, moodCountMap.getOrDefault(moodStr, 0) + 1);

                if (!allMoods.contains(moodStr)) {
                    allMoods.add(moodStr);
                }
            }
        }

        allMoods.sort(String::compareTo);

        sortedMonthKeys.addAll(monthMoodMap.keySet());
        sortedMonthKeys.sort(String::compareTo);

        List<BarEntry> barEntries = new ArrayList<>();

        for (int i = 0; i < sortedMonthKeys.size(); i++) {
            String monthKey = sortedMonthKeys.get(i);
            Map<String, Integer> moodCountMap = monthMoodMap.get(monthKey);

            float[] stackValues = new float[allMoods.size()];
            for (int m = 0; m < allMoods.size(); m++) {
                String mood = allMoods.get(m);
                int count = moodCountMap.getOrDefault(mood, 0);
                stackValues[m] = count;
            }
            barEntries.add(new BarEntry(i, stackValues));
        }

        com.github.mikephil.charting.data.BarDataSet dataSet =
                new com.github.mikephil.charting.data.BarDataSet(barEntries, "");
        dataSet.setStackLabels(allMoods.toArray(new String[0]));
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        return barData;
    }

    private void drawBarChart() {
        BarData barData = generateBarData();

        barChart.setData(barData);

        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawValueAboveBar(true);

        com.github.mikephil.charting.components.XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < sortedMonthKeys.size()) {
                    return convertMonthKeysToMonthNames(sortedMonthKeys.get(index));
                }
                return "";
            }
        });

        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisRight().setTextColor(Color.WHITE);

        Legend legend = barChart.getLegend();
        legend.setTextColor(Color.WHITE);

        barChart.invalidate();
    }

    private String convertMonthKeysToMonthNames(String yearMonthKey) {
        String[] monthNames = new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        String[] parts = yearMonthKey.split("-");
        int monthIndex = Integer.parseInt(parts[1]) - 1;
        return monthNames[monthIndex] + " " + parts[0];
    }
}
