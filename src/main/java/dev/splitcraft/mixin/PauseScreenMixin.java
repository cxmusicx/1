package dev.splitcraft.mixin;

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

        // The integrated server remains available when a singleplayer world is opened to LAN.
        // Do not gate access on controller detection: let users open pairing and see what is detected.
        if (!mc.hasSingleplayerServer()) return;

        boolean active = SplitSession.active();
        Button button = Button.builder(
            Component.literal(active ? "Stop Split Screen" : "Split Screen / Pair Controllers"),
            btn -> {
                if (active) {
                    SplitSession.stop(mc);
                    mc.setScreen(null);
                } else {
                    mc.setScreen(new PairingScreen((PauseScreen)(Object)this,
                        (p1, p2) -> SplitSession.start(mc, p1, p2)));
                }
            }
        ).bounds(10, 10, 190, 20).build();
        this.addRenderableWidget(button);
    }
}
