# Capability matrix: robot-dependent and robot-free operations

Issue #361 (parent #350, epic #344). Research only: this document changes no code.

It lists every operation the editor exposes and answers, for each one: who executes it on the server, whether it
needs a spawned robot today, whether that need is essential or incidental, which checks and limits apply, what is
saved, where the code is and which test covers it.

Base: `develop` at 58aef43 (release 0.0.7), read on 2026-11-27. Line numbers refer to that commit.

## Terms

- **Essential**: the operation acts at the robot's position or facing, or on the robot's inventory or state. Without
  a robot it has no meaning.
- **Incidental**: the operation only goes through the robot. Its target is given in world coordinates (or relative
  to the player), and the robot only supplies plumbing: a queue, a FakePlayer, an item source, or a range anchor.
  It could run without a robot once those parts have another owner.
- **Simulator only**: the editor answers the call itself and sends nothing to the game.

## Files

Short names in the tables refer to these files.

| Short name | Path |
|---|---|
| `api.ts` | `target/sim/api.ts` |
| `sim.ts` | `target/sim/simulator.ts` |
| `ns.ts` | `target/libs/core/ns.ts` (`sims.d.ts` is generated from `api.ts` and has the same calls) |
| `RobotCommand.java` | `core/src/main/java/halisoncode/robot/RobotCommand.java` |
| `Positions.java` | `core/src/main/java/halisoncode/robot/Positions.java` |
| `Protocol.java` | `core/src/main/java/halisoncode/net/Protocol.java` |
| `Limits.java` | `core/src/main/java/halisoncode/Limits.java` |
| `ServerRelay.java`, `PlayerApi.java`, `PlayerEvents.java` | `common/src/main/java/halisoncode/net/` |
| `RobotManager.java`, `RobotQueue.java`, `RobotEntity.java`, `RobotCamera.java`, `RobotLinks.java`, `RobotCrafting.java`, `RobotCommands.java` | `common/src/main/java/halisoncode/robot/` |
| `Permission.java`, `HalisonCode.java` | `common/src/main/java/halisoncode/` |
| `RobotGameTests.java`, `RelayGameTests.java`, `MultiRobotGameTests.java`, `SavedRobotGameTests.java` | `fabric/src/gametest/java/halisoncode/gametest/` |
| `RobotCommandTest.java` | `fabric/src/test/java/halisoncode/robot/` |
| `ProtocolTest.java`, `ChatCommandsTest.java` | `core/src/test/java/halisoncode/net/` |
| `PositionsTest.java`, `RobotShapesTest.java` | `core/src/test/java/halisoncode/robot/` |
| `SignalsTest.java` | `core/src/test/java/halisoncode/api/` |
| `editor-suite.mjs` | `e2e/editor-suite.mjs` |
| `blocks.mjs`, `examples.mjs` | `e2e/programs/` |

## How a call reaches the server

The simulator sends one `cmd` message per call with an `op` string and its arguments (`sim.ts:236`). A call on a
named robot adds a `robot` field (`sim.ts:241`). The game server parses and validates the op in
`RobotCommand.parse` (`RobotCommand.java:189`), then `ServerRelay.handle` routes it (`ServerRelay.java:113`):

1. `positions.toWorld` is answered at once against the player (`ServerRelay.java:128`, `ServerRelay.java:162`).
2. The player ops (`player.*`, `gameplay.*`, `signals.send`) go to `PlayerApi` (`ServerRelay.java:128`,
   `PlayerApi.java:47`). No robot and no `halisoncode.robot` permission is needed.
3. Everything else needs the permission `halisoncode.robot` (`ServerRelay.java:130`). Building ops also need
   `halisoncode.build` (`ServerRelay.java:131`).
4. `agent.camera` is answered at once by `RobotCamera` (`ServerRelay.java:132`, `ServerRelay.java:178`).
5. The rest goes to the queue of the named robot, or the default robot (`ServerRelay.java:133`,
   `RobotManager.java:341`).

Event handlers (`onChat`, `onDied`, `onBlockBroken`, `onRedstonePowered`, `onDayStart`, `signals.onReceived`) send no
`cmd`. The simulator sends one `control subscribe` message with everything the program listens for (`sim.ts:77`,
`sim.ts:246`). The server keeps it in `PlayerEvents` (`ServerRelay.java:202`, `PlayerEvents.java:60`).

Starting a run (`control start`) needs no robot: the run id lives on a queue that is created on demand
(`ServerRelay.java:192`, `RobotManager.java:216`).

### Shared checks, used in the tables

**P: player path** (`PlayerApi`).
- The run must be the current one, else `STOPPED` (`PlayerApi.java:52`).
- At most `Limits.queueLength` calls wait per player (default 16), else `RATE_LIMITED` (`PlayerApi.java:55`,
  `Limits.java:37`).
- One call per player per tick (`PlayerApi.java:67`).
- The player must be online when the call runs, else `UNLOADED` (`PlayerApi.java:60`).
- Game mode does not matter (`PlayerApi.java:29`).
- Nothing is saved.

**R: robot path** (`RobotManager`).
- Permission `halisoncode.robot` (`ServerRelay.java:130`, `Permission.java:14`). By default everyone has it.
- The robot must be spawned and loaded, else `UNLOADED` (`RobotManager.java:342`). The robot is looked up only among
  the sender's own robots (`ServerRelay.java:127`).
- The run must be the current one, else `STOPPED`; at most `Limits.queueLength` commands wait per robot (default 16),
  else `RATE_LIMITED` (`RobotQueue.java:124`).
- Executed on the tick under the shared budgets `actionsPerTick`, `millisPerTick`, `scanBlocksPerTick`,
  `pathNodesPerTick` and `buildBlocksPerTick`, split fairly between players (`RobotManager.java:539`,
  `Limits.java:23`).
- An unloaded robot drops its queue with `UNLOADED` (`RobotManager.java:134`). A player who leaves stops all their
  robots (`ServerRelay.java:96`).

**F: FakePlayer** (`RobotManager.java:1359`). Edits run as a FakePlayer with the owner's UUID, so claim and
protection mods see the owner. `Level.mayInteract` applies (spawn protection, world border). A spectator owner's
robot does nothing. An adventure owner's robot may use blocks but not build. Tests: `RobotGameTests.java:626`,
`:640`, `:652`, `:665`, `:679`, `:692`.

**N: 32-block rule** (`RobotManager.java:817`). Positions given in world coordinates must be within 32 blocks of the
robot on each axis, else `OUT_OF_RANGE`.

**Free items** (`Permission.java:18`, `RobotManager.java:1658`). The owner is in creative mode, or the game rule
`halisoncode:free_items` is on (`HalisonCode.java:66`), or a permission mod grants `halisoncode.free_items`. An
offline owner uses the game rule only.

**E: events** (`PlayerEvents`). At most 16 chat keywords of at most 32 characters, 16 block kinds and 16 signal names
per program (`Protocol.java:114`). At most 16 events wait, the rest are dropped (`PlayerEvents.java:133`). One event per
player per tick (`PlayerEvents.java:166`). Listening ends with the run (`PlayerEvents.java:82`).

**Robot state saved**: inventory, selected slot, place on move, gravity, home and look are saved in the robot
entity's NBT (`RobotEntity.java:239`). Which entity is which owner's robot is saved in `RobotLinks`, a SavedData file
(`RobotLinks.java:36`).

**Editor round trip**: every block in the toolbox has a program in `blocks.mjs` that `editor-suite.mjs:104` turns
from JavaScript to Blocks to Python and back. This covers the declaration of every block below. The test column only
lists tests of behaviour.

## Player

| Call | Op | Actor | Robot today | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|---|
| `player.say` | `player.say` | Player (PlayerApi) | No | P. Plain text, cut to 256 characters, control characters removed (`RobotCommand.java:486`). Only the owner sees it. | Nothing | `api.ts:1354`, `RobotCommand.java:272`, `PlayerApi.java:86` | `RelayGameTests.java:252`, `RobotCommandTest.java:198`, `editor-suite.mjs:251` |
| `player.position` | `player.position` | Player (PlayerApi) | No | P | Nothing | `api.ts:1363`, `RobotCommand.java:277`, `PlayerApi.java:91` | `RelayGameTests.java:252`, `editor-suite.mjs:251` |
| `player.getOrientation` | `player.getOrientation` | Player (PlayerApi) | No | P | Nothing | `api.ts:1373`, `RobotCommand.java:277`, `PlayerApi.java:97` | `RelayGameTests.java:252`, `RelayGameTests.java:275` |
| `player.name` | `player.name` | Player (PlayerApi) | No | P | Nothing | `api.ts:1382`, `RobotCommand.java:277`, `PlayerApi.java:98` | `RelayGameTests.java:252`, `RobotCommandTest.java:208` |
| `player.teleport` | `player.teleport` (after `positions.toWorld` for `~` and `^`) | Player (PlayerApi) | No | P. Permission `halisoncode.teleport`: by default the singleplayer host or an operator (`Permission.java:20`), else `NO_PERMISSION` (`PlayerApi.java:127`). Own dimension, inside the world border and build height, else `OUT_OF_RANGE` (`PlayerApi.java:131`). | Nothing | `api.ts:1393`, `RobotCommand.java:292`, `PlayerApi.java:125` | `RelayGameTests.java:299`, `:416`, `:441` |
| `player.onChat` | none (`control subscribe`, event `chat`) | Player (PlayerEvents) | No | E. The matching chat line is held back from the chat (`PlayerEvents.java:97`). | Nothing | `api.ts:1412`, `sim.ts:46` | `RelayGameTests.java:527`, `:546`, `:605`, `ChatCommandsTest.java:15`, `editor-suite.mjs:341` |
| `player.onDied` | none (`control subscribe`, event `playerDied`) | Player (PlayerEvents) | No | E | Nothing | `api.ts:1422`, `sim.ts:51`, `PlayerEvents.java:107` | `RelayGameTests.java:591`, `editor-suite.mjs:341` |
| `player.runChatCommand` | none | Simulator only | No | Runs the program's own chat handlers; sends nothing. | Nothing | `api.ts:1433`, `sim.ts:96` | `editor-suite.mjs:341` |

## Gameplay

| Call | Op | Actor | Robot today | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|---|
| `gameplay.timeOfDay` | `gameplay.timeOfDay` | Player (PlayerApi) | No | P. The Overworld clock in every dimension. | Nothing | `api.ts:1446`, `RobotCommand.java:281`, `PlayerApi.java:101` | `RelayGameTests.java:472` |
| `gameplay.isRaining` | `gameplay.isRaining` | Player (PlayerApi) | No | P. The owner's dimension. | Nothing | `api.ts:1455`, `RobotCommand.java:281`, `PlayerApi.java:103` | `RelayGameTests.java:472` |
| `gameplay.onDayStart` | none (`control subscribe`, event `dayStart`) | Player (PlayerEvents, owner's level time) | No | E | Nothing | `api.ts:1466`, `sim.ts:66`, `PlayerEvents.java:153` | `RelayGameTests.java:472`, `ChatCommandsTest.java:73`, `editor-suite.mjs:341` |

## Positions

All positions are built in the editor. Only turning a relative (`~`) or local (`^`) position into a world position
asks the game, and it asks about the player, never the robot: `pos` is relative to the player's feet and `posLocal`
also turns with the player's facing (`ServerRelay.java:166`).

| Call | Op | Actor | Robot today | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|---|
| `pos` / `positions.create` | none | Simulator only | No | None | Nothing | `ns.ts:181`, `api.ts:112` | Round trip only |
| `posLocal` / `positions.createLocal` | none | Simulator only | No | None | Nothing | `ns.ts:193`, `api.ts:120` | Round trip only |
| `world` / `positions.createWorld` | none | Simulator only | No | None | Nothing | `ns.ts:205`, `api.ts:128` | Round trip only |
| `positions.random` / `randpos` | none | Simulator only | No | None | Nothing | `api.ts:139`, `ns.ts:212` | Round trip only |
| `Position.getValue` | none | Simulator only | No | None | Nothing | `api.ts:22` | Round trip only |
| `Position.add` | none | Simulator only | No | None | Nothing | `api.ts:43` | Round trip only |
| `Position.equals` | none | Simulator only | No | None | Nothing | `api.ts:53` | Round trip only |
| `Position.toString` | none | Simulator only | No | None | Nothing | `api.ts:62` | `editor-suite.mjs:230` |
| `Position.subtract` | none | Simulator only | No | None | Nothing | `api.ts:89` | `editor-suite.mjs:230` |
| `Position.scale` | none | Simulator only | No | None | Nothing | `api.ts:99` | `editor-suite.mjs:230` |
| `Position.toWorld` | `positions.toWorld` (none for a world position) | Player (ServerRelay, answered at once, not queued) | No | Run must be current, else `STOPPED`. Kind 1 or 2, coordinates within 30,000,000 (`RobotCommand.java:447`, `Positions.java:13`). Only the relay's messages per tick limit applies (`ServerRelay.java:57`). | Nothing | `api.ts:31`, `ServerRelay.java:162`, `Positions.java:34` | `RelayGameTests.java:171`, `PositionsTest.java:10`, `:16`, `editor-suite.mjs:230` |
| `Position.distanceTo` | `positions.toWorld` only when the two kinds differ | Simulator, plus Player for the conversion | No | As `toWorld` | Nothing | `api.ts:74` | `editor-suite.mjs:230` |
| `positions.sortByDistance` | as `distanceTo` | Simulator, plus Player for the conversion | No | As `toWorld` | Nothing | `api.ts:163` | `editor-suite.mjs:230` |
| `positions.nearest` | as `distanceTo` | Simulator, plus Player for the conversion | No | As `toWorld` | Nothing | `api.ts:176` | `editor-suite.mjs:230` |

## Signals

| Call | Op | Actor | Robot today | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|---|
| `signals.onReceived` | none (`control subscribe`, event `signal`) | Player (PlayerEvents). Sent by other mods, or by an operator with `/halisoncode signal` (op level 2, `RobotCommands.java:104`). | No | E. Name rules as `signals.send`. | Nothing | `api.ts:1484`, `sim.ts:71`, `PlayerEvents.java:128` | `RelayGameTests.java:743`, `:772`, `:788`, `:803`, `SignalsTest.java:30`, `editor-suite.mjs:341` |
| `signals.send` | `signals.send` | Player (PlayerApi) | No | P. Name rules (`RobotCommand.java:285`). The same name at most once a second per player (`PlayerApi.java:111`). Goes only to other mods' listeners, never to a program. | Only in memory: the tick each name was last sent, kept across runs until the player leaves (`PlayerApi.java:38`) | `api.ts:1498`, `PlayerApi.java:104` | `RelayGameTests.java:888`, `:904`, `:932`, `editor-suite.mjs:341` |

## Blocks

All building goes through the default robot: `blocks.*` and `shapes.*` call `agent.call` without a robot name, and
`Robot` has no building methods, so a named robot never builds (`api.ts:285`, `api.ts:1536`).

| Call | Op | Actor | Robot today | Dependency | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|---|---|
| `blocks.fill` | `blocks.fill` | Default robot (queue, `RobotManager.build`, F) | Yes, the default robot | Incidental | R, plus `halisoncode.build` (`ServerRelay.java:131`, `Permission.java:16`). At most 32768 blocks (`RobotCommand.java:459`). N for both corners (`RobotManager.java:907`). F for every block. Blocks from the robot's inventory unless free items, else `NO_ITEMS` and the robot says why (`RobotManager.java:948`, `:956`). Blocks in the way are broken and the drops go into the robot (`RobotManager.java:944`, `:1410`). At most `buildBlocksPerTick` (256) changes per tick, shared. | The robot's inventory | `api.ts:1533`, `RobotCommand.java:408`, `RobotManager.java:639`, `:901` | `RobotGameTests.java:1957`, `:1974`, `:2005`, `:2067`, `:2085`, `:2109`, `:2147`, `RelayGameTests.java:371`, `:388`, `RobotCommandTest.java:315`, `RobotShapesTest.java:27`, `examples.mjs:41` |
| `blocks.place` | `blocks.fill` (one block, Replace) | As `blocks.fill` | Yes, the default robot | Incidental | As `blocks.fill` | The robot's inventory | `api.ts:1546` | Through the one-block fills in `RelayGameTests.java:371`, `:388` |
| `blocks.onBlockBroken` | none (`control subscribe`, event `blockBroken`) | Player (PlayerEvents). Only blocks the owner breaks by hand, never the robot's FakePlayer. | No | n/a | E | Nothing | `api.ts:1559`, `sim.ts:56`, `PlayerEvents.java:117` | `RelayGameTests.java:642`, `:666`, `:682`, `editor-suite.mjs:341` |

`blocks.item` and `blocks.block` (`ns.ts:249`, `ns.ts:258`) are hidden value helpers that return their argument.
They are not counted.

## Shapes

All four use the same executor and checks as `blocks.fill` (R, `halisoncode.build`, N, F, free items or
`NO_ITEMS`, build budget), and all four are incidental.

| Call | Op | Actor | Robot today | Extra checks | Saved | Source | Test |
|---|---|---|---|---|---|---|---|
| `shapes.line` | `shapes.line` | Default robot | Yes | None | The robot's inventory | `api.ts:1573`, `RobotCommand.java:408` | `RobotGameTests.java:2131`, `RobotShapesTest.java:54` |
| `shapes.circle` | `shapes.circle` | Default robot | Yes | Radius 1 to 32, axis 0 to 2 (`RobotCommand.java:417`) | The robot's inventory | `api.ts:1590`, `RobotCommand.java:417` | No GameTest. Parsing: `RobotCommandTest.java:315`, `ProtocolTest.java:143`. Shape: `RobotShapesTest.java:36` |
| `shapes.sphere` | `shapes.sphere` | Default robot | Yes | Radius 1 to 32, but above 15 the volume cap refuses it | The robot's inventory | `api.ts:1605`, `RobotCommand.java:417` | `RobotGameTests.java:2131`, `RobotShapesTest.java:47` |
| `shapes.maze` | `shapes.maze` | Default robot | Yes | Width and length 5 to 32, walls and floor not air, the server picks a seed when there is none (`RobotCommand.java:428`) | The robot's inventory | `api.ts:1625`, `RobotCommand.java:428` | `RobotGameTests.java:2173`, `:2240`, `:2245`, `:2251`, `RobotShapesTest.java:68` |

## Builder

The builder lives in the editor (`sim.ts:27`). It starts at the player's feet: its first use asks
`positions.toWorld` for `pos(0, 0, 0)` (`api.ts:1638`). Only what it builds goes to the game.

| Call | Op | Actor | Robot today | Dependency | Source | Test |
|---|---|---|---|---|---|---|
| `builder.teleportTo` | `positions.toWorld` for `~` and `^` | Simulator, plus Player for the conversion | No | n/a | `api.ts:1651` | Round trip only |
| `builder.move` | `positions.toWorld` on first use | Simulator, plus Player | No | n/a | `api.ts:1663` | `examples.mjs:41` |
| `builder.turn` | none | Simulator only | No | n/a | `api.ts:1680` | `examples.mjs:41` |
| `builder.face` | none | Simulator only | No | n/a | `api.ts:1691` | Round trip only |
| `builder.mark` | `positions.toWorld` on first use | Simulator, plus Player | No | n/a | `api.ts:1700` | `examples.mjs:41` |
| `builder.position` | `positions.toWorld` on first use | Simulator, plus Player | No | n/a | `api.ts:1710` | `examples.mjs:41` |
| `builder.place` | `blocks.fill` | Default robot | Yes | Incidental, as `blocks.place` | `api.ts:1720` | Round trip only |
| `builder.line` | `shapes.line` | Default robot | Yes | Incidental, as `shapes.line` | `api.ts:1731` | `examples.mjs:41` |
| `builder.fill` | `blocks.fill` | Default robot | Yes | Incidental, as `blocks.fill` | `api.ts:1743` | `examples.mjs:41` |

None of these saves anything. The builder's state ends with the run.

## Agent (robot)

Every row uses R. The actor is the robot through its queue, except `agent.camera`. The op string is the call's name
(`agent.move` sends `agent.move`), except where the Op column says otherwise. Parsing is in `RobotCommand.java` and
execution in `RobotManager.java`. The statement blocks for calls that answer a value (`ns.ts:38` to `ns.ts:131`) send
the same ops and are not counted twice. `agent.pathStop` and `agent.liquid` (`ns.ts:13`, `ns.ts:23`) are value helpers.

### Movement

| Call | Op | Dependency | Checks, limits | Saved | Source (editor, parse, execute) | Test |
|---|---|---|---|---|---|---|
| `agent.move` | | Essential | 1 to 64 blocks (`RobotCommand.java:196`). One block per `ticksPerBlock` ticks (default 4). Stops in unloaded chunks with `UNLOADED`. Place on move uses F. | Robot position (entity) | `api.ts:193`, `RobotCommand.java:192`, `RobotManager.java:686` | `RobotGameTests.java:137`, `:147`, `:884`, `RelayGameTests.java:77` |
| `agent.turn` | | Essential | Left or right only | Robot facing | `api.ts:205`, `RobotCommand.java:199`, `RobotManager.java:1219` | `RobotGameTests.java:158` |
| `agent.moveTo` | `agent.moveTo` (after `positions.toWorld`) | Essential | Path search from `pathNodesPerTick`. Opens wooden doors and gates through F. | Robot position | `api.ts:220`, `RobotCommand.java:322`, `RobotManager.java:987` | `RobotGameTests.java:1344`, `:1357`, `:1372`, `:1390`, `:1408`, `:1424` |
| `agent.setHome` | | Essential | None | Home with dimension, in robot NBT (`RobotEntity.java:245`) | `api.ts:232`, `RobotCommand.java:379`, `RobotManager.java:1274` | `RobotGameTests.java:1709`, `MultiRobotGameTests.java:175` |
| `agent.goHome` | | Essential | Same dimension only, else false | Robot position | `api.ts:242`, `RobotCommand.java:379`, `RobotManager.java:624` | `RobotGameTests.java:1709` |
| `agent.goToRobot` | | Essential | The target must be one of the sender's robots, loaded, in the same dimension (`RobotManager.java:1036`) | Robot position | `api.ts:254`, `RobotCommand.java:326`, `RobotManager.java:1036` | `MultiRobotGameTests.java:646`, `:681`, `editor-suite.mjs:269` |
| `agent.followPath` | | Essential | At most 256 blocks per call (`RobotTrail.java:27`), route from the scan budget | Robot position | `api.ts:275`, `RobotCommand.java:333`, `RobotManager.java:1096` | `RobotGameTests.java:1452` to `:1567` (nine tests), `RobotGameTests.java:2173` |
| `agent.setGravity` | | Essential | None | `GravityMode` in robot NBT (`RobotEntity.java:244`) | `api.ts:355`, `RobotCommand.java:251`, `RobotManager.java:1246` | `RobotGameTests.java:1159`, `:1172`, `:1184`, `:1197`, `:1212`, `:1226` |

### Building

| Call | Op | Dependency | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|
| `agent.place` | | Essential (the robot's selected slot, next to the robot) | F | Robot inventory | `api.ts:312`, `RobotCommand.java:205`, `RobotManager.java:1312`, `:1375` | `RobotGameTests.java:899`, `:692` |
| `agent.destroy` | | Essential | F, no unbreakable blocks, drops go into the robot | Robot inventory | `api.ts:322`, `RobotCommand.java:205`, `RobotManager.java:1395` | `RobotGameTests.java:910`, `:922`, `:1053` |
| `agent.interact` | | Essential | F. Only doors, trapdoors, gates, buttons and levers (`RobotManager.java:1423`) | Nothing | `api.ts:334`, `RobotCommand.java:210`, `RobotManager.java:1419` | `RobotGameTests.java:1251`, `:1287` |
| `agent.setAssist` | | Essential | Only place on move | `PlaceOnMove` in robot NBT (`RobotEntity.java:243`) | `api.ts:345`, `RobotCommand.java:245`, `RobotManager.java:1242` | `RobotGameTests.java:1070` |

### Inspecting and sensors

| Call | Op | Dependency | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|
| `agent.detect` | | Essential (the block next to the robot) | None | Nothing | `api.ts:366`, `RobotCommand.java:215`, `RobotManager.java:1321` | `RobotGameTests.java:951` |
| `agent.detectEntity` | | Essential | Invisible players and spectators are never found (`RobotManager.java:1534`) | Nothing | `api.ts:377`, `RobotCommand.java:383`, `RobotManager.java:1332` | `RobotGameTests.java:1755` |
| `agent.countNearby` | | Essential (around the robot) | Radius 1 to 8 (`RobotCommand.java:390`) | Nothing | `api.ts:389`, `RobotManager.java:1278` | `RobotGameTests.java:1755` |
| `agent.isOpen` | | Essential | None | Nothing | `api.ts:399`, `RobotCommand.java:210`, `RobotManager.java:1331` | `RobotGameTests.java:1251` |
| `agent.redstonePower` | | Essential | None | Nothing | `api.ts:410`, `RobotCommand.java:210`, `RobotManager.java:1335` | `RobotGameTests.java:1311` |
| `agent.onRedstonePowered` | none (`control subscribe`, event `redstonePowered`) | Essential: PlayerEvents reads the default robot's block each tick, only while it is loaded (`PlayerEvents.java:145`). No event without a default robot, and none for named robots. | E | Nothing | `api.ts:421`, `sim.ts:61` | `RelayGameTests.java:699`, `:718`, `editor-suite.mjs:341` |
| `agent.getPosition` | | Essential | None | Nothing | `api.ts:430`, `RobotCommand.java:232`, `RobotManager.java:1257` | `RobotGameTests.java:173` |
| `agent.getOrientation` | | Essential | None | Nothing | `api.ts:440`, `RobotCommand.java:232`, `RobotManager.java:1261` | `RobotGameTests.java:173` |
| `agent.findNearest` | | Essential (around the robot) | Radius 1 to 32 (`RobotCommand.java:264`), scan budget, loaded chunks only | Nothing | `api.ts:453`, `RobotCommand.java:256`, `RobotManager.java:725` | `RobotGameTests.java:200`, `:233`, `:249` |
| `agent.findNearestCategory` | | Essential | As `findNearest`, 6 categories | Nothing | `api.ts:466`, `RobotCommand.java:256`, `RobotManager.java:725` | `RobotGameTests.java:212` |
| `agent.radar` | | Essential | Radius 1 to 16, at most 64 answers, invisible players never found (`RobotCommand.java:128`, `:131`) | Nothing | `api.ts:481`, `RobotCommand.java:392`, `RobotManager.java:1548` | `RobotGameTests.java:1796` |
| `agent.nearestEntity` | `agent.radar` | Essential | As `agent.radar` | Nothing | `api.ts:498` | Through `RobotGameTests.java:1796` |
| `agent.detectLiquid` | | Essential | None | Nothing | `api.ts:509`, `RobotCommand.java:396`, `RobotManager.java:1558` | `RobotGameTests.java:1841` |
| `agent.isSafe` | | Essential | None | Nothing | `api.ts:520`, `RobotCommand.java:396`, `RobotManager.java:1569` | `RobotGameTests.java:1863` |
| `agent.lightLevel` | | Essential (the robot's own block) | None | Nothing | `api.ts:532`, `RobotCommand.java:400`, `RobotManager.java:1292` | `RobotGameTests.java:1891` |
| `agent.biome` | | Essential (the robot's own block) | None | Nothing | `api.ts:542`, `RobotManager.java:1295` | `RobotGameTests.java:1891` |
| `agent.temperature` | | Essential (the robot's own block) | None | Nothing | `api.ts:552`, `RobotManager.java:1300` | `RobotGameTests.java:1891` |
| `agent.inspect` | | Essential | None | Nothing | `api.ts:686`, `RobotCommand.java:215`, `RobotManager.java:1324` | `RobotGameTests.java:933` |

### Measuring

| Call | Op | Dependency | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|
| `agent.measure` | | Essential (from the robot, along its facing) | Max 1 to 32, scan budget, `UNLOADED` in unloaded chunks | Nothing | `api.ts:572`, `RobotCommand.java:342`, `RobotManager.java:765` | `RobotGameTests.java:1582` |
| `agent.measureRun` | | Essential | As `measure` | Nothing | `api.ts:586`, `RobotCommand.java:346`, `RobotManager.java:768` | `RobotGameTests.java:1597` |
| `agent.measureGap` | | Essential | As `measure` | Nothing | `api.ts:600`, `RobotCommand.java:350`, `RobotManager.java:776` | `RobotGameTests.java:1614` |
| `agent.groundHeight` | `agent.groundHeight` (after `positions.toWorld`) | **Incidental**: reads a world x and z; the robot is only the N anchor | N (`RobotManager.java:784`) | Nothing | `api.ts:611`, `RobotCommand.java:356`, `RobotManager.java:783` | `RobotGameTests.java:1629`, `:1662` |
| `agent.heightProfile` | | Essential (a line from the robot) | Length 1 to 32, forward, back, left or right only | Nothing | `api.ts:626`, `RobotCommand.java:360`, `RobotManager.java:787` | `RobotGameTests.java:1629` |
| `agent.isAreaFree` | `agent.isAreaFree` (after `positions.toWorld`) | **Incidental**: a world box; the robot is only the N anchor | Box under 32 blocks per side (`RobotCommand.java:371`), N for both corners (`RobotManager.java:796`), scan budget over ticks | Nothing | `api.ts:639`, `RobotCommand.java:366`, `RobotManager.java:794` | `RobotGameTests.java:1646`, `:1662` |
| `agent.countSolid` | `agent.countSolid` (after `positions.toWorld`) | **Incidental**, as `isAreaFree` | As `isAreaFree` | Nothing | `api.ts:653`, `RobotCommand.java:366`, `RobotManager.java:794` | `RobotGameTests.java:1646`, `:1677` |
| `agent.findBounds` | | Essential (the block in front of the robot) | Max 1 to 32, six reads per block from the scan budget | Nothing | `api.ts:674`, `RobotCommand.java:375`, `RobotManager.java:861` | `RobotGameTests.java:1692` |

### Inventory, containers, crafting

| Call | Op | Dependency | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|
| `agent.setSlot` | | Essential (robot state) | Slot 1 to 27 | `SelectedSlot` in robot NBT (`RobotEntity.java:242`) | `api.ts:697`, `RobotCommand.java:221`, `RobotManager.java:1223` | `RobotGameTests.java:1131` |
| `agent.getItemCount` | | Essential | Slot 1 to 27 | Nothing | `api.ts:708`, `RobotCommand.java:221`, `RobotManager.java:1227` | `RobotGameTests.java:968` |
| `agent.getItemSpace` | | Essential | Slot 1 to 27 | Nothing | `api.ts:719`, `RobotCommand.java:221`, `RobotManager.java:1230` | `RobotGameTests.java:968` |
| `agent.freeSlots` | | Essential | None | Nothing | `api.ts:728`, `RobotCommand.java:404`, `RobotManager.java:1284` | `RobotGameTests.java:1922` |
| `agent.isInventoryFull` | | Essential | None | Nothing | `api.ts:737`, `RobotCommand.java:404`, `RobotManager.java:1287` | `RobotGameTests.java:1922` |
| `agent.setItem` | | Essential (the robot's inventory) | Free items, else `NO_PERMISSION` (`RobotManager.java:1236`). Count 1 to 64, slot 1 to 27, the item must exist in this Minecraft version (`RobotCommand.java:530`). | Robot inventory | `api.ts:750`, `RobotCommand.java:226`, `RobotManager.java:1235` | `RobotGameTests.java:968`, `:989`, `:2067`, `RelayGameTests.java:388` |
| `agent.collectAll` | | Essential (within one block of the robot) | Items kept for another player are left alone (`RobotManager.java:1617`) | Robot inventory | `api.ts:759`, `RobotCommand.java:232`, `RobotManager.java:1250` | `RobotGameTests.java:1033` |
| `agent.drop` | | Essential | Slot 1 to 27, amount 1 to 64 | Robot inventory | `api.ts:772`, `RobotCommand.java:237`, `RobotManager.java:1336` | `RobotGameTests.java:1011` |
| `agent.dropAll` | | Essential | None | Robot inventory | `api.ts:782`, `RobotCommand.java:205`, `RobotManager.java:1337` | No GameTest. Parsing: `ProtocolTest.java:143` |
| `agent.transfer` | | Essential | Slots 1 to 27, amount 1 to 64 | Robot inventory | `api.ts:795`, `RobotCommand.java:241`, `RobotManager.java:1254` | `RobotGameTests.java:1000` |
| `agent.storeItems` | | Essential (container next to the robot, robot inventory) | The container is opened through F (`RobotManager.java:1456`): claims, locks and a blocked chest lid apply. Robot slot 0 to 27. | Robot inventory and the container | `api.ts:808`, `RobotCommand.java:296`, `RobotManager.java:1469` | `RobotGameTests.java:452`, `:477`, `:568`, `:711` |
| `agent.takeItems` | | Essential | As `storeItems`. Container slot 0 to 54, count 1 to 64. | Robot inventory and the container | `api.ts:822`, `RobotCommand.java:300`, `RobotManager.java:1486` | `RobotGameTests.java:496`, `:1113` |
| `agent.takeItem` | | Essential | As `storeItems` | Robot inventory and the container | `api.ts:836`, `RobotCommand.java:305`, `RobotManager.java:1486` | `RobotGameTests.java:496`, `:523` |
| `agent.containerCount` | | Essential | As `storeItems`; 0 when refused | Nothing | `api.ts:848`, `RobotCommand.java:309`, `RobotManager.java:1440` | `RobotGameTests.java:496` |
| `agent.containerSize` | | Essential | As `storeItems`; 0 when refused | Nothing | `api.ts:858`, `RobotCommand.java:313`, `RobotManager.java:1439` | `RobotGameTests.java:496`, `:541` |
| `agent.sortContainer` | | Essential | As `storeItems`; plain storage only | The container | `api.ts:869`, `RobotCommand.java:313`, `RobotManager.java:1509` | `RobotGameTests.java:541` |
| `agent.craft` | | Essential (robot inventory; 3x3 needs a crafting table next to the robot) | Count 1 to 64 | Robot inventory | `api.ts:883`, `RobotCommand.java:317`, `RobotManager.java:1268`, `RobotCrafting.java:56` | `RobotGameTests.java:802`, `:819`, `:845` |
| `agent.canCraft` | | Essential | Count 1 to 64 | Nothing | `api.ts:895`, `RobotCommand.java:317`, `RobotManager.java:1271` | `RobotGameTests.java:819` |
| `agent.stonecut` | | Essential (a stonecutter next to the robot) | Count 1 to 64 | Robot inventory | `api.ts:907`, `RobotCommand.java:317`, `RobotCrafting.java:56` | `RobotGameTests.java:864` |

### Feedback

| Call | Op | Actor | Dependency | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|---|
| `agent.say` | | Robot (queue) | Essential (text over the robot) | At most 80 characters (`RobotCommand.java:113`) | Nothing | `api.ts:917`, `RobotCommand.java:267`, `RobotManager.java:1264` | `RobotGameTests.java:262`, `RobotCommandTest.java:186` |
| `agent.camera` | | Player (ServerRelay with RobotCamera, at once, not queued) | Essential for Eyes and Follow. Off needs no robot (`RobotCamera.java:88`). | `halisoncode.robot` (`ServerRelay.java:130`). Game rule `halisoncode:robot_camera` off, owner or ops (`RobotCamera.java:45`). The robot must exist and be the player's (or the player is an op), in the same dimension, in a chunk the player can see (`RobotCamera.java:52`). | Nothing | `api.ts:928`, `RobotCommand.java:441`, `ServerRelay.java:178`, `RobotCamera.java:87` | `RelayGameTests.java:217`, `:346`, `RobotGameTests.java:2273`, `:2294`, `:2319`, `:2330`, `:2343` |

## Robots (several robots)

| Call | Op | Actor | Robot today | Checks, limits | Saved | Source | Test |
|---|---|---|---|---|---|---|---|
| `robots.get` | none | Simulator only (a handle with a name) | No, until the handle is used | Name rules are checked when a command is sent (`Protocol.java:220`) | Nothing | `api.ts:1512` | `ProtocolTest.java:36`, `:49` |
| `Robot.<call>`: 58 calls, the same as `agent` except `onRedstonePowered` | The agent op with a `robot` field | That named robot | Yes, that robot | As the agent call. Only the sender's own robots (`ServerRelay.java:127`). How many robots exist is set by the game rule `halisoncode:max_robots`, 1 to 16, default 1, checked at `/robot spawn` (`RobotManager.java:298`, `HalisonCode.java:75`). | Robot NBT as for `agent`, and the name in `RobotLinks` | `api.ts:941`, `api.ts:947` | `MultiRobotGameTests.java:348`, `:371`, `:159`, `:227`, `:254`, `examples.mjs:47` |
| `control.runInParallel` | none | Simulator only | No | Each robot still does one thing at a time | Nothing | `api.ts:1767` | `MultiRobotGameTests.java:348`, `examples.mjs:47` |

## Other simulator-only calls

| Call | Source | Test |
|---|---|---|
| `console.log` | `api.ts:1754` | Round trip, `examples.mjs:41` |
| `loops.pause` | `api.ts:1782` | `editor-suite.mjs:156` |
| `loops.forever` | `api.ts:1792` | `editor-suite.mjs:156` |
| `control.assert` and `control.check` | `ns.ts:276`, `ns.ts:285` | `editor-suite.mjs:544` |
| `control.fail` | `ns.ts:295` | `editor-suite.mjs:544` |

None of these needs a robot, a game connection or a permission.

## Count

- 110 editor operations: player 8, gameplay 3, positions 14, signals 2, blocks 3, shapes 4, builder 9, agent 59,
  robots 3, other 5. The 58 `Robot.<call>` methods are counted once, as one row.
- 71 distinct op strings reach the server: 57 `agent.*`, 5 `player.*`, 2 `gameplay.*`, `signals.send`,
  `positions.toWorld`, `blocks.fill` and 4 `shapes.*`. `RobotCommand.Op.COME` is only for `/robot come` and never
  comes from the editor (`RobotCommand.java:71`).

## Summary 1: operations that already run without a robot

Server side, answered about the player:

- `player.say`, `player.position`, `player.getOrientation`, `player.name`, `player.teleport` (PlayerApi)
- `gameplay.timeOfDay`, `gameplay.isRaining` (PlayerApi)
- `signals.send` (PlayerApi)
- `Position.toWorld`, and through it `distanceTo`, `sortByDistance`, `nearest` (ServerRelay)
- Events: `player.onChat`, `player.onDied`, `gameplay.onDayStart`, `blocks.onBlockBroken`, `signals.onReceived`
  (PlayerEvents)
- `agent.camera` with `CameraMode.Off` (no robot needed, but it still needs `halisoncode.robot`)

Simulator only: `player.runChatCommand`, all other position calls, `builder.teleportTo`, `move`, `turn`, `face`,
`mark`, `position`, `robots.get`, `control.runInParallel`, `console.log`, `loops.pause`, `loops.forever`,
`control.assert`, `control.check`, `control.fail`.

The player ops and `positions.toWorld` skip the `halisoncode.robot` check, so they work for a player that a
permission mod keeps from robots (`RelayGameTests.java:346`).

## Summary 2: operations coupled to the robot only incidentally

Candidates for #351 and #362.

| Operations | What the robot supplies today | Why it is incidental |
|---|---|---|
| `blocks.fill`, `blocks.place`, `shapes.line`, `shapes.circle`, `shapes.sphere`, `shapes.maze`, `builder.place`, `builder.line`, `builder.fill` | The queue and the shared build budget over ticks; the FakePlayer with the owner's UUID (F); the item source (robot inventory) unless free items; the place where broken blocks' drops go; the anchor of the 32-block rule (N) | The targets are world coordinates, worked out from the player's feet in the editor. Nothing depends on where the robot stands except the range check. |
| `agent.groundHeight`, `agent.isAreaFree`, `agent.countSolid` | The queue and the scan budget; the anchor of the 32-block rule | They read world coordinates. The robot's position is only used for the range check (`RobotManager.java:784`, `:796`). |
| `agent.camera` with `CameraMode.Off` | Only the `halisoncode.robot` permission check | `RobotCamera.set` turns the view off without looking at a robot (`RobotCamera.java:88`). |

Not incidental, but a player-based version would be a new op rather than a reroute: `agent.lightLevel`,
`agent.biome`, `agent.temperature`, `agent.findNearest`, `agent.findNearestCategory`, `agent.radar`,
`agent.nearestEntity`, `agent.countNearby`. They read at the robot's own position by design.

## Summary 3: unresolved dependencies and questions for the owner

1. **Item source without a robot.** Building takes blocks from the robot's inventory unless free items apply
   (`RobotManager.java:948`). Without a robot, should building take blocks from the player's inventory, or work only
   with free items (creative, `halisoncode:free_items`, or the permission), or both?
2. **Drops without a robot.** A fill breaks blocks in the way and the drops go into the robot (`RobotManager.java:944`,
   `:1410`). Without a robot, should they go to the player's inventory, drop in the world, or be destroyed?
3. **Range anchor.** Today positions are relative to the player, but the 32-block rule is measured from the robot
   (`RobotManager.java:907`). A player far from the robot gets `OUT_OF_RANGE` for `pos(0, 0, 0)`. Is that intended?
   For robot-free building, should the anchor be the player's feet, with the same 32 blocks?
4. **Who acts.** Robot edits run as a FakePlayer with the owner's UUID and the owner's game mode rules (F). Should
   robot-free edits use the same FakePlayer (not tied to a robot), or the real player? Claim mods see both as the
   owner, but block events differ (a real player's breaks would also fire `blocks.onBlockBroken`, see
   `PlayerEvents.java:117`).
5. **Permission.** Today every non-player op needs `halisoncode.robot` (`ServerRelay.java:130`), and building also
   needs `halisoncode.build`. Should robot-free building need only `halisoncode.build`?
6. **Queue and budget.** PlayerApi runs one call per tick and answers at once. A fill runs over many ticks with the
   shared build budget. Where should a robot-free build wait: a per-player queue inside `RobotManager.execute`, with
   the same fair share? And should `/robot stop`, pause and the Stop button cancel it the same way?
7. **Named robots and building.** Building always uses the default robot (`api.ts:285`, `api.ts:1536`). A player with
   only named robots cannot build at all. Should `Robot` get the building calls, or should building leave the robots
   altogether?
8. **Compatibility.** If a robot-free path exists, should a program on a server with the default robot still build
   through the robot (inventory, range, drops as today), so old programs behave the same?
9. **Redstone event.** `agent.onRedstonePowered` only watches the default robot (`PlayerEvents.java:145`). A program
   with no default robot never gets the event and is not told. Should the subscribe answer with a warning, and should
   named robots get the event?
10. **Test gap.** Every relay GameTest spawns a default robot before it starts (`RelayGameTests.java:68`). So no test
    shows that the player, gameplay, signal and position calls work with no robot at all. One GameTest with no
    robot should come before #351 changes anything.
11. **Missing server tests.** `agent.dropAll` and `shapes.circle` are parsed in tests but never run in a GameTest.
12. **Loaders.** All GameTests in this matrix run on Fabric. NeoForge has only smoke GameTests
    (`neoforge/src/gametest/java/halisoncode/gametest/NeoForgeSmokeGameTests.java`). Epic #344 asks for protection,
    ownership, rate limits and stop to be verified on both loaders.
13. **Docs drift.** `docs/architecture.md:343` says the owner to robot link is kept in a Fabric data attachment.
    The code keeps it in `RobotLinks`, a SavedData file (`RobotLinks.java:36`). `docs/architecture.md:330` calls
    server resolution of relative positions a proposal; it is built (`ServerRelay.java:162`).
14. **Player-based sensors.** Should there be robot-free versions of the robot's own-block reads (light level, biome,
    temperature) and of `findNearest`, measured at the player? `agent.radar` at the player would let a program locate
    other players near the owner; that needs a privacy decision first.
