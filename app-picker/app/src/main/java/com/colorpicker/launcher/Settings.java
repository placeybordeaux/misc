package com.colorpicker.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/** Small wrapper over SharedPreferences for user-tweakable options. */
public final class Settings {

    private static final String PREFS = "color_picker_prefs";
    private static final String KEY_COLOR_SOURCE = "color_source";

    private Settings() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static ColorUtils.ColorSource getColorSource(Context c) {
        int fallback = ColorUtils.ColorSource.AVERAGE.ordinal();
        int i = prefs(c).getInt(KEY_COLOR_SOURCE, fallback);
        ColorUtils.ColorSource[] values = ColorUtils.ColorSource.values();
        return values[Math.max(0, Math.min(i, values.length - 1))];
    }

    public static void setColorSource(Context c, ColorUtils.ColorSource source) {
        prefs(c).edit().putInt(KEY_COLOR_SOURCE, source.ordinal()).apply();
    }

    /** Human-readable labels for the color-source picker. */
    public static String[] colorSourceLabels() {
        return new String[]{
                "Most common  ·  keeps white backgrounds",
                "Vibrant  ·  logo accent color",
                "Average  ·  blended mean color"
        };
    }
}
