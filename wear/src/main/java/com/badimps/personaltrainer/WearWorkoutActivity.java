package com.badimps.personaltrainer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;

/** Premium glanceable controller for the phone-owned guided workout session. */
public final class WearWorkoutActivity extends Activity implements DataClient.OnDataChangedListener {
    private static final String STATE_PATH = "/workout/state";
    private static final String COMMAND_PATH = "/workout/command";
    private final int navy = Color.rgb(7, 24, 42), mint = Color.rgb(102, 231, 204), coral = Color.rgb(255, 107, 93);
    private LinearLayout root;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        showWaiting();
    }

    @Override protected void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
        Wearable.getDataClient(this).getDataItems().addOnSuccessListener(items -> {
            for (DataItem item : items) if (STATE_PATH.equals(item.getUri().getPath())) render(DataMapItem.fromDataItem(item));
            items.release();
        });
    }

    @Override protected void onPause() {
        Wearable.getDataClient(this).removeListener(this);
        super.onPause();
    }

    @Override public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event : events) if (event.getType() == DataEvent.TYPE_CHANGED
                && STATE_PATH.equals(event.getDataItem().getUri().getPath())) render(DataMapItem.fromDataItem(event.getDataItem()));
    }

    private void render(DataMapItem item) {
        boolean active = item.getDataMap().getBoolean("active", false);
        if (!active) { showWaiting(); return; }
        boolean paused = item.getDataMap().getBoolean("paused", false);
        String exercise = item.getDataMap().getString("exercise", "Workout");
        int set = item.getDataMap().getInt("set", 1), sets = item.getDataMap().getInt("sets", 1);
        int index = item.getDataMap().getInt("exercise_index", 0), count = item.getDataMap().getInt("exercise_count", 1);
        base();
        label("LIVE WORKOUT", 12, mint);
        label(exercise, 23, Color.WHITE);
        label("SET " + set + " OF " + sets, 13, Color.LTGRAY);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(1, count)); progress.setProgress(Math.min(count, index + 1)); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(mint));
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(8)));
        label((index + 1) + " / " + count + " exercises", 12, Color.LTGRAY);
        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER); controls.setPadding(0, dp(8), 0, 0);
        controls.addView(control("⏮", "previous", navy));
        controls.addView(control(paused ? "▶" : "⏸", "toggle", coral));
        controls.addView(control("✓", "complete", mint));
        root.addView(controls);
        Button end = control("■", "end", navy); end.setContentDescription("End workout"); root.addView(end, new LinearLayout.LayoutParams(dp(52), dp(42)));
    }

    private void showWaiting() {
        base();
        label("MY PERSONAL TRAINER", 12, mint);
        label("No workout active", 22, Color.WHITE);
        label("Start your planned workout on the phone. This watch will connect automatically.", 13, Color.LTGRAY);
    }

    private void base() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(18), dp(24), dp(18)); root.setBackgroundColor(navy); setContentView(root);
    }

    private void label(String value, int size, int color) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(size); text.setTextColor(color); text.setGravity(Gravity.CENTER);
        text.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)); text.setPadding(0, dp(3), 0, dp(3)); root.addView(text);
    }

    private Button control(String glyph, String command, int color) {
        Button button = new Button(this); button.setText(glyph); button.setTextSize(20); button.setTextColor(color == mint ? navy : Color.WHITE);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        button.setContentDescription(command + " workout"); button.setOnClickListener(view -> send(command));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(58), dp(52)); params.setMargins(dp(4), 0, dp(4), 0); button.setLayoutParams(params);
        return button;
    }

    private void send(String command) {
        Wearable.getNodeClient(this).getConnectedNodes().addOnSuccessListener(nodes -> {
            for (com.google.android.gms.wearable.Node node : nodes) Wearable.getMessageClient(this)
                    .sendMessage(node.getId(), COMMAND_PATH, command.getBytes(StandardCharsets.UTF_8));
        });
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
