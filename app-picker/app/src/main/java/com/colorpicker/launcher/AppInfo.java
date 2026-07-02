package com.colorpicker.launcher;

import android.graphics.drawable.Drawable;

/**
 * Represents an installed app with its dominant icon color for sorting.
 */
public class AppInfo {
    private final String label;
    private final String packageName;
    private final Drawable icon;
    private final float hue;
    private final float saturation;
    private final float brightness;
    private final int dominantColor;

    /** Foreground-usage score (e.g. total ms in foreground over the lookback window). 0 if unknown. */
    private long usageScore;

    /** True if the icon is a multi-color "rainbow" logo (Google, Slack, …). */
    private boolean multicolor;

    public AppInfo(String label, String packageName, Drawable icon,
                   float hue, float saturation, float brightness, int dominantColor) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
        this.dominantColor = dominantColor;
    }

    public String getLabel() { return label; }
    public String getPackageName() { return packageName; }
    public Drawable getIcon() { return icon; }

    /**
     * A fresh, independent copy of the icon for handing to an {@link android.widget.ImageView}.
     *
     * <p>The base {@link #icon} instance is shared with the canvas-drawing views (the color wheel
     * and the usage field), which mutate its bounds on every frame via {@code setBounds}. Handing
     * that same instance to an ImageView lets those mutations shrink/blank the displayed icon (e.g.
     * when the wheel redraws after the launcher resumes). A copy shares the underlying bitmap through
     * the {@link Drawable.ConstantState} but keeps its own bounds, so it renders independently.
     */
    public Drawable newIconDrawable() {
        Drawable.ConstantState state = icon == null ? null : icon.getConstantState();
        return state != null ? state.newDrawable() : icon;
    }
    public float getHue() { return hue; }
    public float getSaturation() { return saturation; }
    public float getBrightness() { return brightness; }
    public int getDominantColor() { return dominantColor; }
    public long getUsageScore() { return usageScore; }
    public void setUsageScore(long usageScore) { this.usageScore = usageScore; }
    public boolean isMulticolor() { return multicolor; }
    public void setMulticolor(boolean multicolor) { this.multicolor = multicolor; }
}
