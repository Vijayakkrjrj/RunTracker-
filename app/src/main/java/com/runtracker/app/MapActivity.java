package com.runtracker.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class MapActivity extends Activity {

    private MapView mapView;

    private Polyline routeLine;

    private Marker currentMarker;
    private Marker startMarker;
    private Marker endMarker;

    private ArrayList<GeoPoint> routePoints;

    private boolean showingSavedRoute = false;

    private final BroadcastReceiver locationReceiver =
            new BroadcastReceiver() {

        @Override
        public void onReceive(
                Context context,
                Intent intent) {

            if (!"RUN_TRACKER_LOCATION".equals(
                    intent.getAction())) {

                return;
            }

            if (showingSavedRoute) {
                return;
            }

            double latitude =
                    intent.getDoubleExtra(
                            "latitude",
                            0
                    );

            double longitude =
                    intent.getDoubleExtra(
                            "longitude",
                            0
                    );

            boolean running =
                    intent.getBooleanExtra(
                            "running",
                            false
                    );

            boolean paused =
                    intent.getBooleanExtra(
                            "paused",
                            false
                    );

            if (!running || paused) {
                return;
            }

            GeoPoint point =
                    new GeoPoint(
                            latitude,
                            longitude
                    );

            routePoints.add(point);

            routeLine.setPoints(
                    routePoints
            );

            updateCurrentMarker(point);

            if (routePoints.size() == 1) {

                setStartMarker(point);

                mapView.getController()
                        .setCenter(point);
            }

            mapView.invalidate();
        }
    };

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Configuration.getInstance()
                .setUserAgentValue(
                        getPackageName()
                );

        setContentView(
                R.layout.activity_map
        );

        mapView =
                findViewById(
                        R.id.mapView
                );

        mapView.setTileSource(
                TileSourceFactory.MAPNIK
        );

        mapView.setMultiTouchControls(true);

        mapView.setBuiltInZoomControls(true);

        mapView.getController()
                .setZoom(17.0);

        routePoints =
                new ArrayList<>();

        routeLine =
                new Polyline();

        routeLine.setWidth(8f);

        mapView.getOverlays()
                .add(routeLine);

        currentMarker =
                new Marker(mapView);

        currentMarker.setTitle(
                "Current Location"
        );

        mapView.getOverlays()
                .add(currentMarker);

        checkForSavedRoute();
    }

    private void checkForSavedRoute() {

        Intent intent =
                getIntent();

        if (intent == null) {
            return;
        }

        String savedRoute =
                intent.getStringExtra(
                        "saved_route"
                );

        if (savedRoute == null ||
                savedRoute.isEmpty()) {

            return;
        }

        showingSavedRoute = true;

        loadSavedRoute(savedRoute);
    }

    private void loadSavedRoute(
            String savedRoute) {

        try {

            JSONArray route =
                    new JSONArray(savedRoute);

            routePoints.clear();

            for (int i = 0;
                    i < route.length();
                    i++) {

                JSONObject point =
                        route.getJSONObject(i);

                double latitude =
                        point.optDouble(
                                "latitude",
                                0
                        );

                double longitude =
                        point.optDouble(
                                "longitude",
                                0
                        );

                if (latitude == 0 &&
                        longitude == 0) {

                    continue;
                }

                GeoPoint geoPoint =
                        new GeoPoint(
                                latitude,
                                longitude
                        );

                routePoints.add(
                        geoPoint
                );
            }

            if (routePoints.isEmpty()) {
                return;
            }

            routeLine.setPoints(
                    routePoints
            );

            GeoPoint firstPoint =
                    routePoints.get(0);

            GeoPoint lastPoint =
                    routePoints.get(
                            routePoints.size() - 1
                    );

            setStartMarker(
                    firstPoint
            );

            setEndMarker(
                    lastPoint
            );

            currentMarker.setPosition(
                    lastPoint
            );

            /*
             * Automatically fit the complete
             * saved route on the screen.
             */
            BoundingBox boundingBox =
                    BoundingBox.fromGeoPoints(
                            routePoints
                    );

            mapView.zoomToBoundingBox(
                    boundingBox,
                    true,
                    80
            );

            mapView.invalidate();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void setStartMarker(
            GeoPoint point) {

        if (startMarker != null) {

            mapView.getOverlays()
                    .remove(startMarker);
        }

        startMarker =
                new Marker(mapView);

        startMarker.setPosition(
                point
        );

        startMarker.setTitle(
                "Start"
        );

        mapView.getOverlays()
                .add(startMarker);
    }

    private void setEndMarker(
            GeoPoint point) {

        if (endMarker != null) {

            mapView.getOverlays()
                    .remove(endMarker);
        }

        endMarker =
                new Marker(mapView);

        endMarker.setPosition(
                point
        );

        endMarker.setTitle(
                "Finish"
        );

        mapView.getOverlays()
                .add(endMarker);
    }

    private void updateCurrentMarker(
            GeoPoint point) {

        if (currentMarker == null) {

            currentMarker =
                    new Marker(mapView);

            currentMarker.setTitle(
                    "Current Location"
            );

            mapView.getOverlays()
                    .add(currentMarker);
        }

        currentMarker.setPosition(
                point
        );
    }

    @Override
    protected void onStart() {

        super.onStart();

        if (!showingSavedRoute) {

            IntentFilter filter =
                    new IntentFilter(
                            "RUN_TRACKER_LOCATION"
                    );

            if (Build.VERSION.SDK_INT >= 33) {

                registerReceiver(
                        locationReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );

            } else {

                registerReceiver(
                        locationReceiver,
                        filter
                );
            }
        }
    }

    @Override
    protected void onStop() {

        if (!showingSavedRoute) {

            try {

                unregisterReceiver(
                        locationReceiver
                );

            } catch (IllegalArgumentException e) {

                e.printStackTrace();
            }
        }

        super.onStop();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (mapView != null) {

            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {

        if (mapView != null) {

            mapView.onPause();
        }

        super.onPause();
    }
        }
