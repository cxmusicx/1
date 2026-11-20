package dev.splitcraft.mixin;

import dev.splitcraft.input.PadScanner;
import dev.splitcraft.pairing.PairingScreen;
import dev.splitcraft.session.SplitSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void splitcraft$addButton(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.hasSingleplayerServer() || mc.getSingleplayerServer().isPublished()) return;

        boolean active = SplitSession.active();
        int pads = 0;
        try {
            PadScanner scanner = new PadScanner();
            scanner.refresh();
            pads = scanner.pads().size();
        } catch (Throwable ignored) {}

        Button button = Button.builder(
            Component.literal(active ? "Stop Split Screen" : "Split Screen"),
            btn -> {
                if (active) {
                    SplitSession.stop(mc);
                    mc.setScreen(null);
                } else {
                    mc.setScreen(new PairingScreen((PauseScreen)(Object)this,
                        (p1, p2) -> SplitSession.start(mc, p1, p2)));
                }
            }
        ).bounds(10, this.splitcraft$getHeight() - 30, 110, 20).build();
        button.active = active || pads >= 2;
        this.splitcraft$addRenderableWidget(button);
    }

    @org.spongepowered.asm.mixin.Shadow
    protected abstract int getHeight();

    @org.spongepowered.asm.mixin.Shadow
    protected abstract <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addRenderableWidget(T widget);
}
