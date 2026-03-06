package com.colorpicker.launcher;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Main launcher activity that displays installed apps sorted by their
 * icon's dominant color in rainbow order, grouped by color sections.
 */
public class MainActivity extends AppCompatActivity {

    private static final int GRID_COLUMNS = 4;

    private RecyclerView appGrid;
    private ProgressBar progress;
    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appGrid = findViewById(R.id.app_grid);
        progress = findViewById(R.id.progress);

        adapter = new AppAdapter(this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, GRID_COLUMNS);
        // Make header items span the full width
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? GRID_COLUMNS : 1;
            }
        });

        appGrid.setLayoutManager(layoutManager);
        appGrid.setAdapter(adapter);

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload when returning to launcher in case apps were installed/removed
        loadApps();
    }

    private void loadApps() {
        progress.setVisibility(View.VISIBLE);
        appGrid.setVisibility(View.GONE);

        AppLoader.loadAsync(this, apps -> {
            adapter.setApps(apps);
            progress.setVisibility(View.GONE);
            appGrid.setVisibility(View.VISIBLE);
        });
    }
}
