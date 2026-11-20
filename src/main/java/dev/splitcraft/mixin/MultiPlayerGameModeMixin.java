package dev.splitcraft.mixin;

import dev.splitcraft.session.SplitSession;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While a Player 2 screen is open, clicks and menu buttons go to Player 2's server-side menu, not the host's. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void sc$click(int containerId, int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
        if (SplitSession.p2ScreenOpen()) { SplitSession.p2Click(slotId, button, type); ci.cancel(); }
    }
    @Inject(method = "handleInventoryButtonClick", at = @At("HEAD"), cancellable = true)
    private void sc$button(int containerId, int buttonId, CallbackInfo ci) {
        if (SplitSession.p2ScreenOpen()) { SplitSession.p2Button(buttonId); ci.cancel(); }
    }
}
