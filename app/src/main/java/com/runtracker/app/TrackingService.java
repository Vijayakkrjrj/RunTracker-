package com.runtracker.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import android.location.Location;

public class TrackingService extends Service {

    private static final String CHANNEL_ID =
            "run_tracker_channel";

    private FusedLocationProviderClient locationClient;

    private Location lastLocation;

    private float totalDistance = 0f;

    private long startTime = 0;
    private long accumulatedTime = 0;

    private boolean running = false;
    private boolean paused = false;

    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        locationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        createNotificationChannel();

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(
                    LocationResult result) {

                if (!running || paused) {
                    return;
                }

                for (Location location :
                        result.getLocations()) {

                    if (lastLocation != null) {

                        float distance =
                                lastLocation.distanceTo(location);

                        if (distance > 0.5f &&
                                distance < 100f) {

                            totalDistance += distance;
                        }
                    }

                    lastLocation = location;

                    sendUpdate();
                }
            }
        };
    }

    private void createNotificationChannel() {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Run Tracker",
                        NotificationManager.IMPORTANCE_LOW
                );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        manager.createNotificationChannel(channel);
    }

    private Notification createNotification() {

        return new Notification.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle("Run Tracker")
                .setContentText(
                        "GPS tracking is active"
                )
                .setSmallIcon(
                        android.R.drawable.ic_menu_mylocation
                )
                .setOngoing(true)
                .build();
    }

    private void startTracking() {

        running = true;
        paused = false;

        totalDistance = 0f;
        accumulatedTime = 0;
        lastLocation = null;

        startTime =
                System.currentTimeMillis();

        startForeground(
                1,
                createNotification()
        );

        LocationRequest request =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        2000
                )
                .setMinUpdateDistanceMeters(2)
                .build();

        try {

            locationClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
            );

        } catch (SecurityException e) {

            stopSelf();
            return;
        }

        sendUpdate();
    }

    private void pauseTracking() {

        if (!running) {
            return;
        }

        if (!paused) {

            accumulatedTime +=
                    System.currentTimeMillis()
                            - startTime;

            paused = true;

            locationClient.removeLocationUpdates(
                    locationCallback
            );

        } else {

            startTime =
                    System.currentTimeMillis();

            paused = false;

            LocationRequest request =
                    new LocationRequest.Builder(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            2000
                    )
                    .setMinUpdateDistanceMeters(2)
                    .build();

            try {

                locationClient.requestLocationUpdates(
                        request,
                        locationCallback,
                        Looper.getMainLooper()
                );

            } catch (SecurityException e) {

                stopSelf();
                return;
            }
        }

        sendUpdate();
    }

    private void stopTracking() {

        if (!running) {
            stopSelf();
            return;
        }

        if (!paused) {

            accumulatedTime +=
                    System.currentTimeMillis()
                            - startTime;
        }

        running = false;
        paused = false;

        locationClient.removeLocationUpdates(
                locationCallback
        );

        sendUpdate();

        stopForeground(
                STOP_FOREGROUND_REMOVE
        );

        stopSelf();
    }

    private long getElapsedTime() {

        if (!running) {
            return accumulatedTime;
        }

        if (paused) {
            return accumulatedTime;
        }

        return accumulatedTime +
                (System.currentTimeMillis()
                        - startTime);
    }

    private void sendUpdate() {

        Intent intent =
                new Intent("RUN_TRACKER_UPDATE");

        intent.setPackage(getPackageName());

        intent.putExtra(
                "distance",
                totalDistance
        );

        intent.putExtra(
                "time",
                getElapsedTime()
        );

        intent.putExtra(
                "running",
                running
        );

        intent.putExtra(
                "paused",
                paused
        );

        if (lastLocation != null &&
                !paused) {

            intent.putExtra(
                    "speed",
                    lastLocation.getSpeed()
            );

        } else {

            intent.putExtra(
                    "speed",
                    0f
            );
        }

        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (intent != null) {

            String action =
                    intent.getAction();

            if ("START".equals(action)) {

                startTracking();

            } else if ("PAUSE".equals(action)) {

                pauseTracking();

            } else if ("STOP".equals(action)) {

                stopTracking();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        if (locationClient != null &&
                locationCallback != null) {

            locationClient.removeLocationUpdates(
                    locationCallback
            );
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
            }
