package dev.splitcraft.input;

public enum PadKind {
    XBOX("Xbox Controller", "LB", "RB", "A", "B", "Menu"),
    SWITCH_PRO("Switch Pro Controller", "L", "R", "A", "B", "+"),
    OTHER("Controller", "L1", "R1", "A", "B", "Start");

    public final String label, left, right, confirm, back, start;
    PadKind(String label, String left, String right, String confirm, String back, String start) {
        this.label = label; this.left = left; this.right = right; this.confirm = confirm; this.back = back; this.start = start;
    }
}
