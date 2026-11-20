package dev.splitcraft.pairing;

import dev.splitcraft.config.SplitConfig;
import dev.splitcraft.input.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class PairingScreen extends Screen {
    private final Screen parent;
    private final PadScanner scanner = new PadScanner();
    private final PairingSession session = new PairingSession();
    private final java.util.function.BiConsumer<Pad, Pad> onStart;

    public PairingScreen(Screen parent, java.util.function.BiConsumer<Pad, Pad> onStart) {
        super(Component.literal("Controller Pairing"));
        this.parent = parent; this.onStart = onStart;
    }

    @Override protected void init() {
        addRenderableWidget(Button.builder(orientationLabel(), b -> {
            SplitConfig.toggleOrientation(); b.setMessage(orientationLabel());
        }).bounds(width / 2 - 100, height - 52, 200, 20).build());
        addRenderableWidget(Button.builder(renderLabel(), b -> {
            SplitConfig.toggleRenderMode(); b.setMessage(renderLabel());
        }).bounds(width / 2 - 100, height - 76, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private Component renderLabel() {
        return Component.literal("Rendering: " + (SplitConfig.renderMode() == SplitConfig.RenderMode.RESIZE ? "Full view (default)" : "Safe crop"));
    }

    private Component orientationLabel() {
        boolean h = SplitConfig.orientation() == SplitConfig.Orientation.HORIZONTAL;
        return Component.literal("Split: " + (h ? "Horizontal (top / bottom)" : "Vertical (left / right)"));
    }

    @Override public void tick() {
        try { scanner.refresh(); } catch (Throwable t) { return; }
        List<Pad> pads = scanner.pads();
        session.tick(pads);
        if (session.phase() == PairingSession.Phase.READY_TO_START) {
            for (Pad p : pads) if (p.pressedStart) { onStart.accept(session.slots[0], session.slots[1]); return; }
        }
    }

    @Override public void render(GuiGraphics g, int mx, int my, float dt) {
        super.render(g, mx, my, dt);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
        boolean horiz = SplitConfig.orientation() == SplitConfig.Orientation.HORIZONTAL;
        int cardW = horiz ? width - 80 : (width - 100) / 2, cardH = horiz ? (height - 180) / 2 : height - 160;
        for (int i = 0; i < PairingSession.SLOTS; i++) {
            int cx = horiz ? 40 : 40 + i * (cardW + 20), cy = horiz ? 32 + i * (cardH + 10) : 40;
            Pad p = session.slots[i];
            boolean ok = session.isConfirmed(p);
            g.fill(cx, cy, cx + cardW, cy + cardH, 0xAA000000);
            g.renderOutline(cx, cy, cardW, cardH, ok ? 0xFF4CD964 : p != null ? 0xFFFFD60A : 0xFF666666);
            for (int led = 0; led < 4; led++) g.fill(cx + 8 + led * 10, cy + 8, cx + 14 + led * 10, cy + 12, led == i ? 0xFF4CD964 : 0xFF333333); // Switch-style player LEDs
            g.drawString(font, "Player " + (i + 1), cx + 8, cy + 20, 0xFFFFFFFF);
            String l1, l2;
            if (p == null) { l1 = "Press L + R on a controller"; l2 = "(LB + RB on Xbox)"; }
            else { l1 = p.kind.label; l2 = ok ? "Ready  -  " + p.kind.back + " to undo" : "Press " + p.kind.confirm + " to confirm"; }
            g.drawString(font, l1, cx + 8, cy + 36, 0xFFDDDDDD);
            g.drawString(font, l2, cx + 8, cy + 48, ok ? 0xFF4CD964 : 0xFFAAAAAA);
        }
        boolean go = session.phase() == PairingSession.Phase.READY_TO_START;
        g.drawCenteredString(font, "Detected: " + scanner.pads().size() + " controller(s)   |   X swaps player order", width / 2, height - 106, 0xFF888888);
        g.drawCenteredString(font, go ? "Press + / Menu to start" : "Both players must join and confirm", width / 2, height - 94, go ? 0xFF4CD964 : 0xFFAAAAAA);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
}
