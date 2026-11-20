package dev.splitcraft.input;

import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.GamepadInputs;

/**
 * One physical controller. Wraps Controlify's ControllerEntity so the rest of the mod never touches
 * Controlify types directly (if Controlify changes, only this file + PadScanner need fixing).
 */
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
        // SDL's gamepad layout is position-based (SOUTH = bottom face button). On a Switch Pro the bottom
        // button is printed "B", so for Switch pads we swap A/B and X/Y to match the labels on the pad.
        boolean south = s.isButtonDown(GamepadInputs.SOUTH_BUTTON);
        boolean east  = s.isButtonDown(GamepadInputs.EAST_BUTTON);
        boolean north = s.isButtonDown(GamepadInputs.NORTH_BUTTON);
        boolean west  = s.isButtonDown(GamepadInputs.WEST_BUTTON);
        boolean confirm = kind == PadKind.SWITCH_PRO ? east : south;   // "A"
        boolean back    = kind == PadKind.SWITCH_PRO ? south : east;   // "B"
        boolean swap    = kind == PadKind.SWITCH_PRO ? north : west;   // "X" on Switch (top), "X" (left) on Xbox
        boolean start = s.isButtonDown(GamepadInputs.START_BUTTON);
        pressedLR = lr && !prevLR; pressedA = confirm && !prevA; pressedB = back && !prevB;
        pressedY = swap && !prevY; pressedStart = start && !prevStart;
        prevLR = lr; prevA = confirm; prevB = back; prevY = swap; prevStart = start;
    }
}
