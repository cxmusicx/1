package dev.splitcraft.input;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ControllerEntity;
import java.util.*;

/** Finds connected Xbox / Switch Pro pads via Controlify's SDL3 controller manager. */
public final class PadScanner {
    private final Map<String, Pad> pads = new LinkedHashMap<>();

    public List<Pad> pads() { return List.copyOf(pads.values()); }

    /** Call every client tick while the pairing screen is open (or in-game). Handles hot-plug. */
    public void refresh() {
        Set<String> seen = new HashSet<>();
        // Controlify exposes getControllerManager() (checked in the jar). If it returns Optional vs plain, adjust here.
        List<ControllerEntity> connected = Controlify.instance().getControllerManager().orElseThrow().getConnectedControllers();
        for (ControllerEntity e : connected) {
            seen.add(e.uid());
            pads.computeIfAbsent(e.uid(), k -> new Pad(e, classify(e)));
        }
        pads.keySet().removeIf(k -> !seen.contains(k));
        pads.values().forEach(Pad::poll);
    }

    static PadKind classify(ControllerEntity e) {
        // Controlify resolves vendor/product IDs to a namespace (controller_identification.json5):
        // "controlify:xbox_one", "controlify:switch", ...
        String ns = e.info().type().namespace().toString();
        if (ns.contains("xbox")) return PadKind.XBOX;
        if (ns.contains("switch")) return PadKind.SWITCH_PRO;
        return PadKind.OTHER;
    }
}
