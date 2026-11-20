package dev.splitcraft.input;

import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.GamepadInputs;

/** One physical controller, with pairing buttons mapped to the labels printed on the pad. */
public final class Pad {
    public final ControllerEntity entity;
    public final PadKind kind;
    private boolean prevLR, prevA, prevB, prevStart, prevY;
    public boolean pressedLR, pressedA, pressedB, pressedStart, pressedY;

    public Pad(ControllerEntity entity, PadKind kind) { this.entity = entity; this.kind = kind; }
    public String id() { return entity.uid(); }

    /** Call once per client tick. Computes edge-triggered presses. */
    public void poll() {
        ControllerStateView s = entity.input().map(i -> (ControllerStateView) i.stateNow()).orElse(null);
        if (s == null) return;
        boolean lr = s.isButtonDown(GamepadInputs.LEFT_SHOULDER_BUTTON) && s.isButtonDown(GamepadInputs.RIGHT_SHOULDER_BUTTON);
        boolean south = s.isButtonDown(GamepadInputs.SOUTH_BUTTON);
        boolean east  = s.isButtonDown(GamepadInputs.EAST_BUTTON);
        boolean north = s.isButtonDown(GamepadInputs.NORTH_BUTTON);
        boolean west  = s.isButtonDown(GamepadInputs.WEST_BUTTON);
        // During pairing, the Switch Pro's printed B (bottom) confirms and A (right) backs out.
        boolean confirm = kind == PadKind.SWITCH_PRO ? south : south;
        boolean back    = kind == PadKind.SWITCH_PRO ? east : east;
        boolean swap    = kind == PadKind.SWITCH_PRO ? north : west;
        boolean start = s.isButtonDown(GamepadInputs.START_BUTTON);
        pressedLR = lr && !prevLR; pressedA = confirm && !prevA; pressedB = back && !prevB;
        pressedY = swap && !prevY; pressedStart = start && !prevStart;
        prevLR = lr; prevA = confirm; prevB = back; prevY = swap; prevStart = start;
    }
}
