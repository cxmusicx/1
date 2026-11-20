package dev.splitcraft.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.splitcraft.config.SplitConfig;
import dev.splitcraft.config.SplitConfig.RenderMode;
import dev.splitcraft.layout.SplitLayout;
import dev.splitcraft.mixin.MinecraftAccessor;
import dev.splitcraft.session.SplitSession;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Draws the frame once per player, copies the results into the window target side by side / stacked,
 * then draws any open screen on top at full size.
 *
 * RESIZE mode: each view is rendered at viewport size (window size is faked while drawing).
 * CROP mode:   each view is rendered at normal window size and the middle is copied out. No size tricks at all.
 * If RESIZE throws, we log it, switch to CROP automatically and keep going. If CROP throws, split-screen stops.
 */
public final class SplitRenderer {
    private static final Logger LOG = LoggerFactory.getLogger("splitcraft");
    public static volatile int ovW, ovH;   // read by WindowMixin (only non-zero in RESIZE mode)
    public static volatile int pass = -1;  // 0 / 1 while a player's view is drawn, else -1
    private static boolean busy;
    private static int sizedW, sizedH;
    private static final RenderTarget[] targets = new RenderTarget[2];

    public static boolean renderSplit(GameRenderer gr, DeltaTracker dt, boolean renderLevel) {
        Minecraft mc = Minecraft.getInstance();
        if (busy || !SplitSession.active() || mc.level == null) return false;
        Entity p2 = SplitSession.clientPlayer2(mc);
        if (p2 == null) return false;

        busy = true;
        RenderTarget real = mc.getMainRenderTarget();
        Entity camBefore = mc.getCameraEntity();
        Screen screenBefore = mc.screen;
        boolean hideGuiBefore = mc.options.hideGui;
        try {
            draw(mc, gr, dt, renderLevel, real, p2, screenBefore);
        } catch (Throwable t) {
            LOG.error("Split-screen render failed in {} mode", SplitConfig.renderMode(), t);
            if (SplitConfig.renderMode() == RenderMode.RESIZE) {
                SplitConfig.setRenderMode(RenderMode.CROP);
                notice(mc, "Splitcraft: switched to safe CROP render mode (see latest.log).");
            } else {
                SplitSession.stop(mc);
                notice(mc, "Splitcraft: rendering failed, split-screen stopped (see latest.log).");
            }
        } finally {
            ((MinecraftAccessor) mc).splitcraft$setMainRenderTarget(real);
            ovW = ovH = 0; pass = -1;
            mc.screen = screenBefore;
            mc.options.hideGui = hideGuiBefore;
            mc.setCameraEntity(camBefore);
            busy = false;
        }
        return true;
    }

    private static void draw(Minecraft mc, GameRenderer gr, DeltaTracker dt, boolean renderLevel, RenderTarget real, Entity p2, Screen screenBefore) {
        int fw = real.width, fh = real.height;
        boolean resize = SplitConfig.renderMode() == RenderMode.RESIZE;
        SplitLayout l0 = SplitLayout.forPlayer(0, SplitConfig.orientation(), fw, fh);
        if (resize) ensureInternalSize(mc, l0.w(), l0.h()); else restoreInternalSize(mc);

        var enc = RenderSystem.getDevice().createCommandEncoder();
        enc.clearColorTexture(real.getColorTexture(), 0xFF000000);
        mc.screen = null;                                   // screens are drawn once, on top, after both world passes
        for (int i = 0; i < 2; i++) {
            SplitLayout l = SplitLayout.forPlayer(i, SplitConfig.orientation(), fw, fh);
            int tw = resize ? l.w() : fw, th = resize ? l.h() : fh;
            RenderTarget t = target(i, tw, th);
            enc.clearColorAndDepthTextures(t.getColorTexture(), 0, t.getDepthTexture(), 1.0);
            if (resize) { ovW = l.w(); ovH = l.h(); }
            pass = i;
            ((MinecraftAccessor) mc).splitcraft$setMainRenderTarget(t);
            mc.setCameraEntity(i == 0 ? mc.player : p2);
            gr.render(dt, renderLevel);                     // re-enters; `busy` makes our hook step aside
            ((MinecraftAccessor) mc).splitcraft$setMainRenderTarget(real);
            ovW = ovH = 0; pass = -1;
            int srcX = (tw - l.w()) / 2, srcY = (th - l.h()) / 2;   // 0 in RESIZE mode, centre crop in CROP mode
            enc.copyTextureToTexture(t.getColorTexture(), real.getColorTexture(), 0, l.x(), fh - l.y() - l.h(), srcX, srcY, l.w(), l.h()); // GL is bottom-left origin
        }
        mc.screen = screenBefore;
        mc.setCameraEntity(mc.player);
        if (screenBefore != null) {                         // overlay pass: just the screen, full window, no HUD / world
            mc.options.hideGui = true;
            gr.render(dt, false);
        }
    }

    private static void notice(Minecraft mc, String msg) {
        if (mc.gui != null) mc.gui.getChat().addMessage(Component.literal(msg));
    }

    private static void ensureInternalSize(Minecraft mc, int w, int h) {
        if (w == sizedW && h == sizedH) return;
        mc.levelRenderer.resize(w, h);
        mc.gameRenderer.resize(w, h);
        sizedW = w; sizedH = h;
    }
    private static void restoreInternalSize(Minecraft mc) {
        if (sizedW == 0) return;
        RenderTarget real = mc.getMainRenderTarget();
        mc.levelRenderer.resize(real.width, real.height);
        mc.gameRenderer.resize(real.width, real.height);
        sizedW = sizedH = 0;
    }

    /** Call when split-screen ends. */
    public static void reset(Minecraft mc) {
        try { restoreInternalSize(mc); } catch (Throwable ignored) {}
        sizedW = sizedH = 0;
        for (int i = 0; i < 2; i++) if (targets[i] != null) { targets[i].destroyBuffers(); targets[i] = null; }
    }

    private static RenderTarget target(int i, int w, int h) {
        RenderTarget t = targets[i];
        if (t == null || t.width != w || t.height != h) {
            if (t != null) t.destroyBuffers();
            t = new TextureTarget("splitcraft_p" + (i + 1), w, h, true);
            targets[i] = t;
        }
        return t;
    }
    private SplitRenderer() {}
}
