package com.colorpicker.launcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
 * RecyclerView adapter that displays apps in a grid with color group section headers.
 * Items are either HEADER (color group label) or APP (icon + name).
 */
public class AppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_APP = 1;

    private final List<Object> items = new ArrayList<>();
    private final Context context;

    public AppAdapter(Context context) {
        this.context = context;
    }

    private List<AppInfo> currentApps = new ArrayList<>();
    private boolean showHeaders = true;
    private boolean rainbowBehind = false;

    public void setApps(List<AppInfo> apps) {
        currentApps = new ArrayList<>(apps);
        rebuildItems();
    }

    public void setShowHeaders(boolean show) {
        if (showHeaders != show) {
            showHeaders = show;
            rebuildItems();
        }
    }

    public boolean getShowHeaders() {
        return showHeaders;
    }

    /** When true, each icon sits on a vivid hue-tinted glow instead of a subtle tint. */
    public void setRainbowBehind(boolean rainbow) {
        if (rainbowBehind != rainbow) {
            rainbowBehind = rainbow;
            notifyDataSetChanged();
        }
    }

    private void rebuildItems() {
        items.clear();

        if (showHeaders) {
            int currentGroup = -1;
            for (AppInfo app : currentApps) {
                int group = ColorUtils.colorGroupIndex(app);
                if (group != currentGroup) {
                    currentGroup = group;
                    items.add(new HeaderItem(
                            ColorUtils.colorGroupName(group),
                            ColorUtils.colorGroupAccent(group)));
                }
                items.add(app);
            }
        } else {
            items.addAll(currentApps);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof HeaderItem ? TYPE_HEADER : TYPE_APP;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_app, parent, false);
            return new AppViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderItem header = (HeaderItem) items.get(position);
            HeaderViewHolder hvh = (HeaderViewHolder) holder;
            hvh.title.setText(header.name);
            hvh.colorStrip.setBackgroundColor(header.color);
        } else {
            AppInfo app = (AppInfo) items.get(position);
            AppViewHolder avh = (AppViewHolder) holder;
            avh.icon.setImageDrawable(app.getIcon());
            avh.label.setText(app.getLabel());

            if (rainbowBehind) {
                // Over the rainbow background, give each icon a soft dark backing for legibility.
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(28f);
                bg.setColor(0x66000000);
                avh.itemView.setBackground(bg);
            } else {
                // Subtle tinted background based on dominant color
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(24f);
                bg.setColor(withAlpha(app.getDominantColor(), 30));
                avh.itemView.setBackground(bg);
            }

            avh.itemView.setOnClickListener(v -> {
                Intent launch = context.getPackageManager()
                        .getLaunchIntentForPackage(app.getPackageName());
                if (launch != null) {
                    context.startActivity(launch);
                }
            });
        }
    }

    /** Accent color of the item's color group at this position (for the rainbow background). */
    public int groupAccentAt(int position) {
        if (position < 0 || position >= items.size()) return Color.DKGRAY;
        Object o = items.get(position);
        if (o instanceof HeaderItem) return ((HeaderItem) o).color;
        AppInfo a = (AppInfo) o;
        return ColorUtils.colorGroupAccent(ColorUtils.colorGroupIndex(a));
    }

    /**
     * Whether the item at this position should span the full grid width (headers do).
     */
    public boolean isHeader(int position) {
        return position >= 0 && position < items.size()
                && items.get(position) instanceof HeaderItem;
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * A vivid version of the icon's hue for the rainbow-behind glow. Chromatic icons get a
     * full-saturation color of their hue; near-gray icons stay neutral so the rainbow reads cleanly.
     */
    private int vividHue(float hue, float saturation) {
        if (saturation < 0.15f) {
            return Color.HSVToColor(new float[]{0f, 0f, 0.6f}); // neutral gray glow
        }
        return Color.HSVToColor(new float[]{hue, 0.9f, 1f});
    }

    // --- View Holders ---

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;

        AppViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final View colorStrip;

        HeaderViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.header_title);
            colorStrip = itemView.findViewById(R.id.color_strip);
        }
    }

    // --- Header model ---

    static class HeaderItem {
        final String name;
        final int color;

        HeaderItem(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }
}
