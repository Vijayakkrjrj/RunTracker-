package com.runtracker.app;

import android.app.Activity;
import android.os.Bundle;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

public class MapActivity extends Activity {

    private MapView mapView;
    private Polyline routeLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance()
                .setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);

        mapView.setTileSource(
                TileSourceFactory.MAPNIK
        );

        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        mapView.getController().setZoom(16.0);

        routeLine = new Polyline();
        routeLine.setWidth(8f);

        mapView.getOverlays().add(routeLine);
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
