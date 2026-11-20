# Splitcraft (WIP, uncompiled)
Standalone Fabric mod (MC 1.21.11) that depends on Controlify only for controller access/detection.

## How it works
- Pause menu -> "Split Screen" (local worlds only) -> Switch-style pairing screen -> start.
- Player 2 = a server-side `Player2 extends ServerPlayer` with a fake connection (Carpet-style), driven each tick from pad 2.
- Rendering: the frame is drawn twice into two half-size TextureTargets (window size faked via WindowMixin), then copied into the window target. Horizontal/vertical toggle on the pairing screen.
- Input: Controlify keeps driving P1; ControlifyMixin pins its current controller to P1's pad. Pad 2 is read directly.

## Pad 2 controls
Left stick move, right stick look, A jump, RT attack/mine, LT use/place, LB/RB hotbar, L3 sprint, R3 sneak.

## Pad 2 extras
Y (Switch: Y) opens Player 2's inventory/2x2 crafting; that pad temporarily becomes Controlify's active pad.

## Build (GitHub Actions)
Push to GitHub (keep `libs/controlify-*.jar` committed). Set `fabric_version` in gradle.properties first.
Every push builds; the jar is under Actions > run > Artifacts. Push a tag like `v0.1.0` to also publish a Release.

## Known gaps
- P2 can't use crafting tables/chests/furnaces (inventory + 2x2 crafting only)
- Hand animation (swing/bob) in P2's view still follows P1; the held item is correct
- Written blind against 1.21.11 Mojang names: expect compile fixes; the Actions log shows them
