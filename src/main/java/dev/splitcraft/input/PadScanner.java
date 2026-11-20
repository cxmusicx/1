package dev.splitcraft.input;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ControllerEntity;
import java.util.*;

/** Finds connected gamepads through Controlify's SDL3 controller manager. */
public final class PadScanner {
    private final Map<String, Pad> pads = new LinkedHashMap<>();

    public List<Pad> pads() { return List.copyOf(pads.values()); }

    /** Call every client tick while the pairing screen is open (or in-game). Handles hot-plug. */
    public void refresh() {
        Set<String> seen = new HashSet<>();
        List<ControllerEntity> connected = Controlify.instance().getControllerManager().orElseThrow().getConnectedControllers();
        for (ControllerEntity e : connected) {
            seen.add(e.uid());
            pads.computeIfAbsent(e.uid(), k -> new Pad(e, classify(e)));
        }
        pads.keySet().removeIf(k -> !seen.contains(k));
        pads.values().forEach(Pad::poll);
    }

    static PadKind classify(ControllerEntity e) {
        // Controlify's registered namespace is the primary identifier. Include the info's
        // display representation as a fallback because some Nintendo pads are registered
        // under a generic SDL/gamepad namespace rather than "switch".
        String namespace = e.info().type().namespace().toString().toLowerCase(Locale.ROOT);
        String details = String.valueOf(e.info()).toLowerCase(Locale.ROOT);
        String id = namespace + " " + details;
        if (id.contains("xbox")) return PadKind.XBOX;
        if (id.contains("switch") || id.contains("nintendo") || id.contains("joy-con") || id.contains("joycon")) {
            return PadKind.SWITCH_PRO;
        }
        return PadKind.OTHER;
    }
}
