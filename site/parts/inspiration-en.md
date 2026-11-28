---
title: Inspiration
description: Eight small programs from the editor's home screen to try and change, with an idea for each.
---

These are the examples on the editor's home screen. To open one, type `/halisoncode` in the game, click it under
**Examples**, and press **Open Example**. You get a project of your own: press Run, then change it as you like.

:::note[Before you start]
Most examples give the robot its blocks with `agent.setItem`. That needs creative mode, or the game rule
`halisoncode:free_items`. In survival, right-click the robot to put the blocks in yourself, and take out the
`agent.setItem` lines. **Two robots** also needs a robot called Miner: `/robot spawn Miner`.
:::

## The examples

<!-- examples -->

## Change it

- **Stone path**: make it a square spiral that grows each time round. Change the 2 to a variable that goes up by 1.
- **Staircase**: build it down into the ground instead, or make each step two blocks wide.
- **Tunnel**: make it longer, and put a torch every fourth block with `agent.place`.
- **Pillar at a position**: build a pillar at your own position. Use `player.position()` instead of `world(0, 64, 3)`.
- **Room and fence**: put a door in the room. Use `blocks.fill` with air for the hole.
- **Two robots**: let Miner dig while your default robot builds, and let each say when it is done.
- **Guard patrol**: let the guard turn left once instead of twice, so it walks a square.
- **Torches in the dark**: walk on until there is a wall in front, with a `while` loop and `agent.detect`.

More ideas: the [player guide](../guide/) shows loops, functions, shapes, mazes, chat commands and several robots.
