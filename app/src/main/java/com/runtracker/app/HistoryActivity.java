package com.runtracker.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class HistoryActivity extends Activity {

    private ListView historyList;
    private Button clearButton;

    private ArrayList<String> historyItems;

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history);

        historyList =
                findViewById(R.id.historyList);

        clearButton =
                findViewById(R.id.clearButton);

        historyItems =
                new ArrayList<>();

        adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        historyItems
                );

        historyList.setAdapter(adapter);

        loadHistory();

        clearButton.setOnClickListener(v -> {

            getSharedPreferences(
                    "run_history",
                    MODE_PRIVATE
            )
            .edit()
            .remove("runs")
            .apply();

            historyItems.clear();

            adapter.notifyDataSetChanged();
        });
    }

    private void loadHistory() {

        historyItems.clear();

        SharedPreferences preferences =
                getSharedPreferences(
                        "run_history",
                        MODE_PRIVATE
                );

        String history =
                preferences.getString(
                        "runs",
                        "[]"
                );

        try {

            JSONArray runs =
                    new JSONArray(history);

            for (int i = runs.length() - 1;
                 i >= 0;
                 i--) {

                JSONObject run =
                        runs.getJSONObject(i);

                String date =
                        run.optString(
                                "date",
                                ""
                        );

                double distance =
                        run.optDouble(
                                "distance",
                                0
                        );

                long time =
                        run.optLong(
                                "time",
                                0
                        );

                double averageSpeed =
                        run.optDouble(
                                "averageSpeed",
                                0
                        );

                double pace =
                        run.optDouble(
                                "pace",
                                0
                        );

                long totalSeconds =
                        time / 1000;

                long minutes =
                        totalSeconds / 60;

                long seconds =
                        totalSeconds % 60;

                int paceMinutes =
                        (int) (pace / 60);

                int paceSeconds =
                        (int) (pace % 60);

                String item =
                        date +
                        "\n" +
                        String.format(
                                Locale.getDefault(),
                                "Distance: %.2f km",
                                distance
                        ) +
                        "\n" +
                        String.format(
                                Locale.getDefault(),
                                "Time: %02d:%02d",
                                minutes,
                                seconds
                        ) +
                        "\n" +
                        String.format(
                                Locale.getDefault(),
                                "Average: %.1f km/h",
                                averageSpeed
                        ) +
                        "\n" +
                        String.format(
                                Locale.getDefault(),
                                "Pace: %02d:%02d min/km",
                                paceMinutes,
                                paceSeconds
                        );

                historyItems.add(item);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        adapter.notifyDataSetChanged();
    }
                  }
