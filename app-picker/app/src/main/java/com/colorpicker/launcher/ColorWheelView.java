package com.colorpicker.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A no-scroll color wheel of all apps. The overview draws one pie wedge per non-empty color group
 * (in rainbow order). Tapping a wedge "focuses" that color and lays its apps out as a tappable grid;
 * tapping the background returns to the wheel.
 */
public class ColorWheelView extends View {

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);
    }

    private final List<AppInfo> apps = new ArrayList<>();
    // group index -> apps, preserving rainbow order, only non-empty groups.
    private final Map<Integer, List<AppInfo>> groups = new LinkedHashMap<>();
    private final List<Integer> presentGroups = new ArrayList<>();

    private int focusedGroup = -1; // -1 == overview
    private final List<Rect> focusHitRects = new ArrayList<>();
    private final List<AppInfo> focusHitApps = new ArrayList<>();

    private final Paint wedgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF wheelBounds = new RectF();

    @Nullable private OnAppClickListener listener;

    public ColorWheelView(Context context) { super(context); init(); }
    public ColorWheelView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        labelPaint.setColor(0xFFE0E0E0);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        dimPaint.setColor(0xCC0D0D0D);
    }

    public void setOnAppClickListener(OnAppClickListener l) { this.listener = l; }

    public void setApps(List<AppInfo> all) {
        apps.clear();
        apps.addAll(all);
        groups.clear();
        presentGroups.clear();
        // group index order 0..11 keeps black, red, orange ... white in a stable rainbow-ish order
        Map<Integer, List<AppInfo>> tmp = new LinkedHashMap<>();
        for (int g = 0; g < ColorUtils.COLOR_GROUP_COUNT; g++) tmp.put(g, new ArrayList<>());
        for (AppInfo a : all) {
            int g = ColorUtils.colorGroupIndex(a.getHue(), a.getSaturation(), a.getBrightness());
            tmp.get(g).add(a);
        }
        for (Map.Entry<Integer, List<AppInfo>> e : tmp.entrySet()) {
            if (!e.getValue().isEmpty()) {
                groups.put(e.getKey(), e.getValue());
                presentGroups.add(e.getKey());
            }
        }
        focusedGroup = -1;
        invalidate();
    }

    /** True if a focused color was showing and we returned to the wheel (lets the host consume back). */
    public boolean popFocus() {
        if (focusedGroup != -1) {
            focusedGroup = -1;
            invalidate();
            return true;
        }
        return false;
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (presentGroups.isEmpty()) return;
        if (focusedGroup == -1) drawWheel(canvas);
        else drawFocused(canvas);
    }

    private void drawWheel(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float radius = Math.min(w, h) / 2f - dp(16);
        wheelBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        int n = presentGroups.size();
        float sweep = 360f / n;
        float start = -90f; // first wedge starts at top

        for (int i = 0; i < n; i++) {
            int g = presentGroups.get(i);
            wedgePaint.setColor(ColorUtils.colorGroupAccent(g));
            canvas.drawArc(wheelBounds, start + i * sweep, sweep, true, wedgePaint);
        }

        // donut hole
        wedgePaint.setColor(0xFF121212);
        canvas.drawCircle(cx, cy, radius * 0.34f, wedgePaint);

        // labels (name + count) at mid-radius of each wedge
        textPaint.setTextSize(dp(13));
        Paint countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        countPaint.setTextAlign(Paint.Align.CENTER);
        countPaint.setTextSize(dp(11));
        float labelR = radius * 0.66f;
        for (int i = 0; i < n; i++) {
            int g = presentGroups.get(i);
            double mid = Math.toRadians(start + i * sweep + sweep / 2f);
            float lx = cx + (float) Math.cos(mid) * labelR;
            float ly = cy + (float) Math.sin(mid) * labelR;
            int label = labelColorFor(ColorUtils.colorGroupAccent(g));
            textPaint.setColor(label);
            countPaint.setColor(label);
            canvas.drawText(ColorUtils.colorGroupName(g), lx, ly, textPaint);
            canvas.drawText(String.valueOf(groups.get(g).size()), lx, ly + dp(15), countPaint);
        }

        // center hint
        textPaint.setColor(0xFFBBBBBB);
        textPaint.setTextSize(dp(12));
        canvas.drawText("tap a color", cx, cy + dp(4), textPaint);
    }

    private void drawFocused(Canvas canvas) {
        focusHitRects.clear();
        focusHitApps.clear();

        int w = getWidth();
        List<AppInfo> list = groups.get(focusedGroup);
        if (list == null) { focusedGroup = -1; return; }

        // dim backdrop
        canvas.drawRect(0, 0, w, getHeight(), dimPaint);

        // header
        textPaint.setColor(ColorUtils.colorGroupAccent(focusedGroup));
        textPaint.setTextSize(dp(20));
        canvas.drawText(ColorUtils.colorGroupName(focusedGroup).toUpperCase(), w / 2f, dp(56), textPaint);
        labelPaint.setColor(0xFF888888);
        labelPaint.setTextSize(dp(12));
        canvas.drawText("tap an app  ·  tap background to go back", w / 2f, dp(78), labelPaint);

        // grid
        int cols = Math.max(3, Math.min(5, w / (int) dp(96)));
        float cell = (w - dp(24)) / cols;
        float iconSize = dp(56);
        float topPad = dp(104);
        labelPaint.setColor(0xFFE0E0E0);
        labelPaint.setTextSize(dp(11));

        for (int i = 0; i < list.size(); i++) {
            AppInfo app = list.get(i);
            int row = i / cols, col = i % cols;
            float cellLeft = dp(12) + col * cell;
            float ccx = cellLeft + cell / 2f;
            float ccy = topPad + row * (iconSize + dp(34)) + iconSize / 2f;

            int l = (int) (ccx - iconSize / 2f);
            int t = (int) (ccy - iconSize / 2f);
            int r = (int) (ccx + iconSize / 2f);
            int b = (int) (ccy + iconSize / 2f);
            Drawable icon = app.getIcon();
            icon.setBounds(l, t, r, b);
            icon.draw(canvas);

            String name = ellipsize(app.getLabel(), labelPaint, cell - dp(6));
            canvas.drawText(name, ccx, b + dp(16), labelPaint);

            focusHitRects.add(new Rect(l - (int) dp(8), t - (int) dp(8),
                    r + (int) dp(8), b + (int) dp(20)));
            focusHitApps.add(app);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return event.getAction() == MotionEvent.ACTION_DOWN || super.onTouchEvent(event);
        }
        float x = event.getX(), y = event.getY();

        if (focusedGroup == -1) {
            int g = hitWedge(x, y);
            if (g != -1) {
                focusedGroup = g;
                invalidate();
            }
            return true;
        }

        // focused: hit-test icons, else return to wheel
        for (int i = 0; i < focusHitRects.size(); i++) {
            if (focusHitRects.get(i).contains((int) x, (int) y)) {
                if (listener != null) listener.onAppClick(focusHitApps.get(i));
                return true;
            }
        }
        focusedGroup = -1;
        invalidate();
        return true;
    }

    private int hitWedge(float x, float y) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - dp(16);
        float dx = x - cx, dy = y - cy;
        float dist = (float) Math.hypot(dx, dy);
        if (dist > radius || dist < radius * 0.34f) return -1; // outside ring / in donut hole

        int n = presentGroups.size();
        float sweep = 360f / n;
        double ang = Math.toDegrees(Math.atan2(dy, dx)); // -180..180, 0 at 3 o'clock
        double rel = (ang - (-90) + 360) % 360;           // relative to top start
        int idx = (int) (rel / sweep);
        if (idx < 0 || idx >= n) return -1;
        return presentGroups.get(idx);
    }

    /** Pick black or white text for legibility over a wedge color. */
    private int labelColorFor(int bg) {
        double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0;
        return lum > 0.6 ? 0xFF111111 : 0xFFFFFFFF;
    }

    private String ellipsize(String s, Paint paint, float maxWidth) {
        if (paint.measureText(s) <= maxWidth) return s;
        String ell = "…";
        while (s.length() > 1 && paint.measureText(s + ell) > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + ell;
    }
}
