package dev.splitcraft;

import dev.splitcraft.config.SplitConfig;
import dev.splitcraft.input.PadScanner;
import dev.splitcraft.pairing.PairingScreen;
import dev.splitcraft.session.SplitSession;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

public class SplitcraftClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        SplitConfig.load();

        // 1. Pause-menu button, local (integrated-server) worlds only
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (!(screen instanceof PauseScreen) || !mc.hasSingleplayerServer() || mc.getSingleplayerServer().isPublished()) return;
            boolean active = SplitSession.active();
            int pads = 0;
            try { PadScanner s = new PadScanner(); s.refresh(); pads = s.pads().size(); } catch (Throwable ignored) {}
            Button b = Button.builder(Component.literal(active ? "Stop Split Screen" : "Split Screen"), btn -> {
                if (active) { SplitSession.stop(mc); mc.setScreen(null); }
                else mc.setScreen(new PairingScreen(screen, (p1, p2) -> SplitSession.start(mc, p1, p2)));
            }).bounds(10, h - 30, 110, 20).build();
            b.active = active || pads >= 2;
            screen.addRenderableWidget(b);
        });

        ClientTickEvents.END_CLIENT_TICK.register(SplitSession::clientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> SplitSession.stop(mc));
    }
}
