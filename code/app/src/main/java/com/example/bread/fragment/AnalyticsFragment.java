package com.example.bread.fragment;

import static com.github.mikephil.charting.utils.ColorTemplate.rgb;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bread.R;
import com.example.bread.model.MoodEvent;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AnalyticsFragment
 *
 * Role / Purpose:
 * A Fragment that visualizes the user's mood data using interactive charts. Displays:
 * - Current streak and longest streak of daily mood logging.
 * - A pie chart representing the frequency of each emotional state.
 * - A stacked bar chart showing monthly mood distributions for the current year.
 * - A line chart with a 30-day trend and 7-day moving average of mood scores.
 *
 * The mood events are passed in as arguments via the `newInstance()` factory method and
 * rendered using the MPAndroidChart library.
 *
 * Design Patterns:
 * - Factory Pattern: Uses a static `newInstance()` method to create a configured fragment instance.
 * - MVC Pattern: Fragment acts as a controller, coordinating data processing and chart rendering.
 * - Observer Pattern: UI updates react to data passed in via arguments (though not LiveData or ViewModel-based).
 * - Strategy Pattern (conceptually): Different chart types encapsulate different data visualization strategies.
 *
 * Outstanding Issues:
 * - Chart interactions (e.g., tapping, zooming) are not enabled or configured.
 * - Data passed via arguments must implement Serializable; may be better with Parcelable or ViewModel for large datasets.
 * - Streak calculations assume fixed 24-hour periods; may not account for timezone or edge cases.
 * - Pie chart, bar chart, and line chart could be extracted to helper methods or a utility class for better separation of concerns.
 * - No unit tests or input validation on mood data consistency.
 */

public class AnalyticsFragment extends Fragment {

    private static final String ARG_PARAM1 = "moodEvents";

    private List<MoodEvent> moodEvents;
    private TextView streakTextView, longestStreakTextView;
    PieChart pieChart;
    BarChart barChart;
    LineChart lineChart;
    private final Map<String, Map<String, Integer>> monthMoodMap = new HashMap<>();
    private final List<String> sortedMonthKeys = new ArrayList<>();

    public AnalyticsFragment() {
        // Required empty public constructor
    }

    public static final int[] MOOD_COLORS = {
            rgb("#FFE066"), rgb("#A9D6E5"), rgb("#FF6B6B"), rgb("#BDB2FF"), rgb("#D3D3D3"), rgb("#FDFFB6"),
            rgb("#9BF6FF"), rgb("#FFC6FF"), rgb("#70F473"), rgb("#FFFFFF")
    };

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
        lineChart = view.findViewById(R.id.line_monthly_chart);
        ImageView closeButton = view.findViewById(R.id.analytics_close_button);
        closeButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().setCustomAnimations(
                    R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
            ).remove(AnalyticsFragment.this).commit();
        });

        calculateStreak();
        calculateLongestStreak();

        drawPieChart();
        drawBarChart();
        drawLineChart();

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
        dataSet.setColors(MOOD_COLORS);
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

        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        monthMoodMap.clear();
        sortedMonthKeys.clear();

        List<String> allMoods = new ArrayList<>();

        for (MoodEvent event : moodEvents) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(event.getTimestamp());
            int year = cal.get(java.util.Calendar.YEAR);

            // Only process events from the current year
            if (year != currentYear) {
                continue;
            }

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
        dataSet.setColors(MOOD_COLORS);
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

        barChart.setExtraBottomOffset(16f);

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

    private LineData generateLineData() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        // Set to midnight today for consistency
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        java.util.Date today = cal.getTime();
        java.util.Calendar startCal = (java.util.Calendar) cal.clone();
        startCal.add(java.util.Calendar.DAY_OF_MONTH, -29);
        java.util.Date startDate = startCal.getTime();

        // Prepare maps to aggregate scores per day (using day midnight timestamp as key)
        Map<Long, Float> dailySum = new HashMap<>();
        Map<Long, Integer> dailyCount = new HashMap<>();

        // Aggregate mood events that fall within the last 30 days
        for (MoodEvent event : moodEvents) {
            java.util.Date eventDate = event.getTimestamp();
            // Only include events within our time window
            if (eventDate.before(startDate) || eventDate.after(today)) {
                continue;
            }
            // Normalize event date to midnight
            java.util.Calendar eventCal = java.util.Calendar.getInstance();
            eventCal.setTime(eventDate);
            eventCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            eventCal.set(java.util.Calendar.MINUTE, 0);
            eventCal.set(java.util.Calendar.SECOND, 0);
            eventCal.set(java.util.Calendar.MILLISECOND, 0);
            long dayKey = eventCal.getTimeInMillis();
            dailySum.put(dayKey, dailySum.getOrDefault(dayKey, 0f) + event.getScore());
            dailyCount.put(dayKey, dailyCount.getOrDefault(dayKey, 0) + 1);
        }

        List<Float> dailyAverages = new ArrayList<>();
        List<com.github.mikephil.charting.data.Entry> rawEntries = new ArrayList<>();
        int totalDays = 30;
        java.util.Calendar iterCal = (java.util.Calendar) startCal.clone();

        for (int i = 0; i < totalDays; i++) {
            long dayKey = iterCal.getTimeInMillis();
            float average = 0f;
            if (dailyCount.containsKey(dayKey)) {
                average = dailySum.get(dayKey) / dailyCount.get(dayKey);
            }
            dailyAverages.add(average);
            // x value is the day index (0-based)
            rawEntries.add(new com.github.mikephil.charting.data.Entry(i, average));
            iterCal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        // Calculate 7-day moving average (smoothing out daily fluctuations)
        List<com.github.mikephil.charting.data.Entry> movingAvgEntries = new ArrayList<>();
        int window = 7;
        for (int i = 0; i < dailyAverages.size(); i++) {
            int startWindow = Math.max(0, i - window + 1);
            float sum = 0f;
            int count = 0;
            for (int j = startWindow; j <= i; j++) {
                sum += dailyAverages.get(j);
                count++;
            }
            float movingAvg = sum / count;
            movingAvgEntries.add(new com.github.mikephil.charting.data.Entry(i, movingAvg));
        }

        com.github.mikephil.charting.data.LineDataSet rawDataSet =
                new com.github.mikephil.charting.data.LineDataSet(rawEntries, "Daily Average");
        rawDataSet.setColor(Color.WHITE);
        rawDataSet.setLineWidth(2f);
        rawDataSet.setCircleRadius(3f);
        rawDataSet.setDrawCircleHole(false);
        rawDataSet.setDrawCircles(false);
        rawDataSet.setValueTextSize(10f);
        rawDataSet.setDrawValues(false);
        rawDataSet.setValueTextColor(Color.WHITE);
        rawDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        com.github.mikephil.charting.data.LineDataSet movingAvgDataSet =
                new com.github.mikephil.charting.data.LineDataSet(movingAvgEntries, "7-Day Moving Average");
        movingAvgDataSet.setColor(Color.CYAN);
        movingAvgDataSet.setLineWidth(3f);
        movingAvgDataSet.setDrawCircles(false);
        movingAvgDataSet.setDrawValues(false);
        movingAvgDataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER);

        return new com.github.mikephil.charting.data.LineData(rawDataSet, movingAvgDataSet);
    }

    private void drawLineChart() {
        com.github.mikephil.charting.data.LineData lineData = generateLineData();
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);

        com.github.mikephil.charting.components.XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -29);
        final java.util.Date startDate = calendar.getTime();
        final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d");

        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(startDate);
                cal.add(java.util.Calendar.DAY_OF_MONTH, (int) value);
                return sdf.format(cal.getTime());
            }
        });

        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisRight().setTextColor(Color.WHITE);
        lineChart.setExtraBottomOffset(16f);

        Legend legend = lineChart.getLegend();
        legend.setTextColor(Color.WHITE);

        lineChart.invalidate();
    }
}
