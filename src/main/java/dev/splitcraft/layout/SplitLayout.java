package dev.splitcraft.layout;

import dev.splitcraft.config.SplitConfig.Orientation;

/**
 * Viewport math in framebuffer pixels, origin top-left.
 * Both players get the EXACT same size (leftover pixels become black border) so the renderer's
 * window-sized internal buffers only ever need to be resized once, not twice per frame.
 */
public record SplitLayout(int x, int y, int w, int h) {
    public static final int DIVIDER = 2;

    public static SplitLayout forPlayer(int player, Orientation o, int fbW, int fbH) {
        if (o == Orientation.HORIZONTAL) { // stacked
            int half = (fbH - DIVIDER) / 2;
            return new SplitLayout(0, player == 0 ? 0 : half + DIVIDER, fbW, half);
        }
        int half = (fbW - DIVIDER) / 2;    // side by side
        return new SplitLayout(player == 0 ? 0 : half + DIVIDER, 0, half, fbH);
    }
    public float aspect() { return (float) w / h; }
}
