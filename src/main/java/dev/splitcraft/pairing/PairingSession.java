package dev.splitcraft.pairing;

import dev.splitcraft.input.Pad;
import java.util.*;

/**
 * Switch-style pairing: a pad joins by pressing L+R (LB+RB on Xbox), takes the next player slot,
 * confirms with A, undoes/leaves with B, X swaps player order. Start unlocks once both are confirmed.
 */
public final class PairingSession {
    public enum Phase { WAITING, READY_TO_START }
    public static final int SLOTS = 2;

    public final Pad[] slots = new Pad[SLOTS];
    public final Set<String> confirmed = new HashSet<>();

    public void tick(List<Pad> all) {
        for (int i = 0; i < SLOTS; i++) {
            if (slots[i] != null && !all.contains(slots[i])) { confirmed.remove(slots[i].id()); slots[i] = null; } // unplugged
        }
        for (Pad p : all) {
            int slot = slotOf(p);
            if (slot < 0) {
                if (p.pressedLR) { int free = firstFree(); if (free >= 0) slots[free] = p; }
                continue;
            }
            if (p.pressedA) confirmed.add(p.id());
            if (p.pressedB) { if (!confirmed.remove(p.id())) slots[slot] = null; } // un-confirm first, then leave
            if (p.pressedY && slots[SLOTS - 1 - slot] != null) {
                Pad t = slots[slot]; slots[slot] = slots[SLOTS - 1 - slot]; slots[SLOTS - 1 - slot] = t;
            }
        }
    }

    public int slotOf(Pad p) { for (int i = 0; i < SLOTS; i++) if (slots[i] == p) return i; return -1; }
    private int firstFree() { for (int i = 0; i < SLOTS; i++) if (slots[i] == null) return i; return -1; }
    public boolean isConfirmed(Pad p) { return p != null && confirmed.contains(p.id()); }
    public Phase phase() {
        return slots[0] != null && slots[1] != null && isConfirmed(slots[0]) && isConfirmed(slots[1]) ? Phase.READY_TO_START : Phase.WAITING;
    }
}
