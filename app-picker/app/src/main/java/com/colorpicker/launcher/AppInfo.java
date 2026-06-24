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
    public float getHue() { return hue; }
    public float getSaturation() { return saturation; }
    public float getBrightness() { return brightness; }
    public int getDominantColor() { return dominantColor; }
    public long getUsageScore() { return usageScore; }
    public void setUsageScore(long usageScore) { this.usageScore = usageScore; }
}
