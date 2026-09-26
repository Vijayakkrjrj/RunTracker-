package com.runtracker.app;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView distanceText;
    private TextView timeText;
    private TextView speedText;

    private Button startButton;
    private Button pauseButton;
    private Button stopButton;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (!"RUN_TRACKER_UPDATE".equals(intent.getAction())) {
                return;
            }

            float distance = intent.getFloatExtra("distance", 0f);
            long time = intent.getLongExtra("time", 0);
            float speed = intent.getFloatExtra("speed", 0f);

            boolean running =
                    intent.getBooleanExtra("running", false);

            boolean paused =
                    intent.getBooleanExtra("paused", false);

            distanceText.setText(
                    String.format(
                            "Distance: %.2f km",
                            distance / 1000f
                    )
            );

            long totalSeconds = time / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;

            timeText.setText(
                    String.format(
                            "Time: %02d:%02d",
                            minutes,
                            seconds
                    )
            );

            speedText.setText(
                    String.format(
                            "Speed: %.1f km/h",
                            speed * 3.6f
                    )
            );

            if (!running) {
                startButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                pauseButton.setText("PAUSE");
            }
            else if (paused) {
                startButton.setEnabled(false);
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
                pauseButton.setText("RESUME");
            }
            else {
                startButton.setEnabled(false);
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
                pauseButton.setText("PAUSE");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        distanceText = findViewById(R.id.distanceText);
        timeText = findViewById(R.id.timeText);
        speedText = findViewById(R.id.speedText);

        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        stopButton = findViewById(R.id.stopButton);

        requestPermissionsIfNeeded();

        startButton.setOnClickListener(v -> startTracking());

        pauseButton.setOnClickListener(v -> pauseResumeTracking());

        stopButton.setOnClickListener(v -> stopTracking());
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter =
                new IntentFilter("RUN_TRACKER_UPDATE");

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    updateReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(
                    updateReceiver,
                    filter
            );
        }
    }

    @Override
    protected void onStop() {
        unregisterReceiver(updateReceiver);
        super.onStop();
    }

    private void requestPermissionsIfNeeded() {

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    100
            );
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    101
            );
        }
    }

    private void startTracking() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionsIfNeeded();
            return;
        }

        Intent intent =
                new Intent(this, TrackingService.class);

        intent.setAction("START");

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void pauseResumeTracking() {

        Intent intent =
                new Intent(this, TrackingService.class);

        intent.setAction("PAUSE");

        startService(intent);
    }

    private void stopTracking() {

        Intent intent =
                new Intent(this, TrackingService.class);

        intent.setAction("STOP");

        startService(intent);
    }
                }
