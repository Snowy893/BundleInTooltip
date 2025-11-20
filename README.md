# Bundle In Tooltip (Forge 1.20.1)

Backports the Minecraft 1.21 bundle tooltip interaction to Forge 1.20.1 so you can peek inside any bundle, scroll through its contents, and pull out the exact stack you want without dumping everything on the floor first.

<p align="center">
  <img src="assets/BundleInTooltip.gif" width="50%">
</p>

## Features
- Works with vanilla bundles, dyed bundles from **Bundle In Palette**, and any other item in the `forge:bundles` tag.
- Scroll on a bundle tooltip to highlight the stack you plan to grab.
- Right-click (while staying hovered over the bundle slot) to withdraw the highlighted stack directly onto your cursor.
- Plays nicely in both singleplayer and multiplayer thanks to a lightweight networking sync.

## Controls & Flow
1. Hover your mouse over a bundle slot inside any inventory screen.
2. The tooltip opens, showing the contents grid.
3. **Mouse Wheel:** cycle through stacks in the tooltip; the highlighted slot follows your scroll direction.
4. **Right-Click:** while still hovering the original bundle slot, immediately pull the highlighted stack onto your cursor.
5. **Left-Click/Drag:** behaves like vanilla, letting you move the bundle itself even when it contains items.

Tips:
- You can scroll even while the tooltip is partially off-screen; the last valid slot remains selected.
- The interaction ignores empty bundles automatically, so you won’t accidentally pull “air.”

## Configuration
`bundleintooltip-client.toml` (created after first launch) exposes these client-side options:

- `tooltip.showCapacityBar` — switch between the modern 1.21-style capacity bar and the classic numeric fullness line (`24/64`). Set it to `true` if you prefer the animated bar, or leave it `false` to keep the numbers.
- `tooltip.capacityLabelMode` — controls what text appears inside the bar when it’s visible (defaults to `VANILLA`):
  - `NONE`: leave the bar blank, just like the 1.21 snapshots.
  - `VANILLA`: show the “Empty” / “Full” labels when appropriate.
  - `CLASSIC`: show the classic `24/64` fullness number centered inside the bar.
  - `HYBRID`: mixes both styles (Empty/Full labels at the extremes, numeric in between).
  - `VANILLA_REVISED`: Empty / Filling / Full labels inspired by the preview UI.
- `tooltip.slotTextureMode` — switch between the modern 1.21 slot sprites (`VANILLA`, default) and the older 1.20.1 grid (`CLASSIC`).

## Requirements
- Minecraft **1.20.1**
- Forge **47.3.0** or newer in the 1.20.1 line

## Installation
1. Install the matching Forge build.
2. Drop the released jar into your `mods/` directory.
3. Launch the game; “Bundle In Tooltip” should appear on the Mods screen.

## Credits
Created by **dmor-me**. Inspired by the official 1.21 bundle UX and designed to complement the dyed bundles from **Bundle In Palette**.
<p align="center">
  <img src="assets/BundleInTooltip.png" width="10%">
</p>
