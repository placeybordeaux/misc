package com.colorpicker.launcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal strip of apps sorted by usage (most-used first). Each card is tinted by its rainbow
 * position in the list, so the row sweeps red→violet left-to-right while encoding usage rank.
 */
public class UsageStripAdapter extends RecyclerView.Adapter<UsageStripAdapter.VH> {

    private final Context context;
    private final List<AppInfo> apps = new ArrayList<>();
    private boolean hasUsageData = false;

    public UsageStripAdapter(Context context) {
        this.context = context;
    }

    public void setApps(List<AppInfo> sortedByUsage, boolean hasUsageData) {
        apps.clear();
        apps.addAll(sortedByUsage);
        this.hasUsageData = hasUsageData;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return apps.size(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usage, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AppInfo app = apps.get(position);
        h.icon.setImageDrawable(app.getIcon());
        h.label.setText(app.getLabel());
        h.usage.setText(hasUsageData ? formatUsage(app.getUsageScore()) : "");

        // Rainbow by rank: hue spans 0..300 across the list, left = red (most used).
        int count = Math.max(1, apps.size() - 1);
        float hue = 300f * position / count;
        int tint = Color.HSVToColor(new float[]{hue, 0.85f, 0.95f});

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(28f);
        bg.setColor(withAlpha(tint, 235));
        h.itemView.setBackground(bg);

        h.itemView.setOnClickListener(v -> {
            Intent launch = context.getPackageManager()
                    .getLaunchIntentForPackage(app.getPackageName());
            if (launch != null) context.startActivity(launch);
        });
    }

    private static String formatUsage(long ms) {
        if (ms <= 0) return "—";
        long minutes = ms / 60000;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        return (hours / 24) + "d";
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView usage;

        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.app_icon);
            label = v.findViewById(R.id.app_label);
            usage = v.findViewById(R.id.app_usage);
        }
    }
}
