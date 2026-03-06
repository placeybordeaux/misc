package com.colorpicker.launcher;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private static final int GRID_COLUMNS = 4;

    private RecyclerView appGrid;
    private ProgressBar progress;
    private ImageView toggleView;
    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appGrid = findViewById(R.id.app_grid);
        progress = findViewById(R.id.progress);
        toggleView = findViewById(R.id.toggle_view);

        adapter = new AppAdapter(this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, GRID_COLUMNS);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? GRID_COLUMNS : 1;
            }
        });

        appGrid.setLayoutManager(layoutManager);
        appGrid.setAdapter(adapter);

        updateToggleIcon();
        toggleView.setOnClickListener(v -> {
            adapter.setShowHeaders(!adapter.getShowHeaders());
            updateToggleIcon();
        });

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    private void updateToggleIcon() {
        if (adapter.getShowHeaders()) {
            // Currently grouped — show list icon to switch to flat
            toggleView.setImageResource(R.drawable.ic_view_list);
        } else {
            // Currently flat — show grid icon to switch to grouped
            toggleView.setImageResource(R.drawable.ic_view_grouped);
        }
    }
}
