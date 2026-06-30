package com.colorpicker.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Full-screen rainbow backdrop for the Rainbow grid. It paints the colors of the sections currently
 * on screen as a smooth vertical gradient, so the rainbow itself is the background and it slides
 * through the spectrum — matching the section you're scrolled to — as you move down the list.
 */
public class RainbowBackground extends View {

    private int[] colors = {0xFF202020, 0xFF202020};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RainbowBackground(Context c) { super(c); }
    public RainbowBackground(Context c, AttributeSet a) { super(c, a); }

    /** Colors of the visible color sections, top to bottom. */
    public void setColors(int[] groupAccents) {
        if (groupAccents == null || groupAccents.length == 0) return;
        if (groupAccents.length == 1) {
            int c = darken(groupAccents[0]);
            colors = new int[]{c, c};
        } else {
            colors = new int[groupAccents.length];
            for (int i = 0; i < groupAccents.length; i++) colors[i] = darken(groupAccents[i]);
        }
        invalidate();
    }

    /** Keep it rich but dark enough that icons (on their dark backing) stay readable. */
    private int darken(int c) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 1.0f);
        hsv[2] = hsv[2] * 0.5f;
        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        canvas.drawColor(0xFF0E0E0E);
        paint.setShader(new LinearGradient(0, 0, 0, h, colors, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
    }
}
