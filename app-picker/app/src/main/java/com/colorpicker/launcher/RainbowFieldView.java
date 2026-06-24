package com.colorpicker.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dense, horizontally-scrolling rainbow field for the Usage mode.
 *
 * <p>Each colour becomes a horizontal lane, stacked top-to-bottom in rainbow order (red at the top
 * through to violet, with the neutrals below) so the whole spectrum is visible at once as a hazy,
 * boundary-less gradient. Within a lane, apps are packed tightly and ordered by usage (most-used on
 * the left); you scroll sideways through all lanes together, travelling along the rainbow.
 */
public class RainbowFieldView extends View {

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);
    }

    /** Colour-group lane order: rainbow (red..pink), the multi-color group, then the neutrals. */
    private static final int[] LANE_ORDER = {1, 2, 3, 4, 5, 6, 7, 8, 12, 9, 11, 10, 0};

    private final List<List<AppInfo>> lanes = new ArrayList<>();
    private final List<Integer> laneGroups = new ArrayList<>();
    private int maxLaneSize = 0;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Nullable private Shader bgShader;
    private int bgShaderH = -1;

    private final OverScroller scroller;
    private final GestureDetector gestures;
    private int scrollX = 0;
    private int contentWidth = 0;
    private boolean hasUsage = false;

    @Nullable private OnAppClickListener listener;

    public RainbowFieldView(Context c) { super(c); scroller = new OverScroller(c); gestures = buildGestures(c); init(); }
    public RainbowFieldView(Context c, AttributeSet a) { super(c, a); scroller = new OverScroller(c); gestures = buildGestures(c); init(); }

    private void init() {
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setShadowLayer(dp(3), 0, dp(1), 0xDD000000);
    }

    public void setOnAppClickListener(OnAppClickListener l) { this.listener = l; }

    public void setApps(List<AppInfo> allApps, boolean hasUsage) {
        this.hasUsage = hasUsage;
        lanes.clear();
        laneGroups.clear();
        maxLaneSize = 0;

        // Bucket by colour group.
        List<List<AppInfo>> byGroup = new ArrayList<>();
        for (int i = 0; i < ColorUtils.COLOR_GROUP_COUNT; i++) byGroup.add(new ArrayList<>());
        for (AppInfo a : allApps) {
            byGroup.get(ColorUtils.colorGroupIndex(a)).add(a);
        }
        // Build lanes in rainbow order, each sorted by usage (most-used first).
        for (int g : LANE_ORDER) {
            List<AppInfo> lane = byGroup.get(g);
            if (lane.isEmpty()) continue;
            Collections.sort(lane, (a, b) -> {
                int cmp = Long.compare(b.getUsageScore(), a.getUsageScore());
                return cmp != 0 ? cmp : a.getLabel().compareToIgnoreCase(b.getLabel());
            });
            lanes.add(lane);
            laneGroups.add(g);
            maxLaneSize = Math.max(maxLaneSize, lane.size());
        }
        scrollX = 0;
        invalidate();
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private float padX() { return dp(10); }

    // Sizing derived from how many lanes have to fit the screen height.
    private float laneHeight() { return lanes.isEmpty() ? 0 : (float) getHeight() / lanes.size(); }
    private float iconSize() { return Math.max(dp(30), Math.min(dp(46), laneHeight() * 0.6f)); }
    private float colSpacing() { return iconSize() + dp(14); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || lanes.isEmpty()) return;

        // Opaque clear every frame so nothing accumulates/darkens while scrolling.
        canvas.drawColor(0xFF0E0E0E);

        float lh = laneHeight();
        float icon = iconSize();
        float spacing = colSpacing();
        contentWidth = (int) (padX() * 2 + maxLaneSize * spacing);

        drawRainbow(canvas, w, h);

        boolean showLabels = lh > dp(64);
        labelPaint.setTextSize(dp(9));

        for (int k = 0; k < lanes.size(); k++) {
            List<AppInfo> lane = lanes.get(k);
            float cy = k * lh + lh / 2f - (showLabels ? dp(6) : 0);
            for (int j = 0; j < lane.size(); j++) {
                float x = padX() + j * spacing + spacing / 2f - scrollX;
                if (x < -spacing || x > w + spacing) continue;

                AppInfo app = lane.get(j);
                Drawable d = app.getIcon();
                d.setBounds((int) (x - icon / 2), (int) (cy - icon / 2),
                        (int) (x + icon / 2), (int) (cy + icon / 2));
                d.draw(canvas);

                if (showLabels) {
                    String label = ellipsize(app.getLabel(), labelPaint, spacing - dp(2));
                    canvas.drawText(label, x, cy + icon / 2 + dp(11), labelPaint);
                }
            }
        }
    }

    /** Continuous hazy rainbow aligned to the lanes: each lane's colour blended top-to-bottom. */
    private void drawRainbow(Canvas canvas, int w, int h) {
        if (bgShader == null || bgShaderH != h) {
            int n = lanes.size();
            int[] colors = new int[n];
            float[] stops = new float[n];
            for (int k = 0; k < n; k++) {
                colors[k] = softTint(ColorUtils.colorGroupAccent(laneGroups.get(k)));
                stops[k] = n == 1 ? 0f : (k + 0.5f) / n;
            }
            if (n == 1) { stops = new float[]{0f, 1f}; colors = new int[]{colors[0], colors[0]}; }
            bgShader = new LinearGradient(0, 0, 0, h, colors, stops, Shader.TileMode.CLAMP);
            bgShaderH = h;
        }
        paint.setAlpha(255);
        paint.setShader(bgShader);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
        // Light scrim keeps icons/labels legible without washing out the colour.
        paint.setColor(0x26000000);
        canvas.drawRect(0, 0, w, h, paint);
    }

    /** A vivid, saturated version of a group accent for the backdrop. */
    private int softTint(int accent) {
        float[] hsv = new float[3];
        Color.colorToHSV(accent, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 1.05f);
        hsv[2] = Math.min(1f, hsv[2] * 1.0f);
        return Color.HSVToColor(hsv);
    }

    // --- scrolling & taps ---

    private GestureDetector buildGestures(Context context) {
        return new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) {
                if (!scroller.isFinished()) scroller.abortAnimation();
                return true;
            }
            @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                doScroll(scrollX + (int) dx);
                return true;
            }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                scroller.fling(scrollX, 0, (int) -vx, 0, 0, maxScroll(), 0, 0);
                postInvalidateOnAnimation();
                return true;
            }
            @Override public boolean onSingleTapUp(MotionEvent e) {
                return tapAt(e.getX(), e.getY());
            }
        });
    }

    private boolean tapAt(float x, float y) {
        if (lanes.isEmpty()) return false;
        float lh = laneHeight(), icon = iconSize(), spacing = colSpacing();
        int k = (int) (y / lh);
        if (k < 0 || k >= lanes.size()) return false;
        List<AppInfo> lane = lanes.get(k);
        int j = Math.round((x + scrollX - padX() - spacing / 2f) / spacing);
        if (j < 0 || j >= lane.size()) return false;
        float cx = padX() + j * spacing + spacing / 2f - scrollX;
        if (Math.abs(x - cx) > icon * 0.75f) return false;
        if (listener != null) listener.onAppClick(lane.get(j));
        return true;
    }

    private int maxScroll() { return Math.max(0, contentWidth - getWidth()); }

    private void doScroll(int x) {
        scrollX = Math.max(0, Math.min(x, maxScroll()));
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            doScroll(scroller.getCurrX());
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestures.onTouchEvent(event);
    }

    private String ellipsize(String s, Paint p, float maxWidth) {
        if (p.measureText(s) <= maxWidth) return s;
        String ell = "…";
        while (s.length() > 1 && p.measureText(s + ell) > maxWidth) s = s.substring(0, s.length() - 1);
        return s + ell;
    }
}
