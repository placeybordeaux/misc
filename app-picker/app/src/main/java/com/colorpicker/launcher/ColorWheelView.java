package com.colorpicker.launcher;

import android.animation.ValueAnimator;
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
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A two-level color explorer that fills the screen.
 *
 * <p><b>Level 1</b> is a mosaic of color <i>regions</i> — one tile per color group, in rainbow
 * order, sized by how many apps it holds. <b>Tap a region</b> to <b>subselect</b>: the view drills
 * into that color and lays its apps out as their own screen-filling mosaic, each tappable to launch.
 * Tapping the background (or Back) returns to the regions.
 */
public class ColorWheelView extends View {

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);
    }

    /** Region order: rainbow (red..pink), then the multi-color group, then neutrals. */
    private static final int[] ORDER = {1, 2, 3, 4, 5, 6, 7, 8, 12, 9, 11, 10, 0};

    private static class Tile {
        final RectF rect;
        final int color;
        @Nullable final AppInfo app;     // set for app tiles
        final int group;                 // set for region tiles
        final String label;
        final int count;
        @Nullable final List<AppInfo> samples; // representative icons, for region tiles
        Tile(RectF rect, int color, @Nullable AppInfo app, int group, String label, int count,
             @Nullable List<AppInfo> samples) {
            this.rect = rect; this.color = color; this.app = app; this.group = group;
            this.label = label; this.count = count; this.samples = samples;
        }
    }

    private final List<AppInfo> apps = new ArrayList<>();
    private final List<List<AppInfo>> groupApps = new ArrayList<>();
    private final List<Integer> groupIds = new ArrayList<>();

    private final List<Tile> regionTiles = new ArrayList<>();
    private final List<Tile> appTiles = new ArrayList<>();

    private int selectedGroup = -1;       // index into groupApps, -1 == regions
    private float progress = 0f;          // 0 = regions, 1 = subselection
    @Nullable private ValueAnimator animator;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @Nullable private OnAppClickListener listener;

    public ColorWheelView(Context context) { super(context); init(); }
    public ColorWheelView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setOnAppClickListener(OnAppClickListener l) { this.listener = l; }

    public void setApps(List<AppInfo> all) {
        apps.clear();
        apps.addAll(all);
        regionTiles.clear();
        appTiles.clear();
        selectedGroup = -1;
        progress = 0f;
        groupApps.clear();
        groupIds.clear();

        List<List<AppInfo>> buckets = new ArrayList<>();
        for (int i = 0; i < ColorUtils.COLOR_GROUP_COUNT; i++) buckets.add(new ArrayList<>());
        for (AppInfo a : all) {
            buckets.get(ColorUtils.colorGroupIndex(a)).add(a);
        }
        for (int g : ORDER) {
            if (buckets.get(g).isEmpty()) continue;
            List<AppInfo> list = buckets.get(g);
            Collections.sort(list, (a, b) -> Float.compare(a.getHue(), b.getHue()));
            groupApps.add(list);
            groupIds.add(g);
        }
        invalidate();
    }

    /** Back: if subselected, return to regions. */
    public boolean popFocus() {
        if (progress > 0.01f || selectedGroup != -1) { animateTo(0f); return true; }
        return false;
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || apps.isEmpty()) return;
        canvas.drawColor(0xFF0E0E0E);
        if (regionTiles.isEmpty()) layoutRegions(w, h);

        if (progress < 0.999f) drawTiles(canvas, regionTiles, (int) (255 * (1f - progress)), true);
        if (selectedGroup != -1 && progress > 0.001f) drawTiles(canvas, appTiles, (int) (255 * progress), false);
    }

    private void drawTiles(Canvas canvas, List<Tile> tiles, int alpha, boolean isRegion) {
        for (Tile t : tiles) {
            boolean rainbow = isRegion && ColorUtils.isRainbowGroup(t.group);
            if (rainbow) {
                int[] hues = new int[7];
                for (int i = 0; i < hues.length; i++) hues[i] = Color.HSVToColor(new float[]{i * 50f, 0.8f, 0.95f});
                fill.setShader(new LinearGradient(t.rect.left, t.rect.top, t.rect.right, t.rect.bottom,
                        hues, null, Shader.TileMode.CLAMP));
            }
            fill.setColor(t.color);
            fill.setAlpha(alpha);
            canvas.drawRoundRect(t.rect, dp(8), dp(8), fill);
            fill.setShader(null);
            fill.setAlpha(255);

            float cw = t.rect.width(), ch = t.rect.height();
            if (isRegion) {
                int textColor = rainbow ? 0xFFFFFFFF : textColorFor(t.color);
                // Name at the top, a few representative icons in the middle, count at the bottom.
                textPaint.setColor(textColor);
                textPaint.setAlpha(alpha);
                textPaint.setTextSize(Math.min(dp(20), ch * 0.16f));
                canvas.drawText(t.label, t.rect.centerX(), t.rect.top + ch * 0.24f, textPaint);

                if (t.samples != null && !t.samples.isEmpty()) {
                    int ns = Math.min(t.samples.size(), cw > dp(150) ? 4 : 3);
                    float isz = Math.min(dp(40), (cw - dp(16)) / ns - dp(6));
                    isz = Math.max(isz, dp(20));
                    float spacing = isz + dp(6);
                    float startX = t.rect.centerX() - (ns - 1) * spacing / 2f;
                    float iy = t.rect.centerY() + dp(4);
                    for (int s = 0; s < ns; s++) {
                        Drawable d = t.samples.get(s).getIcon();
                        float ix = startX + s * spacing;
                        d.setBounds((int) (ix - isz / 2), (int) (iy - isz / 2),
                                (int) (ix + isz / 2), (int) (iy + isz / 2));
                        d.setAlpha(alpha);
                        d.draw(canvas);
                        d.setAlpha(255);
                    }
                }

                textPaint.setColor(textColor);
                textPaint.setAlpha(alpha);
                textPaint.setTextSize(Math.min(dp(13), ch * 0.1f));
                canvas.drawText(t.count + (t.count == 1 ? " app" : " apps"),
                        t.rect.centerX(), t.rect.bottom - ch * 0.1f, textPaint);
                textPaint.setAlpha(255);
            } else if (t.app != null) {
                boolean showLabel = ch > dp(72) && cw > dp(62);
                float iconSize = Math.min(Math.min(cw, ch) * (showLabel ? 0.5f : 0.62f), dp(72));
                float ccx = t.rect.centerX(), ccy = t.rect.centerY() - (showLabel ? dp(8) : 0);
                Drawable d = t.app.getIcon();
                d.setBounds((int) (ccx - iconSize / 2), (int) (ccy - iconSize / 2),
                        (int) (ccx + iconSize / 2), (int) (ccy + iconSize / 2));
                d.setAlpha(alpha);
                d.draw(canvas);
                d.setAlpha(255);
                if (showLabel) {
                    textPaint.setColor(textColorFor(t.color));
                    textPaint.setAlpha(alpha);
                    textPaint.setTextSize(dp(11));
                    canvas.drawText(ellipsize(t.app.getLabel(), textPaint, cw - dp(8)),
                            ccx, ccy + iconSize / 2 + dp(15), textPaint);
                    textPaint.setAlpha(255);
                }
            }
        }
    }

    // --- layout ---

    private void layoutRegions(int w, int h) {
        regionTiles.clear();
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
            List<AppInfo> group = new ArrayList<>(groupApps.get(i));
            Collections.sort(group, (a, b) -> Long.compare(b.getUsageScore(), a.getUsageScore()));
            List<AppInfo> samples = new ArrayList<>(group.subList(0, Math.min(4, group.size())));
            regionTiles.add(new Tile(rects.get(i), ColorUtils.colorGroupAccent(g), null, g,
                    ColorUtils.colorGroupName(g), groupApps.get(i).size(), samples));
        }
    }

    private void layoutApps(int w, int h) {
        appTiles.clear();
        if (selectedGroup < 0 || selectedGroup >= groupApps.size()) return;
        List<AppInfo> list = groupApps.get(selectedGroup);
        int n = list.size();
        long usageMax = 0;
        for (AppInfo a : list) usageMax = Math.max(usageMax, a.getUsageScore());
        float[] aspects = new float[n];
        for (int i = 0; i < n; i++) {
            float pop = usageMax > 0 ? (float) (list.get(i).getUsageScore() / (double) usageMax) : 0.4f;
            aspects[i] = 0.85f + 0.9f * (float) Math.sqrt(pop);
        }
        List<RectF> rects = justifyFill(aspects, w, h);
        for (int i = 0; i < rects.size(); i++) {
            AppInfo a = list.get(i);
            appTiles.add(new Tile(rects.get(i), tileColor(a), a, -1, a.getLabel(), 0, null));
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
        targetH = Math.max(dp(60), Math.min(availH, targetH));

        float y = pad;
        int i = 0;
        while (i < n) {
            float rowSum = 0;
            int rowEnd = i;
            for (int j = i; j < n; j++) {
                rowSum += aspects[j];
                rowEnd = j;
                float projectedH = (availW - gap * (j - i)) / rowSum;
                if (projectedH <= targetH) break;
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

        // Scale row heights so the mosaic fills to the bottom (no dead space).
        float usedH = (y - gap) - pad;
        if (usedH > pad && usedH < availH) {
            float scale = availH / usedH;
            for (RectF r : out) {
                r.top = pad + (r.top - pad) * scale;
                r.bottom = pad + (r.bottom - pad) * scale;
            }
        }
        return out;
    }

    private int tileColor(AppInfo app) {
        float[] hsv = new float[3];
        Color.colorToHSV(app.getDominantColor(), hsv);
        if (hsv[1] >= 0.12f) {
            hsv[1] = Math.min(1f, hsv[1] * 1.15f + 0.05f);
            hsv[2] = Math.max(0.35f, hsv[2] * 0.92f);
        }
        return Color.HSVToColor(hsv);
    }

    private int textColorFor(int bg) {
        double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0;
        return lum > 0.6 ? 0xFF111111 : 0xFFFFFFFF;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        regionTiles.clear();
        appTiles.clear();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float x = event.getX(), y = event.getY();

        if (progress > 0.5f) {
            for (Tile t : appTiles) {
                if (t.rect.contains(x, y) && t.app != null) {
                    if (listener != null) listener.onAppClick(t.app);
                    return true;
                }
            }
            animateTo(0f); // tap empty -> back to regions
            return true;
        }

        for (int i = 0; i < regionTiles.size(); i++) {
            if (regionTiles.get(i).rect.contains(x, y)) {
                selectedGroup = i;
                layoutApps(getWidth(), getHeight());
                animateTo(1f);
                return true;
            }
        }
        return true;
    }

    private void animateTo(float target) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(220);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> { progress = (float) a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    private String ellipsize(String s, Paint p, float maxWidth) {
        if (p.measureText(s) <= maxWidth) return s;
        String ell = "…";
        while (s.length() > 1 && p.measureText(s + ell) > maxWidth) s = s.substring(0, s.length() - 1);
        return s + ell;
    }
}
