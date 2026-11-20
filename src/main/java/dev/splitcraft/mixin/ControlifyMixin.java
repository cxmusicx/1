package dev.splitcraft.mixin;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.splitcraft.session.SplitSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Controlify drives the local player from ONE "current controller" and auto-switches to whichever pad
 * last gave input. During split-screen, pin it to the pad that owns the screen/player (P1 normally,
 * P2 only while P2's inventory is open).
 */
@Mixin(value = Controlify.class, remap = false)
public abstract class ControlifyMixin {
    @Inject(method = "setCurrentController", at = @At("HEAD"), cancellable = true)
    private void splitcraft$pin(ControllerEntity controller, boolean silent, CallbackInfo ci) {
        if (!SplitSession.active() || controller == null) return;
        if (!controller.uid().equals(SplitSession.owner().id())) ci.cancel();
    }
}
