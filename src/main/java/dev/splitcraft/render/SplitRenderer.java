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

/** Renders each player into a private target, then composites the two viewports. */
public final class SplitRenderer {
    private static final Logger LOG = LoggerFactory.getLogger("splitcraft");
    public static volatile int ovW, ovH;
    public static volatile int pass = -1;
    private static boolean busy;
    private static int sizedW, sizedH;
    private static final RenderTarget[] targets = new RenderTarget[2];
    private static RenderTarget uiTarget;
    private static Screen initializedScreen;
    private static int initializedScreenW = -1, initializedScreenH = -1;

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

    private static void draw(Minecraft mc, GameRenderer gr, DeltaTracker dt, boolean renderLevel,
                             RenderTarget real, Entity p2, Screen screenBefore) {
        int fw = real.width, fh = real.height;
        boolean resize = SplitConfig.renderMode() == RenderMode.RESIZE;
        SplitLayout first = SplitLayout.forPlayer(0, SplitConfig.orientation(), fw, fh);
        if (resize) ensureInternalSize(mc, first.w(), first.h()); else restoreInternalSize(mc);

        var enc = RenderSystem.getDevice().createCommandEncoder();
        enc.clearColorTexture(real.getColorTexture(), 0xFF000000);
        mc.screen = null;
        for (int i = 0; i < 2; i++) {
            SplitLayout l = SplitLayout.forPlayer(i, SplitConfig.orientation(), fw, fh);
            int tw = resize ? l.w() : fw, th = resize ? l.h() : fh;
            RenderTarget t = target(i, tw, th);
            enc.clearColorAndDepthTextures(t.getColorTexture(), 0xFF000000, t.getDepthTexture(), 1.0);
            if (resize) { ovW = l.w(); ovH = l.h(); }
            pass = i;
            ((MinecraftAccessor) mc).splitcraft$setMainRenderTarget(t);
            mc.setCameraEntity(i == 0 ? mc.player : p2);
            gr.render(dt, renderLevel);
            ((MinecraftAccessor) mc).splitcraft$setMainRenderTarget(real);
            ovW = ovH = 0; pass = -1;
            copyViewport(enc, t, real, l, fw, fh, resize);
        }

        // A Minecraft Screen is global, so render it into only the viewport belonging to its owner.
        // The other player's already-composited viewport remains untouched and fully playable.
        if (screenBefore != null) {
            int owner = SplitSession.p2ScreenOpen() ? 1 : 0;
            SplitLayout l = SplitLayout.forPlayer(owner, SplitConfig.orientation(), fw, fh);
            RenderTarget worldTarget = targets[owner];
            RenderTarget overlay = resize ? worldTarget : uiTarget(l.w(), l.h());
            if (!resize) {
                int srcX = (fw - l.w()) / 2;
                int srcY = (fh - l.h()) / 2;
                enc.copyTextureToTexture(worldTarget.getColorTexture(), overlay.getColorTexture(),
                        0, 0, 0, srcX, srcY, l.w(), l.h());
                enc.clearColorAndDepthTextures(overlay.getColorTexture(), 0xFF000000,
                        overlay.getDepthTexture(), 1.0);
                // Restore the cropped world image after clearing the overlay's depth buffer.
                enc.copyTextureToTexture(worldTarget.getColorTexture(), overlay.getColorTexture(),
                        0, 0, 0, srcX, srcY, l.w(), l.h());
            }
            ovW = l.w(); ovH = l.h(); pass = owner;
            ((MinecraftAccessor) mc).splitcraft$setMainRenderTarget(overlay);
            mc.setCameraEntity(owner == 0 ? mc.player : p2);
            mc.screen = screenBefore;
            mc.options.hideGui = true;
            int guiW = (int) Math.ceil((double) l.w() / mc.getWindow().getGuiScale());
            int guiH = (int) Math.ceil((double) l.h() / mc.getWindow().getGuiScale());
            if (initializedScreen != screenBefore || initializedScreenW != guiW || initializedScreenH != guiH) {
                screenBefore.init(mc, guiW, guiH);
                initializedScreen = screenBefore;
                initializedScreenW = guiW;
                initializedScreenH = guiH;
            }
            gr.render(dt, false);
            ((MinecraftAccessor) mc).splitcraft$setMainRenderTarget(real);
            ovW = ovH = 0; pass = -1;
            copyViewport(enc, overlay, real, l, fw, fh, true);
        } else {
            initializedScreen = null;
            initializedScreenW = initializedScreenH = -1;
        }
        mc.screen = screenBefore;
        mc.setCameraEntity(mc.player);
    }

    private static void copyViewport(com.mojang.blaze3d.systems.CommandEncoder enc, RenderTarget src,
                                     RenderTarget dst, SplitLayout l, int fw, int fh, boolean alreadySized) {
        int srcX = alreadySized ? 0 : (src.width - l.w()) / 2;
        int srcY = alreadySized ? 0 : (src.height - l.h()) / 2;
        enc.copyTextureToTexture(src.getColorTexture(), dst.getColorTexture(), 0,
                l.x(), fh - l.y() - l.h(), srcX, srcY, l.w(), l.h());
    }

    private static RenderTarget uiTarget(int w, int h) {
        if (uiTarget == null || uiTarget.width != w || uiTarget.height != h) {
            if (uiTarget != null) uiTarget.destroyBuffers();
            uiTarget = new TextureTarget("splitcraft_ui", w, h, true);
        }
        return uiTarget;
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

    public static void reset(Minecraft mc) {
        try { restoreInternalSize(mc); } catch (Throwable ignored) {}
        sizedW = sizedH = 0;
        for (int i = 0; i < 2; i++) if (targets[i] != null) { targets[i].destroyBuffers(); targets[i] = null; }
        if (uiTarget != null) { uiTarget.destroyBuffers(); uiTarget = null; }
        initializedScreen = null;
        initializedScreenW = initializedScreenH = -1;
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