package com.badimps.personaltrainer;

import android.content.SharedPreferences;

import androidx.wear.protolayout.ActionBuilders;
import androidx.wear.protolayout.ColorBuilders;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ModifiersBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.TimelineBuilders;
import androidx.wear.protolayout.material.Text;
import androidx.wear.protolayout.material.Typography;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileService;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/** Glanceable resume surface; detailed planning remains in the phone app. */
public final class CoachWorkoutTileService extends TileService {
    private static final String RESOURCES_VERSION = "1";

    @Override public ListenableFuture<TileBuilders.Tile> onTileRequest(RequestBuilders.TileRequest request) {
        SharedPreferences state = getSharedPreferences("watch_workout_snapshot", MODE_PRIVATE);
        boolean active = state.getBoolean("active", false);
        String exercise = state.getString("exercise", "Ready when you are");
        String detail = active
                ? "Set " + state.getInt("set", 1) + " of " + state.getInt("sets", 1)
                : "Start on your phone";
        ActionBuilders.AndroidActivity activity = new ActionBuilders.AndroidActivity.Builder()
                .setPackageName(getPackageName()).setClassName(WearWorkoutActivity.class.getName()).build();
        ModifiersBuilders.Clickable clickable = new ModifiersBuilders.Clickable.Builder()
                .setId("open_workout").setOnClick(new ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(activity).build()).build();
        LayoutElementBuilders.Column content = new LayoutElementBuilders.Column.Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setModifiers(new ModifiersBuilders.Modifiers.Builder().setClickable(clickable).build())
                .addContent(new Text.Builder(this, active ? "ACTIVE WORKOUT" : "MY PERSONAL TRAINER")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(0xff66e7cc)).build())
                .addContent(new Text.Builder(this, exercise)
                        .setTypography(Typography.TYPOGRAPHY_TITLE2)
                        .setColor(ColorBuilders.argb(0xfff7fbff)).build())
                .addContent(new Text.Builder(this, detail)
                        .setTypography(Typography.TYPOGRAPHY_BODY2)
                        .setColor(ColorBuilders.argb(0xffa8bac8)).build())
                .addContent(new Text.Builder(this, active ? "Tap to resume" : "Tap to open")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                        .setColor(ColorBuilders.argb(0xffff725f)).build())
                .build();
        return Futures.immediateFuture(new TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(content)).build());
    }

    @Override public ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(
            RequestBuilders.ResourcesRequest request) {
        return Futures.immediateFuture(new ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION).build());
    }
}
