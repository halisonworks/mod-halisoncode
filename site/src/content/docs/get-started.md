---
title: Get started
description: Install Halison Code, spawn the robot and run your first program.
---

This takes about fifteen minutes the first time. A parent can do the install.

## 1. What you need

- Minecraft Java Edition, version 1.21.1 to 1.21.11 or 26.1 to 26.3, on a computer with Windows, macOS or Linux.
- A mod loader: [Fabric](https://fabricmc.net/use/installer/) with [Fabric API](https://modrinth.com/mod/fabric-api),
  or [NeoForge](https://neoforged.net/).
- The Halison Code `.jar` file for your loader and Minecraft version, from
  [the releases](https://github.com/halisonworks/mod-halisoncode/releases). The file name says which one it is, for
  example `halisoncode-fabric-26.3-...jar`.
- A web browser: Chrome, Edge or Firefox.

## 2. Install

1. Install Fabric (or NeoForge) for your Minecraft version, and start the new profile once so the `mods` folder is made.
2. Put the Halison Code `.jar` in the `mods` folder, and for Fabric also the Fabric API `.jar`.
3. Start Minecraft with the new profile.

The [player guide](../guide/#install) has every step, with the folder for Windows, macOS and Linux.

## 3. Spawn the robot and open the editor

1. Open a world. Press T to open chat.
2. Type `/robot spawn` and press Enter. Kintro appears at your feet.
3. Type `/halisoncode` and press Enter. The editor opens in your browser. The link works only on this computer, and
   only until you close Minecraft.

## 4. Your first program

1. In the editor, press **New Project**, and open the green **Agent** category.
2. Drag **agent move forward by 1** into the workspace and change 1 to 3.
3. Drag **agent turn left** under it and change left to right.
4. Press **Run** (the play button under the simulator). Look in the game: the robot walks 3 blocks and turns.

The same program in JavaScript:

```js
agent.move(SixDirection.Forward, 3)
agent.turn(TurnDirection.Right)
```

Each command waits until the robot is done; then the next one starts. To stop a program, press the stop button in the
editor or type `/robot stop` in the game.

## What next?

- Try the programs on the [Inspiration](../inspiration/) page, and change them.
- Read about loops, functions and stepping through a program in the [player guide](../guide/).
- Something does not work? See [Troubleshooting](../guide/#troubleshooting).
