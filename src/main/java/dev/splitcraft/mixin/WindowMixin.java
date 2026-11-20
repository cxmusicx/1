package dev.splitcraft.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.splitcraft.render.SplitRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While a player's view is being drawn, make the game believe the window is only that viewport's size. */
@Mixin(Window.class)
public abstract class WindowMixin {
    // Minecraft 1.21.11 exposes the GUI scale as an int, not a double.
    @Shadow public abstract int getGuiScale();

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    private void sc$w(CallbackInfoReturnable<Integer> cir) {
        if (SplitRenderer.ovW > 0) cir.setReturnValue(SplitRenderer.ovW);
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private void sc$h(CallbackInfoReturnable<Integer> cir) {
        if (SplitRenderer.ovH > 0) cir.setReturnValue(SplitRenderer.ovH);
    }

    @Inject(method = "getGuiScaledWidth", at = @At("HEAD"), cancellable = true)
    private void sc$gw(CallbackInfoReturnable<Integer> cir) {
        if (SplitRenderer.ovW > 0) cir.setReturnValue((int) Math.ceil((double) SplitRenderer.ovW / getGuiScale()));
    }

    @Inject(method = "getGuiScaledHeight", at = @At("HEAD"), cancellable = true)
    private void sc$gh(CallbackInfoReturnable<Integer> cir) {
        if (SplitRenderer.ovH > 0) cir.setReturnValue((int) Math.ceil((double) SplitRenderer.ovH / getGuiScale()));
    }
}
