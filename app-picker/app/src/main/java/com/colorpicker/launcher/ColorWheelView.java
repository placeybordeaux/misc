package com.colorpicker.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A screen-filling mosaic of color <i>regions</i> — one tile per color group, in rainbow order,
 * sized by how many apps it holds. Each tile shows a small color name in the corner and fills the
 * rest of itself with the actual app icons in that color. Tapping a region notifies the host, which
 * opens those apps as a normal scrollable list.
 */
public class ColorWheelView extends View {

    public interface OnRegionClickListener {
        void onRegionClick(int group, String name, int color, List<AppInfo> apps);
    }

    /** Region order: rainbow (red..pink), then the multi-color group, then neutrals. */
    private static final int[] ORDER = {1, 2, 3, 4, 5, 6, 7, 8, 12, 9, 11, 10, 0};

    private static class Tile {
        final RectF rect;
        final int color;
        final int group;
        final String name;
        final List<AppInfo> apps;
        Tile(RectF rect, int color, int group, String name, List<AppInfo> apps) {
            this.rect = rect; this.color = color; this.group = group; this.name = name; this.apps = apps;
        }
    }

    private final List<List<AppInfo>> groupApps = new ArrayList<>();
    private final List<Integer> groupIds = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @Nullable private OnRegionClickListener listener;

    public ColorWheelView(Context context) { super(context); init(); }
    public ColorWheelView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    public void setOnRegionClickListener(OnRegionClickListener l) { this.listener = l; }

    public void setApps(List<AppInfo> all) {
        groupApps.clear();
        groupIds.clear();
        tiles.clear();

        List<List<AppInfo>> buckets = new ArrayList<>();
        for (int i = 0; i < ColorUtils.COLOR_GROUP_COUNT; i++) buckets.add(new ArrayList<>());
        for (AppInfo a : all) buckets.get(ColorUtils.colorGroupIndex(a)).add(a);
        for (int g : ORDER) {
            if (buckets.get(g).isEmpty()) continue;
            List<AppInfo> list = buckets.get(g);
            Collections.sort(list, (a, b) -> Long.compare(b.getUsageScore(), a.getUsageScore()));
            groupApps.add(list);
            groupIds.add(g);
        }
        invalidate();
    }

    public boolean popFocus() { return false; }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || groupApps.isEmpty()) return;
        canvas.drawColor(0xFF0E0E0E);
        if (tiles.isEmpty()) layout(w, h);

        for (Tile t : tiles) {
            boolean rainbow = ColorUtils.isRainbowGroup(t.group);
            if (rainbow) {
                int[] hues = new int[7];
                for (int i = 0; i < hues.length; i++) hues[i] = Color.HSVToColor(new float[]{i * 50f, 0.8f, 0.95f});
                fill.setShader(new LinearGradient(t.rect.left, t.rect.top, t.rect.right, t.rect.bottom,
                        hues, null, Shader.TileMode.CLAMP));
            }
            fill.setColor(t.color);
            canvas.drawRoundRect(t.rect, dp(8), dp(8), fill);
            fill.setShader(null);

            int textColor = rainbow ? 0xFFFFFFFF : textColorFor(t.color);
            float pad = dp(10);
            // Small, corner-aligned color name.
            textPaint.setColor(textColor);
            textPaint.setTextSize(dp(13));
            canvas.drawText(t.name, t.rect.left + pad, t.rect.top + pad + dp(11), textPaint);

            drawRegionIcons(canvas, t, t.rect.top + pad + dp(20));
        }
    }

    /** Fills the tile below the name with as many app icons as fit. */
    private void drawRegionIcons(Canvas canvas, Tile t, float top) {
        float pad = dp(10), gap = dp(5);
        float left = t.rect.left + pad, right = t.rect.right - pad, bottom = t.rect.bottom - pad;
        float w = right - left, h = bottom - top;
        if (w <= 0 || h <= 0) return;
        int n = t.apps.size();

        float[] candidates = {dp(46), dp(40), dp(34), dp(30), dp(26), dp(22), dp(18)};
        float icon = candidates[candidates.length - 1];
        int cols = 1, rows = 1;
        for (float s : candidates) {
            int c = Math.max(1, (int) ((w + gap) / (s + gap)));
            int r = Math.max(1, (int) ((h + gap) / (s + gap)));
            if (c * r >= n) { icon = s; cols = c; rows = r; break; }
            icon = s; cols = c; rows = r; // keep smallest if nothing fits all
        }
        int show = Math.min(n, cols * rows);
        for (int i = 0; i < show; i++) {
            int r = i / cols, c = i % cols;
            float ix = left + c * (icon + gap) + icon / 2;
            float iy = top + r * (icon + gap) + icon / 2;
            Drawable d = t.apps.get(i).getIcon();
            d.setBounds((int) (ix - icon / 2), (int) (iy - icon / 2),
                    (int) (ix + icon / 2), (int) (iy + icon / 2));
            d.draw(canvas);
        }
    }

    private void layout(int w, int h) {
        tiles.clear();
        int n = groupApps.size();
        if (n == 0) return;
        float[] aspects = new float[n];
        int maxCount = 1;
        for (List<AppInfo> g : groupApps) maxCount = Math.max(maxCount, g.size());
        for (int i = 0; i < n; i++) {
            aspects[i] = 0.85f + 0.9f * (float) Math.sqrt(groupApps.get(i).size() / (float) maxCount);
        }
        List<RectF> rects = justifyFill(aspects, w, h);
        for (int i = 0; i < rects.size(); i++) {
            int g = groupIds.get(i);
            tiles.add(new Tile(rects.get(i), ColorUtils.colorGroupAccent(g), g,
                    ColorUtils.colorGroupName(g), groupApps.get(i)));
        }
    }

    /** Justified-rows packing scaled so the tiles fill the whole rect. */
    private List<RectF> justifyFill(float[] aspects, int w, int h) {
        List<RectF> out = new ArrayList<>();
        int n = aspects.length;
        if (n == 0) return out;
        float pad = dp(6), gap = dp(4);
        float availW = w - 2 * pad, availH = h - 2 * pad;
        float sumAr = 0;
        for (float a : aspects) sumAr += a;
        float targetH = (float) Math.sqrt(availW * availH / Math.max(1f, sumAr));
        targetH = Math.max(dp(80), Math.min(availH, targetH));

        float y = pad;
        int i = 0;
        while (i < n) {
            float rowSum = 0;
            int rowEnd = i;
            for (int j = i; j < n; j++) {
                rowSum += aspects[j];
                rowEnd = j;
                if ((availW - gap * (j - i)) / rowSum <= targetH) break;
            }
            int count = rowEnd - i + 1;
            float rowH = (availW - gap * (count - 1)) / rowSum;
            rowH = Math.min(rowH, targetH * 1.6f);
            float x = pad;
            for (int k = 0; k < count; k++) {
                float tw = aspects[i + k] * rowH;
                out.add(new RectF(x, y, x + tw, y + rowH));
                x += tw + gap;
            }
            y += rowH + gap;
            i = rowEnd + 1;
        }
        float usedH = (y - gap) - pad;
        if (usedH > pad && usedH < availH) {
            float scale = availH / usedH;
            for (RectF r : out) { r.top = pad + (r.top - pad) * scale; r.bottom = pad + (r.bottom - pad) * scale; }
        }
        return out;
    }

    private int textColorFor(int bg) {
        double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0;
        return lum > 0.6 ? 0xFF111111 : 0xFFFFFFFF;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        tiles.clear();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = event.getX(), y = event.getY();
        for (Tile t : tiles) {
            if (t.rect.contains(x, y)) {
                if (listener != null) listener.onRegionClick(t.group, t.name, t.color, t.apps);
                return true;
            }
        }
        return true;
    }
}
