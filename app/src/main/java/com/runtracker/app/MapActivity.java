package com.runtracker.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class MapActivity extends Activity {

    private MapView mapView;
    private Polyline routeLine;
    private Marker currentMarker;

    private ArrayList<GeoPoint> routePoints;

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

            routeLine.setPoints(routePoints);

            currentMarker.setPosition(point);

            mapView.getController()
                    .setCenter(point);

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
    }

    @Override
    protected void onStart() {

        super.onStart();

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

    @Override
    protected void onStop() {

        unregisterReceiver(
                locationReceiver
        );

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
