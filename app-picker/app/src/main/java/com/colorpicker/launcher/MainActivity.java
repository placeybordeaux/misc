package com.colorpicker.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private EditText searchBox;

    private final List<AppInfo> allApps = new ArrayList<>();
    private final List<AppInfo> apps = new ArrayList<>(); // filtered for display
    private String query = "";
    private boolean hasUsageAccess = false;

    private Mode mode = Mode.GROUPED;
    private boolean showHeaders = true;
    @Nullable private View colorListOverlay; // the per-color scrollable list, when open

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
        searchBox = findViewById(R.id.search_box);

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

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                applyFilter();
                renderMode();
            }
        });

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
    }

    @Override
    public void onBackPressed() {
        // Close an open per-color list first.
        if (colorListOverlay != null) { closeColorList(); return; }
        if (!query.isEmpty()) { searchBox.setText(""); return; }
        super.onBackPressed();
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void showColorSourceDialog() {
        ColorUtils.ColorSource[] sources = ColorUtils.ColorSource.values();
        int current = Settings.getColorSource(this).ordinal();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Color source")
                .setSingleChoiceItems(Settings.colorSourceLabels(), current, (dialog, which) -> {
                    Settings.setColorSource(this, sources[which]);
                    dialog.dismiss();
                    loadApps();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadApps() {
        progress.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        hasUsageAccess = UsageStatsHelper.hasAccess(this);

        AppLoader.loadAsync(this, loaded -> {
            allApps.clear();
            allApps.addAll(loaded);
            applyFilter();
            progress.setVisibility(View.GONE);
            contentContainer.setVisibility(View.VISIBLE);
            renderMode();
        });
    }

    /** Recomputes the displayed app set from the search query. */
    private void applyFilter() {
        apps.clear();
        if (query.isEmpty()) {
            apps.addAll(allApps);
        } else {
            String q = query.toLowerCase(Locale.getDefault());
            for (AppInfo a : allApps) {
                if (a.getLabel().toLowerCase(Locale.getDefault()).contains(q)) apps.add(a);
            }
        }
    }

    /** Rebuilds the content view for the current mode (or the search results when searching). */
    private void renderMode() {
        colorListOverlay = null;
        contentContainer.removeAllViews();

        boolean searching = !query.isEmpty();
        boolean listMode = mode == Mode.GROUPED || mode == Mode.RAINBOW;
        toggleView.setVisibility(listMode && !searching ? View.VISIBLE : View.GONE);
        updateToggleIcon();
        usageBanner.setVisibility(
                mode == Mode.USAGE && !hasUsageAccess && !searching ? View.VISIBLE : View.GONE);

        if (searching) {
            title.setText(apps.size() + (apps.size() == 1 ? " result" : " results"));
            contentContainer.addView(buildAppGrid(apps, false));
            return;
        }

        switch (mode) {
            case GROUPED:  title.setText("Apps");    renderGrid(false); break;
            case RAINBOW:  title.setText("Rainbow"); renderGrid(true);  break;
            case WHEEL:    title.setText("Wheel");   renderWheel();      break;
            case USAGE:    title.setText("Usage");   renderUsage();      break;
        }
    }

    /** A plain scrollable 4-column grid of apps (flat, no headers). */
    private RecyclerView buildAppGrid(List<AppInfo> list, boolean rainbowBehind) {
        RecyclerView grid = new RecyclerView(this);
        grid.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        grid.setClipToPadding(false);
        grid.setPadding(dp(8), dp(8), dp(8), dp(8));
        AppAdapter adapter = new AppAdapter(this);
        adapter.setShowHeaders(false);
        adapter.setRainbowBehind(rainbowBehind);
        grid.setLayoutManager(new GridLayoutManager(this, GRID_COLUMNS));
        grid.setAdapter(adapter);
        adapter.setApps(list);
        return grid;
    }

    private void renderGrid(boolean rainbowBehind) {
        AppAdapter adapter = new AppAdapter(this);
        adapter.setShowHeaders(showHeaders);
        adapter.setRainbowBehind(rainbowBehind);

        RecyclerView grid = new RecyclerView(this);
        grid.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        grid.setClipToPadding(false);
        grid.setPadding(dp(8), dp(8), dp(8), dp(8));

        GridLayoutManager lm = new GridLayoutManager(this, GRID_COLUMNS);
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? GRID_COLUMNS : 1;
            }
        });

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
                @Override public void onScrolled(RecyclerView rv, int dx, int dy) { update.run(); }
            });
            grid.post(update);
        }
    }

    private int[] visibleSectionColors(GridLayoutManager lm, AppAdapter adapter) {
        int first = lm.findFirstVisibleItemPosition();
        int last = lm.findLastVisibleItemPosition();
        if (first < 0 || last < first) return new int[]{0xFF202020};
        List<Integer> cols = new ArrayList<>();
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
        ColorWheelView wheel = new ColorWheelView(this);
        wheel.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        wheel.setOnRegionClickListener((group, name, color, regionApps) ->
                showColorList(name, color, regionApps));
        wheel.setApps(apps);
        contentContainer.addView(wheel);
    }

    /** Opens a color region's apps as a normal scrollable list over the wheel. */
    private void showColorList(String name, int color, List<AppInfo> list) {
        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setBackgroundColor(0xFF121212);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView header = new TextView(this);
        header.setText("‹  " + name);
        header.setTextColor(0xFFFFFFFF);
        header.setTextSize(18f);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOnClickListener(v -> closeColorList());
        overlay.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        RecyclerView grid = buildAppGrid(list, false);
        overlay.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        contentContainer.addView(overlay);
        colorListOverlay = overlay;
    }

    private void closeColorList() {
        if (colorListOverlay != null) {
            contentContainer.removeView(colorListOverlay);
            colorListOverlay = null;
        }
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
