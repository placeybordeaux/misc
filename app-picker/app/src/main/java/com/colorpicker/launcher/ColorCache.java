package com.colorpicker.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists each app's extracted color data (color, HSB, multicolor flag) keyed by package name, so
 * the expensive Palette + multicolor analysis only runs once per app instead of on every load. The
 * whole cache is invalidated when the color-source strategy changes; individual entries are dropped
 * by {@link #removePackage} when an app is installed/updated/removed.
 */
public final class ColorCache {

    private static final String PREFS = "color_cache";
    static final String KEY_SOURCE = "__source";

    private ColorCache() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Encoded as "color|hue|sat|bri|multi". */
    static String encode(int color, float hue, float sat, float bri, boolean multi) {
        return color + "|" + hue + "|" + sat + "|" + bri + "|" + (multi ? 1 : 0);
    }

    public static void removePackage(Context c, String pkg) {
        prefs(c).edit().remove(pkg).apply();
    }

    public static void clear(Context c) {
        prefs(c).edit().clear().apply();
    }
}
