package dev.splitcraft.mixin;

import dev.splitcraft.render.SplitRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The first-person hand always shows the LOCAL player's items; during P2's pass show P2's instead. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void sc$head(CallbackInfo ci) {
        if (SplitRenderer.pass == 1 && Minecraft.getInstance().getCameraEntity() instanceof LivingEntity e) {
            mainHandItem = e.getMainHandItem(); offHandItem = e.getOffhandItem();
        }
    }
    @Inject(method = "renderHandsWithItems", at = @At("TAIL"))
    private void sc$tail(CallbackInfo ci) {
        var p = Minecraft.getInstance().player;
        if (SplitRenderer.pass == 1 && p != null) { mainHandItem = p.getMainHandItem(); offHandItem = p.getOffhandItem(); }
    }
}
