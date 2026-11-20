package dev.splitcraft.mixin;

import dev.splitcraft.input.PadScanner;
import dev.splitcraft.pairing.PairingScreen;
import dev.splitcraft.session.SplitSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

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
        ).bounds(10, this.height - 30, 110, 20).build();
        button.active = active || pads >= 2;
        this.addRenderableWidget(button);
    }
}
