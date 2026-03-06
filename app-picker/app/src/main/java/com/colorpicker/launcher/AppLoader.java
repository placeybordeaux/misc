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

        for (ResolveInfo ri : resolveInfos) {
            String label = ri.loadLabel(pm).toString();
            String packageName = ri.activityInfo.packageName;
            Drawable icon = ri.loadIcon(pm);

            int dominantColor = ColorUtils.extractDominantColor(icon);
            float[] hsb = ColorUtils.toHSB(dominantColor);

            apps.add(new AppInfo(label, packageName, icon,
                    hsb[0], hsb[1], hsb[2], dominantColor));
        }

        // Sort by color group first, then by hue within group, then by label
        Collections.sort(apps, (a, b) -> {
            int groupA = ColorUtils.colorGroupIndex(a.getHue(), a.getSaturation(), a.getBrightness());
            int groupB = ColorUtils.colorGroupIndex(b.getHue(), b.getSaturation(), b.getBrightness());
            if (groupA != groupB) return Integer.compare(groupA, groupB);

            int hueCmp = Float.compare(a.getHue(), b.getHue());
            if (hueCmp != 0) return hueCmp;

            return a.getLabel().compareToIgnoreCase(b.getLabel());
        });

        return apps;
    }
}
