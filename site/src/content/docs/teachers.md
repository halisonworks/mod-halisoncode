---
title: For teachers
description: Use Halison Code with a class or a coding club. What each pupil needs, how to set up a shared world, and ideas for lessons.
---

Halison Code lets children program in a game many of them already play. They see each block they run happen in the
world, and a mistake shows at once: the robot walks into a wall, or the tower has a hole in it.

## What each pupil needs

- A computer with Minecraft Java Edition (each pupil needs their own Minecraft account), the mod loader and the
  Halison Code `.jar`. See [Get started](../get-started/).
- A web browser. The editor runs on the pupil's own computer and talks only to their own game: there are no accounts,
  and projects are kept in that browser.

Pupils can share a computer. The robot belongs to whoever is logged in to Minecraft, but the projects are in the
browser: give each pupil a browser profile of their own, or they see each other's projects.

## One world for the whole class

Each pupil can play in their own singleplayer world. To build together, run a server with Halison Code:

1. Set up a Fabric or NeoForge server with the same Halison Code version as the pupils' games. The
   [server guide](../servers/) has the steps.
2. Decide what pupils may do with game rules (see [Commands and game rules](../commands/)):
   - `halisoncode:free_items`: robots get blocks free, so pupils can build without gathering first.
   - `halisoncode:max_robots`: how many robots each pupil may have (1 to 16).
   - `halisoncode:robot_camera`: who may look through a robot (off, owner, ops).
3. Each robot belongs to one pupil. Nobody else can program it or take its items. A teacher who is an op can stop every
   robot with `/robot stop all`.
4. Robots follow spawn protection, game modes and claim mods, as if they were their owner. In adventure mode they place
   and break nothing.

## Ideas for lessons

Each idea builds on the one before. Start with blocks; pupils who are ready can switch the same project to JavaScript
or Python.

| Lesson | Idea | Coding |
|---|---|---|
| 1 | Walk the robot to a tree and back | commands one after another |
| 2 | A stone path, a staircase | repeat loops |
| 3 | A tunnel: dig when there is a block in the way, else walk | while loops and if |
| 4 | Torches in the dark | sensors (light level) and conditions |
| 5 | A room with a door, a fence round a field | functions of your own |
| 6 | Two robots that build at the same time | several robots, parallel work |
| 7 | A guard that patrols for ever, and a chat command that sends it home | forever, events |

The programs on the [Inspiration](../inspiration/) page fit lessons 2 to 7, and each has an idea for changing it.

## Good to know

- The editor shows each command in the robot log, and the robot says over its head why it stopped (out of blocks, not
  on the path). The debugger can run a program one block at a time.
- The editor, the guides and the game's messages are in English and Norwegian.
- Halison Code is a pre-release. Update the server and every pupil's game to the same version at the same time.
- Problems and ideas: [report them on GitHub](https://github.com/halisonworks/mod-halisoncode/issues/new/choose).
