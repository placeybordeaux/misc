package com.colorpicker.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.palette.graphics.Palette;

/**
 * Extracts the dominant color from a Drawable icon using AndroidX Palette,
 * and converts it to HSB for rainbow-order sorting.
 */
public class ColorUtils {

    public static final int COLOR_GROUP_COUNT = 13;
    public static final int GROUP_RED = 1;
    public static final int GROUP_ORANGE = 2;
    /** Icons with several distinct vivid hues (Google, Slack, …) — "rainbow", not white. */
    public static final int GROUP_RAINBOW = 12;

    /**
     * Hand-tuned group overrides for icons the automatic classifier reads wrong (multi-color logos
     * the heuristic misses, or whose averaged color lands in the neighbouring group). Keyed by
     * package name; see {@code ColorUtilsTest} for the cases this pins down.
     */
    private static final java.util.Map<String, Integer> GROUP_OVERRIDES = new java.util.HashMap<>();
    static {
        GROUP_OVERRIDES.put("com.google.android.apps.maps", GROUP_RAINBOW); // multi-color pin
        GROUP_OVERRIDES.put("com.google.android.gm", GROUP_RAINBOW);        // Gmail "M"
        GROUP_OVERRIDES.put("com.anthropic.claude", GROUP_ORANGE);          // coral reads as red
        GROUP_OVERRIDES.put("com.playstack.balatro.android", GROUP_RED);    // reads as blue
    }

    /** The forced group for a package, or {@code null} if it should be classified normally. */
    public static Integer groupOverride(String packageName) {
        return GROUP_OVERRIDES.get(packageName);
    }

    /**
     * How an app's single representative colour is chosen from its icon. The strategies differ in
     * how they treat white/light backgrounds, which most app icons have.
     */
    public enum ColorSource {
        /** Most populous colour, white/light backgrounds included (Palette filters cleared). */
        MOST_COMMON,
        /** The vivid accent / logo colour; near-white and near-black are filtered out. */
        VIBRANT,
        /** Mean colour of all opaque pixels — a blend of background and logo. */
        AVERAGE
    }

    /** Backwards-compatible default extraction (vivid accent colour). */
    public static int extractDominantColor(Drawable drawable) {
        return extractColor(drawable, ColorSource.VIBRANT);
    }

    /**
     * True if the icon is a multi-color "rainbow" logo (Google, Slack, Photos…) rather than a
     * single-color, two-tone, or gradient icon.
     *
     * <p>Saturated pixels are binned into 12 hues. We count <i>separated</i> hue clusters rather
     * than populated bins: a gradient (Firefox) or two-tone logo (Alaska) spreads across adjacent
     * bins and forms only one or two clusters, while a true rainbow has 3+ well-separated color
     * clusters around the wheel.
     */
    public static boolean isMulticolor(Drawable drawable) {
        Bitmap bitmap = drawableToBitmap(drawable, 48, 48);
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] px = new int[w * h];
        bitmap.getPixels(px, 0, w, 0, 0, w, h);
        bitmap.recycle();

        int[] bins = new int[12];
        int saturated = 0;
        float[] hsv = new float[3];
        for (int c : px) {
            if (Color.alpha(c) < 16) continue;
            Color.colorToHSV(c, hsv);
            if (hsv[1] > 0.45f && hsv[2] > 0.35f) {
                bins[((int) (hsv[0] / 30f)) % 12]++;
                saturated++;
            }
        }
        if (saturated < px.length * 0.08f) return false; // not enough colored area

        // If one hue dominates the colored area it's really that color (e.g. a red logo with a
        // small multi-color glyph), not a rainbow.
        int maxBin = 0;
        for (int b : bins) maxBin = Math.max(maxBin, b);
        if (maxBin > saturated * 0.5f) return false;

        // Mark bins that hold a meaningful share, then count circular runs of marked bins.
        boolean[] sig = new boolean[12];
        int sigCount = 0;
        for (int i = 0; i < 12; i++) {
            if (bins[i] >= saturated * 0.09f) { sig[i] = true; sigCount++; }
        }
        if (sigCount < 3 || sigCount == 12) return false;

        int clusters = 0;
        for (int i = 0; i < 12; i++) {
            if (sig[i] && !sig[(i + 11) % 12]) clusters++; // start of a run (ring-aware)
        }
        return clusters >= 3;
    }

    /** Color group for an app, accounting for multi-color "rainbow" icons. */
    public static int colorGroupIndex(AppInfo app) {
        Integer override = GROUP_OVERRIDES.get(app.getPackageName());
        if (override != null) return override;
        if (app.isMulticolor()) return GROUP_RAINBOW;
        return colorGroupIndex(app.getHue(), app.getSaturation(), app.getBrightness());
    }

    /** Extracts the representative colour of an icon using the chosen strategy. */
    public static int extractColor(Drawable drawable, ColorSource source) {
        Bitmap bitmap = drawableToBitmap(drawable, 48, 48);
        int color;
        switch (source) {
            case AVERAGE:
                color = averageColor(bitmap);
                break;
            case VIBRANT:
                color = vibrantColor(bitmap);
                break;
            case MOST_COMMON:
            default:
                color = mostCommonColor(bitmap);
                break;
        }
        bitmap.recycle();
        return color;
    }

    /** Vivid accent colour: Palette's default filter drops near-white/near-black backgrounds. */
    private static int vibrantColor(Bitmap bitmap) {
        Palette palette = Palette.from(bitmap).maximumColorCount(16).generate();
        Palette.Swatch swatch = palette.getDominantSwatch();
        if (swatch == null) swatch = palette.getVibrantSwatch();
        if (swatch == null) swatch = palette.getMutedSwatch();
        return swatch != null ? swatch.getRgb() : Color.GRAY;
    }

    /** Most populous colour with filters cleared, so a white background reads as white. */
    private static int mostCommonColor(Bitmap bitmap) {
        Palette palette = Palette.from(bitmap).clearFilters().maximumColorCount(24).generate();
        Palette.Swatch swatch = palette.getDominantSwatch();
        return swatch != null ? swatch.getRgb() : averageColor(bitmap);
    }

    /** Mean of all sufficiently-opaque pixels. */
    private static int averageColor(Bitmap bitmap) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] px = new int[w * h];
        bitmap.getPixels(px, 0, w, 0, 0, w, h);
        long r = 0, g = 0, b = 0, count = 0;
        for (int c : px) {
            if (Color.alpha(c) < 16) continue;
            r += Color.red(c);
            g += Color.green(c);
            b += Color.blue(c);
            count++;
        }
        if (count == 0) return Color.GRAY;
        return Color.rgb((int) (r / count), (int) (g / count), (int) (b / count));
    }

    /**
     * Convert a color to HSB components. Returns float[]{hue, saturation, brightness}.
     */
    public static float[] toHSB(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv; // hsv[0]=hue(0-360), hsv[1]=saturation(0-1), hsv[2]=value/brightness(0-1)
    }

    /**
     * Maps a color to one of the named color groups for section headers.
     */
    public static int colorGroupIndex(float hue, float saturation, float brightness) {
        // Achromatic: very low saturation or very dark/bright
        if (saturation < 0.15f) {
            if (brightness < 0.3f) return 0;  // Black
            if (brightness > 0.85f) return 11; // White
            return 10; // Gray
        }

        // Chromatic groups by hue (rainbow order)
        if (hue < 15)   return 1; // Red
        if (hue < 40)   return 2; // Orange
        if (hue < 70)   return 3; // Yellow
        if (hue < 150)  return 4; // Green
        if (hue < 190)  return 5; // Teal
        if (hue < 250)  return 6; // Blue
        if (hue < 290)  return 7; // Purple
        if (hue < 330)  return 8; // Pink
        return 1; // Red (wraps around)
    }

    public static String colorGroupName(int index) {
        switch (index) {
            case 0:  return "Black";
            case 1:  return "Red";
            case 2:  return "Orange";
            case 3:  return "Yellow";
            case 4:  return "Green";
            case 5:  return "Teal";
            case 6:  return "Blue";
            case 7:  return "Purple";
            case 8:  return "Pink";
            case 9:  return "Brown";
            case 10: return "Gray";
            case 11: return "White";
            case 12: return "Rainbow";
            default: return "Other";
        }
    }

    public static int colorGroupAccent(int index) {
        switch (index) {
            case 0:  return Color.parseColor("#212121");
            case 1:  return Color.parseColor("#F44336");
            case 2:  return Color.parseColor("#FF9800");
            case 3:  return Color.parseColor("#FFEB3B");
            case 4:  return Color.parseColor("#4CAF50");
            case 5:  return Color.parseColor("#009688");
            case 6:  return Color.parseColor("#2196F3");
            case 7:  return Color.parseColor("#9C27B0");
            case 8:  return Color.parseColor("#E91E63");
            case 9:  return Color.parseColor("#795548");
            case 10: return Color.parseColor("#9E9E9E");
            case 11: return Color.parseColor("#E0E0E0");
            case 12: return Color.parseColor("#7E57C2"); // fallback; rainbow groups render a gradient
            default: return Color.GRAY;
        }
    }

    /** Whether this group should be drawn as a rainbow rather than a solid color. */
    public static boolean isRainbowGroup(int group) {
        return group == GROUP_RAINBOW;
    }

    private static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }
}
