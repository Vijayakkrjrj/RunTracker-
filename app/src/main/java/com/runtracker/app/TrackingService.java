package com.runtracker.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    // Current run route
    private JSONArray currentRoute;

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

                    saveRoutePoint(location);

                    sendLocationUpdate(location);
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

        currentRoute = new JSONArray();

        startTime =
                System.currentTimeMillis();

        startForeground(
                1,
                createNotification()
        );

        requestLocationUpdates();

        sendUpdate();
    }

    private void requestLocationUpdates() {

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
        }
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

            requestLocationUpdates();
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

        long finalTime =
                accumulatedTime;

        saveRun(
                totalDistance,
                finalTime,
                currentRoute
        );

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
                (
                        System.currentTimeMillis()
                                - startTime
                );
    }

    private void saveRoutePoint(
            Location location) {

        if (currentRoute == null) {
            currentRoute = new JSONArray();
        }

        try {

            JSONObject point =
                   
