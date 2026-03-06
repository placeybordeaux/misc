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

    public static final int COLOR_GROUP_COUNT = 12;

    /**
     * Extracts dominant color swatch from a drawable.
     * Falls back to vibrant, then muted, then gray if nothing found.
     */
    public static int extractDominantColor(Drawable drawable) {
        Bitmap bitmap = drawableToBitmap(drawable, 48, 48);
        Palette palette = Palette.from(bitmap).maximumColorCount(16).generate();
        bitmap.recycle();

        Palette.Swatch swatch = palette.getDominantSwatch();
        if (swatch == null) swatch = palette.getVibrantSwatch();
        if (swatch == null) swatch = palette.getMutedSwatch();
        if (swatch != null) return swatch.getRgb();

        return Color.GRAY;
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
            default: return Color.GRAY;
        }
    }

    private static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }
}
