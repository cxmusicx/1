package dev.splitcraft.session;

import com.mojang.authlib.GameProfile;
import dev.splitcraft.config.SplitConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * A real ServerPlayer with no real client behind it (same idea as Carpet's fake players), driven each
 * server tick by controller state that the client thread writes into {@link #input}.
 * Its data is saved under a fixed UUID, so Player 2 keeps its own inventory/position between sessions.
 */
public class Player2 extends ServerPlayer {
    public record Pad2Input(float moveX, float moveY, float lookX, float lookY, boolean jump, boolean sneak,
                            boolean sprint, boolean attack, boolean use, int hotbarDelta) {
        public static final Pad2Input NONE = new Pad2Input(0, 0, 0, 0, false, false, false, false, false, 0);
    }
    public record PendingMenu(AbstractContainerMenu menu, Component title) {}

    private static final double BASE_SPEED = 0.1;       // vanilla player base movement speed
    public volatile Pad2Input input = Pad2Input.NONE;
    private volatile PendingMenu pendingMenu;
    private final UUID hostId;
    private final MinecraftServer splitServer;
    private final FakeConnection fakeConnection;
    private boolean wasUse;
    private int respawnDelay;
    private BlockPos breakPos; private float breakProgress;

    private Player2(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation info, UUID hostId) {
        super(server, level, profile, info);
        this.hostId = hostId;
        this.splitServer = server;
        this.fakeConnection = new FakeConnection();
    }

    public static Player2 spawn(MinecraftServer server, ServerPlayer host) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("splitcraft-player2".getBytes()), SplitConfig.player2Name());
        ClientInformation info = ClientInformation.createDefault();
        Player2 p = new Player2(server, (ServerLevel) host.level(), profile, info, host.getUUID());
        server.getPlayerList().placeNewPlayer(p.fakeConnection, p, new CommonListenerCookie(profile, 0, info, false));
        p.teleportToHost(host);
        p.setGameMode(host.gameMode.getGameModeForPlayer());
        return p;
    }

    private void teleportToHost(ServerPlayer host) {
        teleportTo((ServerLevel) host.level(), host.getX() + 1.0, host.getY(), host.getZ(), java.util.Set.of(), host.getYRot(), 0f, false);
    }
    public MinecraftServer splitServer() { return splitServer; }
    public void despawn() { splitServer.getPlayerList().remove(this); }
    public PendingMenu takePendingMenu() { PendingMenu p = pendingMenu; pendingMenu = null; return p; }

    /** Crafting tables, chests, furnaces... call this. The packet to the (non-existent) client is swallowed; we hand the menu to the client tick instead. */
    @Override public OptionalInt openMenu(MenuProvider provider) {
        OptionalInt r = super.openMenu(provider);
        if (r.isPresent() && containerMenu != inventoryMenu) pendingMenu = new PendingMenu(containerMenu, provider.getDisplayName());
        return r;
    }

    @Override public void tick() {
        if (isDeadOrDying()) {                       // no death screen for P2: respawn next to the host after a short delay
            super.tick();
            if (deathTime >= 20 && ++respawnDelay > 40) revive();
            return;
        }
        respawnDelay = 0;
        Pad2Input in = input;
        // --- look
        setYRot(getYRot() + in.lookX() * 6f);
        setXRot(Math.max(-90f, Math.min(90f, getXRot() + in.lookY() * 6f)));
        setYHeadRot(getYRot());
        // --- hotbar
        if (in.hotbarDelta() != 0) getInventory().setSelectedSlot(Math.floorMod(getInventory().getSelectedSlot() + in.hotbarDelta(), 9));
        // --- act (uses the true look direction, so before the movement yaw trick below)
        handleAttack(in.attack());
        if (in.use() && !wasUse) handleUse();
        wasUse = in.use();

        // --- analog movement: players only take digital Input, so walk "forward" along the stick direction by
        // temporarily rotating yaw, and scale speed by stick magnitude through the base speed attribute.
        float mag = Math.min(1f, (float) Math.hypot(in.moveX(), in.moveY()));
        boolean moving = mag > 0.01f;
        float savedYaw = getYRot();
        if (moving) setYRot(savedYaw + (float) Math.toDegrees(Math.atan2(in.moveX(), in.moveY())));
        setLastClientInput(new Input(moving, false, false, false, in.jump(), in.sneak(), in.sprint() && moving)); // VERIFY: Input record shape
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(BASE_SPEED * (moving ? Math.max(0.3f, mag) : 1f));

        super.tick();
        doTick();                                    // normally called by the network listener, which P2 doesn't have
        setYRot(savedYaw); setYHeadRot(savedYaw);    // undo the yaw trick before the tracker sends rotation to the client
        if (tickCount % 20 == 0) { fakeConnection.fake.outboundMessages().clear(); connection.resetPosition(); }
    }

    private void revive() {
        ServerPlayer host = splitServer.getPlayerList().getPlayer(hostId);
        if (host == null) return;
        dead = false; deathTime = 0; respawnDelay = 0;
        setHealth(getMaxHealth());
        getFoodData().setFoodLevel(20);
        setPose(Pose.STANDING);
        teleportToHost(host);
    }

    private void handleAttack(boolean held) {
        HitResult hit = pick(4.5, 1f, false);
        if (!held) { breakPos = null; breakProgress = 0; return; }
        if (hit instanceof EntityHitResult e) { if (getAttackStrengthScale(0.5f) >= 1f) { attack(e.getEntity()); swing(InteractionHand.MAIN_HAND); resetAttackStrengthTicker(); } return; }
        if (hit instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
            var level = (ServerLevel) level();
            var state = level.getBlockState(b.getBlockPos());
            if (!b.getBlockPos().equals(breakPos)) { breakPos = b.getBlockPos(); breakProgress = 0; }
            breakProgress += state.getDestroyProgress(this, level, breakPos);
            if (isCreative() || breakProgress >= 1f) { gameMode.destroyBlock(breakPos); breakPos = null; breakProgress = 0; }
            swing(InteractionHand.MAIN_HAND);
        }
    }

    private void handleUse() {
        var level = (ServerLevel) level();
        ItemStack stack = getItemInHand(InteractionHand.MAIN_HAND);
        HitResult hit = pick(4.5, 1f, false);
        if (hit instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
            if (gameMode.useItemOn(this, level, stack, InteractionHand.MAIN_HAND, b).consumesAction()) { swing(InteractionHand.MAIN_HAND); return; }
        }
        gameMode.useItem(this, level, stack, InteractionHand.MAIN_HAND);
    }
}
