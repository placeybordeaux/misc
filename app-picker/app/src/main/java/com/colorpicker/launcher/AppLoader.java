package com.colorpicker.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Loads all launchable apps, extracts their dominant icon color,
 * and returns them sorted in rainbow/hue order.
 */
public class AppLoader {

    public interface Callback {
        void onAppsLoaded(List<AppInfo> apps);
    }

    public static void loadAsync(Context context, Callback callback) {
        new AsyncTask<Void, Void, List<AppInfo>>() {
            @Override
            protected List<AppInfo> doInBackground(Void... voids) {
                return loadApps(context);
            }

            @Override
            protected void onPostExecute(List<AppInfo> apps) {
                callback.onAppsLoaded(apps);
            }
        }.execute();
    }

    private static List<AppInfo> loadApps(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        List<AppInfo> apps = new ArrayList<>();

        // Per-app foreground usage (empty without Usage Access; used by the Usage mode).
        Map<String, Long> usage = UsageStatsHelper.queryUsageScores(context);

        ColorUtils.ColorSource colorSource = Settings.getColorSource(context);

        // Color analysis is the expensive part, so cache it per package. The cache is wiped when the
        // color-source strategy changes; per-package entries are dropped on install/update/removal.
        android.content.SharedPreferences cache = ColorCache.prefs(context);
        boolean sourceChanged = cache.getInt(ColorCache.KEY_SOURCE, -1) != colorSource.ordinal();
        android.content.SharedPreferences.Editor editor = cache.edit();
        if (sourceChanged) {
            editor.clear();
            editor.putInt(ColorCache.KEY_SOURCE, colorSource.ordinal());
        }

        for (ResolveInfo ri : resolveInfos) {
            String label = ri.loadLabel(pm).toString();
            String packageName = ri.activityInfo.packageName;
            Drawable icon = ri.loadIcon(pm);

            int dominantColor;
            float[] hsb;
            boolean multicolor;

            String cached = sourceChanged ? null : cache.getString(packageName, null);
            String[] parts = cached == null ? null : cached.split("\\|");
            if (parts != null && parts.length == 5) {
                dominantColor = Integer.parseInt(parts[0]);
                hsb = new float[]{Float.parseFloat(parts[1]), Float.parseFloat(parts[2]),
                        Float.parseFloat(parts[3])};
                multicolor = "1".equals(parts[4]);
            } else {
                dominantColor = ColorUtils.extractColor(icon, colorSource);
                hsb = ColorUtils.toHSB(dominantColor);
                multicolor = ColorUtils.isMulticolor(icon);
                editor.putString(packageName,
                        ColorCache.encode(dominantColor, hsb[0], hsb[1], hsb[2], multicolor));
            }

            AppInfo info = new AppInfo(label, packageName, icon,
                    hsb[0], hsb[1], hsb[2], dominantColor);
            info.setMulticolor(multicolor);
            Long score = usage.get(packageName);
            if (score != null) info.setUsageScore(score);
            apps.add(info);
        }
        editor.apply();

        // Sort by color group first, then by hue within group, then by label
        Collections.sort(apps, (a, b) -> {
            int groupA = ColorUtils.colorGroupIndex(a);
            int groupB = ColorUtils.colorGroupIndex(b);
            if (groupA != groupB) return Integer.compare(groupA, groupB);

            int hueCmp = Float.compare(a.getHue(), b.getHue());
            if (hueCmp != 0) return hueCmp;

            return a.getLabel().compareToIgnoreCase(b.getLabel());
        });

        return apps;
    }

    /**
     * A copy of {@code apps} ordered by raw hue (continuous rainbow), achromatic colors last.
     * Used by the color-wheel mode.
     */
    public static List<AppInfo> sortedByHue(List<AppInfo> apps) {
        List<AppInfo> out = new ArrayList<>(apps);
        Collections.sort(out, (a, b) -> {
            boolean grayA = a.getSaturation() < 0.15f;
            boolean grayB = b.getSaturation() < 0.15f;
            if (grayA != grayB) return grayA ? 1 : -1; // chromatic first
            return Float.compare(a.getHue(), b.getHue());
        });
        return out;
    }

    /**
     * A copy of {@code apps} ordered by usage, most-used first. Apps with no usage data fall to the
     * end ordered alphabetically, so the list is stable even without Usage Access.
     */
    public static List<AppInfo> sortedByUsage(List<AppInfo> apps) {
        List<AppInfo> out = new ArrayList<>(apps);
        Collections.sort(out, (a, b) -> {
            int cmp = Long.compare(b.getUsageScore(), a.getUsageScore());
            if (cmp != 0) return cmp;
            return a.getLabel().compareToIgnoreCase(b.getLabel());
        });
        return out;
    }
}
