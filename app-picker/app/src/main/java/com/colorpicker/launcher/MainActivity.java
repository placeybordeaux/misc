package com.colorpicker.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int GRID_COLUMNS = 4;
    private static final String PREFS = "color_picker_prefs";
    private static final String KEY_MODE = "layout_mode";

    /** The experimental layouts the mode-switch button cycles through. */
    private enum Mode { GROUPED, RAINBOW, WHEEL, USAGE }

    private FrameLayout contentContainer;
    private ProgressBar progress;
    private ImageView toggleView;
    private ImageView modeSwitch;
    private ImageView settingsButton;
    private TextView title;
    private TextView usageBanner;

    private final List<AppInfo> apps = new ArrayList<>();
    private boolean hasUsageAccess = false;

    private Mode mode = Mode.GROUPED;
    private boolean showHeaders = true; // shared by GROUPED/RAINBOW
    private ColorWheelView wheelView;    // kept so back can pop its focus

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentContainer = findViewById(R.id.content_container);
        progress = findViewById(R.id.progress);
        toggleView = findViewById(R.id.toggle_view);
        modeSwitch = findViewById(R.id.mode_switch);
        settingsButton = findViewById(R.id.settings_button);
        title = findViewById(R.id.title);
        usageBanner = findViewById(R.id.usage_banner);

        settingsButton.setOnClickListener(v -> showColorSourceDialog());

        mode = Mode.values()[getPrefs().getInt(KEY_MODE, 0)];

        modeSwitch.setOnClickListener(v -> {
            mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
            getPrefs().edit().putInt(KEY_MODE, mode.ordinal()).apply();
            renderMode();
        });

        toggleView.setOnClickListener(v -> {
            showHeaders = !showHeaders;
            updateToggleIcon();
            renderMode();
        });

        usageBanner.setOnClickListener(v ->
                startActivity(UsageStatsHelper.usageAccessSettingsIntent()));

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
    }

    @Override
    public void onBackPressed() {
        // In the wheel, back first returns from a focused color to the overview.
        if (mode == Mode.WHEEL && wheelView != null && wheelView.popFocus()) return;
        super.onBackPressed();
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Lets the user try the different icon-color extraction strategies; reloads on change. */
    private void showColorSourceDialog() {
        ColorUtils.ColorSource[] sources = ColorUtils.ColorSource.values();
        int current = Settings.getColorSource(this).ordinal();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Color source")
                .setSingleChoiceItems(Settings.colorSourceLabels(), current, (dialog, which) -> {
                    Settings.setColorSource(this, sources[which]);
                    dialog.dismiss();
                    loadApps(); // re-extract colors with the new strategy
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadApps() {
        progress.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        hasUsageAccess = UsageStatsHelper.hasAccess(this);

        AppLoader.loadAsync(this, loaded -> {
            apps.clear();
            apps.addAll(loaded);
            progress.setVisibility(View.GONE);
            contentContainer.setVisibility(View.VISIBLE);
            renderMode();
        });
    }

    /** Rebuilds the content view for the current mode. */
    private void renderMode() {
        contentContainer.removeAllViews();
        wheelView = null;

        boolean listMode = mode == Mode.GROUPED || mode == Mode.RAINBOW;
        toggleView.setVisibility(listMode ? View.VISIBLE : View.GONE);
        updateToggleIcon();

        // Usage banner only matters in the usage mode without access.
        usageBanner.setVisibility(
                mode == Mode.USAGE && !hasUsageAccess ? View.VISIBLE : View.GONE);

        switch (mode) {
            case GROUPED:  title.setText("Apps");    renderGrid(false); break;
            case RAINBOW:  title.setText("Rainbow"); renderGrid(true);  break;
            case WHEEL:    title.setText("Wheel");   renderWheel();      break;
            case USAGE:    title.setText("Usage");   renderUsage();      break;
        }
    }

    private void renderGrid(boolean rainbowBehind) {
        RecyclerView grid = new RecyclerView(this);
        grid.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        grid.setClipToPadding(false);
        grid.setPadding(dp(8), dp(8), dp(8), dp(8));

        AppAdapter adapter = new AppAdapter(this);
        adapter.setShowHeaders(showHeaders);
        adapter.setRainbowBehind(rainbowBehind);

        GridLayoutManager lm = new GridLayoutManager(this, GRID_COLUMNS);
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? GRID_COLUMNS : 1;
            }
        });
        // In Rainbow mode, the rainbow itself is the background; it slides through the spectrum to
        // match your scroll position. Added before the grid so it sits behind it.
        final RainbowBackground rainbowBg = rainbowBehind ? new RainbowBackground(this) : null;
        if (rainbowBg != null) {
            rainbowBg.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            contentContainer.addView(rainbowBg);
        }

        grid.setLayoutManager(lm);
        grid.setAdapter(adapter);
        adapter.setApps(apps);
        contentContainer.addView(grid);

        if (rainbowBg != null) {
            Runnable update = () -> rainbowBg.setColors(visibleSectionColors(lm, adapter));
            grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView rv, int dx, int dy) { update.run(); }
            });
            grid.post(update);
        }
    }

    /** The accent colors of the color sections currently visible, top to bottom (deduped). */
    private int[] visibleSectionColors(GridLayoutManager lm, AppAdapter adapter) {
        int first = lm.findFirstVisibleItemPosition();
        int last = lm.findLastVisibleItemPosition();
        if (first < 0 || last < first) return new int[]{0xFF202020};
        java.util.List<Integer> cols = new java.util.ArrayList<>();
        int prev = 0;
        boolean has = false;
        for (int p = first; p <= last; p++) {
            int c = adapter.groupAccentAt(p);
            if (!has || c != prev) { cols.add(c); prev = c; has = true; }
        }
        int[] out = new int[cols.size()];
        for (int i = 0; i < out.length; i++) out[i] = cols.get(i);
        return out;
    }

    private void renderWheel() {
        wheelView = new ColorWheelView(this);
        wheelView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        wheelView.setOnAppClickListener(this::launch);
        wheelView.setApps(apps);
        contentContainer.addView(wheelView);
    }

    private void renderUsage() {
        RainbowFieldView field = new RainbowFieldView(this);
        field.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        field.setOnAppClickListener(this::launch);
        field.setApps(AppLoader.sortedByUsage(apps), hasUsageAccess);
        contentContainer.addView(field);
    }

    private void launch(AppInfo app) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(app.getPackageName());
        if (intent != null) startActivity(intent);
    }

    private void updateToggleIcon() {
        toggleView.setImageResource(
                showHeaders ? R.drawable.ic_view_list : R.drawable.ic_view_grouped);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
