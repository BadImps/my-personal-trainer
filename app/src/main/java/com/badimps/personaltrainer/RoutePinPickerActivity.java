package com.badimps.personaltrainer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** A deliberately simple Google Maps pin picker for an Endurance start or destination. */
public final class RoutePinPickerActivity extends Activity implements OnMapReadyCallback {
    public static final String EXTRA_QUERY = "route_pin_query";
    public static final String EXTRA_LABEL = "route_pin_label";
    public static final String EXTRA_ADDRESS = "route_pin_address";
    public static final String EXTRA_LATITUDE = "route_pin_latitude";
    public static final String EXTRA_LONGITUDE = "route_pin_longitude";

    private static final int NAVY = Color.rgb(10, 28, 50);
    private static final int MINT = Color.rgb(111, 231, 207);
    private static final int PAPER = Color.rgb(247, 250, 253);
    private MapView mapView;
    private GoogleMap map;
    private EditText search;
    private TextView status;
    private LatLng selected;
    private String selectedAddress = "";

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        buildUi();
        mapView.onCreate(state);
        mapView.getMapAsync(this);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(NAVY);

        String label = getIntent().getStringExtra(EXTRA_LABEL);
        TextView title = new TextView(this);
        title.setText("Pin " + (label == null ? "location" : label.toLowerCase(Locale.ROOT)));
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView help = new TextView(this);
        help.setText("Search a place, or tap the map to drop the pin exactly where you want it.");
        help.setTextSize(14);
        help.setTextColor(Color.rgb(214, 229, 242));
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, -2);
        helpParams.topMargin = dp(4);
        root.addView(help, helpParams);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search address or landmark");
        search.setText(getIntent().getStringExtra(EXTRA_QUERY));
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        search.setTextColor(NAVY);
        search.setHintTextColor(Color.rgb(89, 112, 135));
        search.setPadding(dp(12), 0, dp(12), 0);
        search.setBackgroundColor(PAPER);
        searchRow.addView(search, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button find = new Button(this);
        find.setText("Find");
        find.setAllCaps(false);
        find.setTextColor(NAVY);
        find.setBackgroundColor(MINT);
        find.setOnClickListener(view -> searchMap());
        LinearLayout.LayoutParams findParams = new LinearLayout.LayoutParams(dp(76), dp(52));
        findParams.leftMargin = dp(8);
        searchRow.addView(find, findParams);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(52));
        searchParams.topMargin = dp(14);
        root.addView(searchRow, searchParams);

        mapView = new MapView(this);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(-1, 0, 1);
        mapParams.topMargin = dp(12);
        root.addView(mapView, mapParams);

        status = new TextView(this);
        status.setText("Tap the map to position your pin.");
        status.setTextColor(Color.rgb(214, 229, 242));
        status.setTextSize(13);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(10);
        root.addView(status, statusParams);

        Button usePin = new Button(this);
        usePin.setText("Use this pin");
        usePin.setAllCaps(false);
        usePin.setTextColor(NAVY);
        usePin.setBackgroundColor(MINT);
        usePin.setOnClickListener(view -> returnPin());
        LinearLayout.LayoutParams useParams = new LinearLayout.LayoutParams(-1, dp(54));
        useParams.topMargin = dp(8);
        root.addView(usePin, useParams);
        setContentView(root);
    }

    @Override public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setCompassEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(10.6918, -61.2225), 11f));
        map.setOnMapClickListener(this::selectPin);
        if (!search.getText().toString().trim().isEmpty()) searchMap();
    }

    private void searchMap() {
        String query = search.getText().toString().trim();
        if (query.isEmpty()) { search.setError("Enter a place to find"); return; }
        status.setText("Finding location on Google Maps…");
        new Thread(() -> {
            try {
                List<Address> results = new Geocoder(this, Locale.getDefault()).getFromLocationName(query, 1);
                runOnUiThread(() -> {
                    if (results == null || results.isEmpty()) { status.setText("No exact location found. Try a fuller address or tap the map."); return; }
                    Address result = results.get(0);
                    selectPin(new LatLng(result.getLatitude(), result.getLongitude()));
                    selectedAddress = addressFor(result);
                });
            } catch (IOException error) {
                runOnUiThread(() -> status.setText("Search is unavailable right now. You can still tap the map to pin a location."));
            }
        }).start();
    }

    private void selectPin(LatLng point) {
        if (map == null) return;
        selected = point;
        selectedAddress = "";
        map.clear();
        map.addMarker(new MarkerOptions().position(point).title("Selected location"));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f));
        status.setText(String.format(Locale.US, "Pin selected · %.5f, %.5f", point.latitude, point.longitude));
        reverseGeocode(point);
    }

    private void reverseGeocode(LatLng point) {
        new Thread(() -> {
            try {
                List<Address> results = new Geocoder(this, Locale.getDefault()).getFromLocation(point.latitude, point.longitude, 1);
                if (results == null || results.isEmpty()) return;
                String resolved = addressFor(results.get(0));
                runOnUiThread(() -> {
                    if (selected != null && selected.latitude == point.latitude && selected.longitude == point.longitude) {
                        selectedAddress = resolved;
                        status.setText("Pin selected · " + resolved);
                    }
                });
            } catch (IOException ignored) { }
        }).start();
    }

    private void returnPin() {
        if (selected == null) { status.setText("Tap the map or search a place first."); return; }
        Intent result = new Intent();
        result.putExtra(EXTRA_ADDRESS, selectedAddress);
        result.putExtra(EXTRA_LATITUDE, selected.latitude);
        result.putExtra(EXTRA_LONGITUDE, selected.longitude);
        setResult(RESULT_OK, result);
        finish();
    }

    private String addressFor(Address address) {
        String line = address.getAddressLine(0);
        return line == null || line.trim().isEmpty()
                ? String.format(Locale.US, "%.5f, %.5f", address.getLatitude(), address.getLongitude()) : line;
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override protected void onStop() { mapView.onStop(); super.onStop(); }
    @Override protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
