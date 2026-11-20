package dev.splitcraft.session;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.splitcraft.input.*;
import dev.splitcraft.render.SplitRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/** Owns the lifecycle of a split-screen game: start/stop, pad ownership, input forwarding, state mirroring. */
public final class SplitSession {
    private static boolean active, p2Screen, prevInvBtn;
    private static Pad pad1, pad2;
    private static Player2 p2;
    private static Screen p2ScreenObj;
    private static int hotbarCooldown;

    public static boolean active() { return active; }
    public static boolean p2ScreenOpen() { return active && p2Screen; }
    public static Pad pad1() { return pad1; }
    /** The pad Controlify should currently treat as "the" controller: P1's, or P2's while P2's inventory is open. */
    public static Pad owner() { return p2Screen ? pad2 : pad1; }
    public static Entity clientPlayer2(Minecraft mc) { return p2 == null || mc.level == null ? null : mc.level.getEntity(p2.getId()); }

    public static void start(Minecraft mc, Pad a, Pad b) {
        var server = mc.getSingleplayerServer();
        if (server == null || mc.player == null || a == null || b == null) return;
        pad1 = a; pad2 = b; active = true; p2Screen = false;
        Controlify.instance().setCurrentController(a.entity, true);
        ServerPlayer host = server.getPlayerList().getPlayer(mc.player.getUUID());
        server.execute(() -> p2 = Player2.spawn(server, host));
        mc.setScreen(null);
    }

    public static void stop(Minecraft mc) {
        active = false; p2Screen = false;
        var server = mc.getSingleplayerServer();
        Player2 p = p2; p2 = null;
        if (server != null && p != null) server.execute(p::despawn);
        SplitRenderer.reset(mc);
    }

    /** Called (via mixin) for every click inside P2's inventory screen. */
    public static void p2Click(int slot, int button, ClickType type) {
        Player2 sp = p2;
        if (sp == null) return;
        sp.server.execute(() -> { sp.containerMenu.clicked(slot, button, type, sp); sp.containerMenu.broadcastChanges(); });
    }

    public static void p2Button(int id) {
        Player2 sp = p2;
        if (sp == null) return;
        sp.server.execute(() -> { sp.containerMenu.clickMenuButton(sp, id); sp.containerMenu.broadcastChanges(); });
    }

    public static void clientTick(Minecraft mc) {
        if (!active) return;
        if (mc.level == null || mc.getSingleplayerServer() == null) { stop(mc); return; }
        Player2 sp = p2;
        if (sp == null) return;
        ControllerStateView s = pad2.entity.input().map(i -> (ControllerStateView) i.stateNow()).orElse(null);
        if (s == null) return;
        boolean sw = pad2.kind == PadKind.SWITCH_PRO;
        boolean invBtn = s.isButtonDown(sw ? GamepadInputs.WEST_BUTTON : GamepadInputs.NORTH_BUTTON);   // "Y" by label
        Entity cp2 = clientPlayer2(mc);

        // ---- a Player 2 screen is open (inventory, crafting table, chest, furnace...): pad 2 is Controlify's active pad
        if (p2Screen) {
            if (mc.screen != p2ScreenObj) {                       // closed -> give Controlify back to P1, close P2's menu server-side
                p2Screen = false; p2ScreenObj = null;
                Controlify.instance().setCurrentController(pad1.entity, true);
                sp.server.execute(sp::closeContainer);
            } else if (mc.screen instanceof InventoryScreen && cp2 instanceof Player cp) {   // inventory screen uses the client mirror
                var cm = cp.inventoryMenu; var sm = sp.inventoryMenu;
                for (int i = 0; i < cm.slots.size(); i++) {
                    ItemStack st = sm.getSlot(i).getItem();
                    if (!ItemStack.matches(st, cm.getSlot(i).getItem())) cm.getSlot(i).set(st.copy());
                }
                cm.setCarried(sm.getCarried().copy());
            }
            sp.input = Player2.Pad2Input.NONE; prevInvBtn = invBtn; return;
        }
        Player2.PendingMenu pm = sp.takePendingMenu();               // P2 right-clicked a crafting table / chest / furnace ...
        if (pm != null && mc.screen == null) {
            Screen scr = P2Screens.create(pm.menu(), pm.title(), sp.getInventory());
            if (scr == null) sp.server.execute(sp::closeContainer);   // unsupported menu type
            else { openP2Screen(mc, scr); sp.input = Player2.Pad2Input.NONE; prevInvBtn = invBtn; return; }
        }
        if (invBtn && !prevInvBtn && mc.screen == null && cp2 instanceof Player cp) {
            openP2Screen(mc, new InventoryScreen(cp));
            sp.input = Player2.Pad2Input.NONE; prevInvBtn = invBtn; return;
        }
        prevInvBtn = invBtn;

        // ---- normal gameplay input
        float mx = s.getAxisState(GamepadInputs.LEFT_STICK_AXIS_RIGHT) - s.getAxisState(GamepadInputs.LEFT_STICK_AXIS_LEFT);
        float my = s.getAxisState(GamepadInputs.LEFT_STICK_AXIS_UP) - s.getAxisState(GamepadInputs.LEFT_STICK_AXIS_DOWN);
        float lx = s.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_RIGHT) - s.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_LEFT);
        float ly = s.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_DOWN) - s.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_UP);
        boolean jump = s.isButtonDown(sw ? GamepadInputs.EAST_BUTTON : GamepadInputs.SOUTH_BUTTON);          // "A" by label
        boolean sneak = s.isButtonDown(GamepadInputs.RIGHT_STICK_BUTTON);
        boolean sprint = s.isButtonDown(GamepadInputs.LEFT_STICK_BUTTON);
        boolean attack = s.getAxisState(GamepadInputs.RIGHT_TRIGGER_AXIS) > 0.5f;
        boolean use = s.getAxisState(GamepadInputs.LEFT_TRIGGER_AXIS) > 0.5f;
        int hb = 0;
        if (hotbarCooldown > 0) hotbarCooldown--;
        else if (s.isButtonDown(GamepadInputs.RIGHT_SHOULDER_BUTTON)) { hb = 1; hotbarCooldown = 5; }
        else if (s.isButtonDown(GamepadInputs.LEFT_SHOULDER_BUTTON)) { hb = -1; hotbarCooldown = 5; }
        sp.input = new Player2.Pad2Input(dz(mx), dz(my), dz(lx), dz(ly), jump, sneak, sprint, attack, use, hb);

        // Mirror server-side P2 state onto the client's RemotePlayer so the HUD (hotbar, hearts, food) is right.
        if (cp2 instanceof Player cp) {
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack st = sp.getInventory().getItem(i);
                if (!ItemStack.matches(st, cp.getInventory().getItem(i))) cp.getInventory().setItem(i, st.copy());
            }
            cp.getInventory().setSelectedSlot(sp.getInventory().getSelectedSlot());
            cp.getFoodData().setFoodLevel(sp.getFoodData().getFoodLevel());
            cp.setHealth(sp.getHealth());
        }
    }
    private static void openP2Screen(Minecraft mc, Screen scr) {
        p2Screen = true; p2ScreenObj = scr;
        Controlify.instance().setCurrentController(pad2.entity, true);
        mc.setScreen(scr);
    }
    private static float dz(float v) { return Math.abs(v) < 0.15f ? 0f : v; }
    private SplitSession() {}
}
