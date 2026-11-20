package dev.splitcraft.session;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

/**
 * Builds the vanilla screen for a menu Player 2 opened (crafting table, chest, furnace...).
 * The screen is handed the SERVER-side menu object itself (same JVM), so slots, furnace progress and
 * crafting results are live with no syncing; clicks are routed to that menu by MultiPlayerGameModeMixin.
 */
public final class P2Screens {
    public static Screen create(AbstractContainerMenu m, Component title, Inventory inv) {
        if (m instanceof CraftingMenu c) return new CraftingScreen(c, inv, title);
        if (m instanceof ChestMenu c) return new ContainerScreen(c, inv, title);
        if (m instanceof FurnaceMenu c) return new FurnaceScreen(c, inv, title);
        if (m instanceof BlastFurnaceMenu c) return new BlastFurnaceScreen(c, inv, title);
        if (m instanceof SmokerMenu c) return new SmokerScreen(c, inv, title);
        if (m instanceof DispenserMenu c) return new DispenserScreen(c, inv, title);
        if (m instanceof HopperMenu c) return new HopperScreen(c, inv, title);
        if (m instanceof ShulkerBoxMenu c) return new ShulkerBoxScreen(c, inv, title);
        if (m instanceof EnchantmentMenu c) return new EnchantmentScreen(c, inv, title);
        if (m instanceof BrewingStandMenu c) return new BrewingStandScreen(c, inv, title);
        if (m instanceof AnvilMenu c) return new AnvilScreen(c, inv, title);   // renaming doesn't work for P2 (text goes over P1's connection)
        if (m instanceof GrindstoneMenu c) return new GrindstoneScreen(c, inv, title);
        if (m instanceof StonecutterMenu c) return new StonecutterScreen(c, inv, title);
        if (m instanceof LoomMenu c) return new LoomScreen(c, inv, title);
        if (m instanceof CartographyTableMenu c) return new CartographyTableScreen(c, inv, title);
        if (m instanceof SmithingMenu c) return new SmithingScreen(c, inv, title);
        return null; // unsupported menu -> caller closes it
    }
    private P2Screens() {}
}
