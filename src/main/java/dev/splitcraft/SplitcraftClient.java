package dev.splitcraft;

import dev.splitcraft.config.SplitConfig;
import dev.splitcraft.session.SplitSession;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

public class SplitcraftClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        SplitConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(SplitSession::clientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> SplitSession.stop(mc));
    }
}
