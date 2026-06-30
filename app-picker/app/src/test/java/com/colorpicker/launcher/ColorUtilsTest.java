package com.colorpicker.launcher;

import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Unit tests for ColorUtils color grouping and sorting logic.
 * Uses Robolectric so android.graphics.Color works on JVM.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ColorUtilsTest {

    // --- colorGroupIndex tests ---

    @Test
    public void blackGroup_lowSaturationLowBrightness() {
        // Black: sat < 0.15, brightness < 0.3
        assertEquals(0, ColorUtils.colorGroupIndex(0f, 0.05f, 0.1f));
        assertEquals(0, ColorUtils.colorGroupIndex(180f, 0.10f, 0.2f));
    }

    @Test
    public void whiteGroup_lowSaturationHighBrightness() {
        // White: sat < 0.15, brightness > 0.85
        assertEquals(11, ColorUtils.colorGroupIndex(0f, 0.05f, 0.95f));
        assertEquals(11, ColorUtils.colorGroupIndex(120f, 0.10f, 0.90f));
    }

    @Test
    public void grayGroup_lowSaturationMidBrightness() {
        // Gray: sat < 0.15, brightness between 0.3 and 0.85
        assertEquals(10, ColorUtils.colorGroupIndex(0f, 0.10f, 0.5f));
        assertEquals(10, ColorUtils.colorGroupIndex(200f, 0.05f, 0.6f));
    }

    @Test
    public void redGroup_lowHue() {
        assertEquals(1, ColorUtils.colorGroupIndex(5f, 0.8f, 0.7f));
        assertEquals(1, ColorUtils.colorGroupIndex(10f, 0.5f, 0.5f));
    }

    @Test
    public void redGroup_highHueWraparound() {
        // Hue >= 330 wraps back to red
        assertEquals(1, ColorUtils.colorGroupIndex(340f, 0.8f, 0.7f));
        assertEquals(1, ColorUtils.colorGroupIndex(355f, 0.6f, 0.5f));
    }

    @Test
    public void orangeGroup() {
        assertEquals(2, ColorUtils.colorGroupIndex(25f, 0.8f, 0.7f));
        assertEquals(2, ColorUtils.colorGroupIndex(35f, 0.6f, 0.5f));
    }

    @Test
    public void yellowGroup() {
        assertEquals(3, ColorUtils.colorGroupIndex(50f, 0.8f, 0.7f));
        assertEquals(3, ColorUtils.colorGroupIndex(65f, 0.6f, 0.5f));
    }

    @Test
    public void greenGroup() {
        assertEquals(4, ColorUtils.colorGroupIndex(100f, 0.8f, 0.7f));
        assertEquals(4, ColorUtils.colorGroupIndex(140f, 0.6f, 0.5f));
    }

    @Test
    public void tealGroup() {
        assertEquals(5, ColorUtils.colorGroupIndex(160f, 0.8f, 0.7f));
        assertEquals(5, ColorUtils.colorGroupIndex(185f, 0.6f, 0.5f));
    }

    @Test
    public void blueGroup() {
        assertEquals(6, ColorUtils.colorGroupIndex(210f, 0.8f, 0.7f));
        assertEquals(6, ColorUtils.colorGroupIndex(240f, 0.6f, 0.5f));
    }

    @Test
    public void purpleGroup() {
        assertEquals(7, ColorUtils.colorGroupIndex(260f, 0.8f, 0.7f));
        assertEquals(7, ColorUtils.colorGroupIndex(285f, 0.6f, 0.5f));
    }

    @Test
    public void pinkGroup() {
        assertEquals(8, ColorUtils.colorGroupIndex(300f, 0.8f, 0.7f));
        assertEquals(8, ColorUtils.colorGroupIndex(320f, 0.6f, 0.5f));
    }

    // --- colorGroupName tests ---

    @Test
    public void groupNames_allMapped() {
        assertEquals("Black", ColorUtils.colorGroupName(0));
        assertEquals("Red", ColorUtils.colorGroupName(1));
        assertEquals("Orange", ColorUtils.colorGroupName(2));
        assertEquals("Yellow", ColorUtils.colorGroupName(3));
        assertEquals("Green", ColorUtils.colorGroupName(4));
        assertEquals("Teal", ColorUtils.colorGroupName(5));
        assertEquals("Blue", ColorUtils.colorGroupName(6));
        assertEquals("Purple", ColorUtils.colorGroupName(7));
        assertEquals("Pink", ColorUtils.colorGroupName(8));
        assertEquals("Brown", ColorUtils.colorGroupName(9));
        assertEquals("Gray", ColorUtils.colorGroupName(10));
        assertEquals("White", ColorUtils.colorGroupName(11));
    }

    @Test
    public void groupName_outOfRange_returnsOther() {
        assertEquals("Other", ColorUtils.colorGroupName(99));
        assertEquals("Other", ColorUtils.colorGroupName(-1));
    }

    // --- toHSB tests ---

    @Test
    public void toHSB_pureRed() {
        float[] hsb = ColorUtils.toHSB(Color.RED);
        assertEquals(0f, hsb[0], 1f);       // hue ~0
        assertEquals(1f, hsb[1], 0.01f);     // fully saturated
        assertEquals(1f, hsb[2], 0.01f);     // full brightness
    }

    @Test
    public void toHSB_pureGreen() {
        float[] hsb = ColorUtils.toHSB(Color.GREEN);
        assertEquals(120f, hsb[0], 1f);      // hue ~120
        assertEquals(1f, hsb[1], 0.01f);
        assertEquals(1f, hsb[2], 0.01f);
    }

    @Test
    public void toHSB_pureBlue() {
        float[] hsb = ColorUtils.toHSB(Color.BLUE);
        assertEquals(240f, hsb[0], 1f);      // hue ~240
        assertEquals(1f, hsb[1], 0.01f);
        assertEquals(1f, hsb[2], 0.01f);
    }

    @Test
    public void toHSB_white() {
        float[] hsb = ColorUtils.toHSB(Color.WHITE);
        assertEquals(0f, hsb[1], 0.01f);     // no saturation
        assertEquals(1f, hsb[2], 0.01f);     // full brightness
    }

    @Test
    public void toHSB_black() {
        float[] hsb = ColorUtils.toHSB(Color.BLACK);
        assertEquals(0f, hsb[1], 0.01f);     // no saturation
        assertEquals(0f, hsb[2], 0.01f);     // no brightness
    }

    // --- End-to-end: color -> group mapping ---

    @Test
    public void pureRed_mapsToRedGroup() {
        float[] hsb = ColorUtils.toHSB(Color.RED);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("Red", ColorUtils.colorGroupName(group));
    }

    @Test
    public void pureGreen_mapsToGreenGroup() {
        float[] hsb = ColorUtils.toHSB(Color.GREEN);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("Green", ColorUtils.colorGroupName(group));
    }

    @Test
    public void pureBlue_mapsToBlueGroup() {
        float[] hsb = ColorUtils.toHSB(Color.BLUE);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("Blue", ColorUtils.colorGroupName(group));
    }

    @Test
    public void white_mapsToWhiteGroup() {
        float[] hsb = ColorUtils.toHSB(Color.WHITE);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("White", ColorUtils.colorGroupName(group));
    }

    @Test
    public void black_mapsToBlackGroup() {
        float[] hsb = ColorUtils.toHSB(Color.BLACK);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("Black", ColorUtils.colorGroupName(group));
    }

    @Test
    public void gray_mapsToGrayGroup() {
        float[] hsb = ColorUtils.toHSB(Color.GRAY);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("Gray", ColorUtils.colorGroupName(group));
    }

    @Test
    public void orange_mapsToOrangeGroup() {
        int orange = Color.rgb(255, 165, 0);
        float[] hsb = ColorUtils.toHSB(orange);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("Orange", ColorUtils.colorGroupName(group));
    }

    @Test
    public void cyan_mapsToTealGroup() {
        float[] hsb = ColorUtils.toHSB(Color.CYAN);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("Teal", ColorUtils.colorGroupName(group));
    }

    @Test
    public void magenta_mapsToPinkGroup() {
        float[] hsb = ColorUtils.toHSB(Color.MAGENTA);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("Pink", ColorUtils.colorGroupName(group));
    }

    @Test
    public void yellow_mapsToYellowGroup() {
        float[] hsb = ColorUtils.toHSB(Color.YELLOW);
        int group = ColorUtils.colorGroupIndex(hsb[0], hsb[1], hsb[2]);
        assertEquals("Yellow", ColorUtils.colorGroupName(group));
    }

    // --- Sorting order: group indices should follow rainbow order ---

    @Test
    public void groupOrder_rainbowSequence() {
        // Verify red < orange < yellow < green < teal < blue < purple < pink
        float[] red    = {5f,   0.8f, 0.7f};
        float[] orange = {25f,  0.8f, 0.7f};
        float[] yellow = {55f,  0.8f, 0.7f};
        float[] green  = {120f, 0.8f, 0.7f};
        float[] teal   = {170f, 0.8f, 0.7f};
        float[] blue   = {220f, 0.8f, 0.7f};
        float[] purple = {270f, 0.8f, 0.7f};
        float[] pink   = {310f, 0.8f, 0.7f};

        float[][] colors = {red, orange, yellow, green, teal, blue, purple, pink};
        for (int i = 0; i < colors.length - 1; i++) {
            int groupA = ColorUtils.colorGroupIndex(colors[i][0], colors[i][1], colors[i][2]);
            int groupB = ColorUtils.colorGroupIndex(colors[i+1][0], colors[i+1][1], colors[i+1][2]);
            assertTrue("Group " + groupA + " should be < " + groupB,
                    groupA < groupB);
        }
    }

    // --- colorGroupAccent tests ---

    @Test
    public void accentColors_nonNull() {
        for (int i = 0; i < ColorUtils.COLOR_GROUP_COUNT; i++) {
            int accent = ColorUtils.colorGroupAccent(i);
            assertNotEquals("Accent for group " + i + " should not be transparent",
                    0, accent);
        }
    }

    // --- Per-package group overrides (reported mis-categorizations) ---
    // Each app below was landing in the wrong color group; the override forces the right one.
    // The stored hue/saturation in these AppInfos is deliberately the *wrong* group so the test
    // proves the override wins over normal classification.

    private static AppInfo appWith(String pkg, float hue, float sat, float bri) {
        return new AppInfo("label", pkg, null, hue, sat, bri, Color.HSVToColor(new float[]{hue, sat, bri}));
    }

    @Test
    public void googleMaps_overriddenToRainbow() {
        // Maps' multi-color pin should read as Rainbow even though one hue can dominate.
        AppInfo maps = appWith("com.google.android.apps.maps", 220f, 0.8f, 0.7f); // would be Blue
        assertEquals(ColorUtils.GROUP_RAINBOW, ColorUtils.colorGroupIndex(maps));
        assertEquals("Rainbow", ColorUtils.colorGroupName(ColorUtils.colorGroupIndex(maps)));
    }

    @Test
    public void gmail_overriddenToRainbow() {
        // Gmail's red/blue/yellow/green "M" on white should be Rainbow, not White.
        AppInfo gmail = appWith("com.google.android.gm", 0f, 0.0f, 0.95f); // would be White
        assertEquals(ColorUtils.GROUP_RAINBOW, ColorUtils.colorGroupIndex(gmail));
    }

    @Test
    public void claude_overriddenToOrange() {
        // Claude's coral sits at ~14° and falls into Red by hue; it should be Orange.
        AppInfo claude = appWith("com.anthropic.claude", 14f, 0.6f, 0.85f); // would be Red
        assertEquals(ColorUtils.GROUP_ORANGE, ColorUtils.colorGroupIndex(claude));
        assertEquals("Orange", ColorUtils.colorGroupName(ColorUtils.colorGroupIndex(claude)));
    }

    @Test
    public void balatro_overriddenToRed() {
        // Balatro reads as Blue from its averaged color but feels Red.
        AppInfo balatro = appWith("com.playstack.balatro.android", 220f, 0.8f, 0.5f); // would be Blue
        assertEquals(ColorUtils.GROUP_RED, ColorUtils.colorGroupIndex(balatro));
        assertEquals("Red", ColorUtils.colorGroupName(ColorUtils.colorGroupIndex(balatro)));
    }

    @Test
    public void nonOverriddenApp_classifiedNormally() {
        // A package with no override falls back to hue-based classification.
        AppInfo blueApp = appWith("com.example.unknown", 220f, 0.8f, 0.7f);
        assertEquals("Blue", ColorUtils.colorGroupName(ColorUtils.colorGroupIndex(blueApp)));
        assertNull(ColorUtils.groupOverride("com.example.unknown"));
    }
}
