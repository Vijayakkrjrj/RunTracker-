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

    // Route points of current run
    private JSONArray routePoints;

    @Override
    public void onCreate() {
        super.onCreate();

        locationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        createNotificationChannel();

        routePoints = new JSONArray();

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

        routePoints = new JSONArray();

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
                finalTime
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

        try {

            JSONObject point =
                    new JSONObject();

            point.put(
                    "latitude",
                    location.getLatitude()
            );

            point.put(
                    "longitude",
                    location.getLongitude()
            );

            routePoints.put(point);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void saveRun(
            float distance,
            long time) {

        try {

            SharedPreferences preferences =
                    getSharedPreferences(
                            "run_history",
                            MODE_PRIVATE
                    );

            String oldHistory =
                    preferences.getString(
                            "runs",
                            "[]"
                    );

            JSONArray runs =
                    new JSONArray(oldHistory);

            JSONObject run =
                    new JSONObject();

            float distanceKm =
                    distance / 1000f;

            float averageSpeed = 0f;

            if (time > 0 && distance > 0) {

                averageSpeed =
                        distanceKm
                                / (time / 3600000f);
            }

            float paceSecondsPerKm = 0f;

            if (distanceKm > 0) {

                paceSecondsPerKm =
                        (time / 1000f)
                                / distanceKm;
            }

            run.put(
                    "date",
                    new SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                    ).format(new Date())
            );

            run.put(
                    "distance",
                    distanceKm
            );

            run.put(
                    "time",
                    time
            );

            run.put(
                    "averageSpeed",
                    averageSpeed
            );

            run.put(
                    "pace",
                    paceSecondsPerKm
            );

            // Save complete GPS route
            run.put(
                    "route",
                    routePoints
            );

            runs.put(run);

            preferences
                    .edit()
                    .putString(
                            "runs",
                            runs.toString()
                    )
                    .apply();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void sendLocationUpdate(
            Location location) {

        Intent intent =
                new Intent(
                        "RUN_TRACKER_LOCATION"
                );

        intent.setPackage(
                getPackageName()
        );

        intent.putExtra(
                "latitude",
                location.getLatitude()
        );

        intent.putExtra(
                "longitude",
                location.getLongitude()
        );

        intent.putExtra(
                "distance",
                totalDistance
        );

        intent.putExtra(
                "time",
                getElapsedTime()
        );

        intent.putExtra(
                "speed",
                location.getSpeed()
        );

        intent.putExtra(
                "running",
                running
        );

        intent.putExtra(
                "paused",
                paused
        );

        sendBroadcast(intent);

        sendUpdate();
    }
