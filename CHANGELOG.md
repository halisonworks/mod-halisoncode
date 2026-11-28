# Changelog

Every user-visible change to Halison Code (Codeling up to 0.0.5) is listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the mod version follows
[Semantic Versioning](https://semver.org/) (see "Versioning" in [CONTRIBUTING.md](CONTRIBUTING.md)).

## [Unreleased]

### Fixed

- Halison Code's messages in the game were English even with the game set to Norwegian (issue #457): the texts were in
  `nb_no.json`, but Minecraft's code for Norwegian Bokmal is `no_no`, so the game never read them. The file is now
  `no_no.json`, and a test fails if it gets the old name again.

## [0.0.7] - 2026-11-27

The seventh pre-release, for testing: fixes found while writing lessons against 0.0.6.

### Added

- The game's messages in Norwegian (issue #275): with the game set to Norsk bokmål, chat replies to `/robot` and
  `/halisoncode`, the game rules and the key names are in Norwegian. What the robot says over its head and the robot
  log stay English for now. CONTRIBUTING.md says how to add another language.

### Changed

- Building in survival (issue #438): when the robot runs out of blocks during fill, place, line, shapes, maze or
  builder, the call now fails with the new error `NO_ITEMS` ("I ran out of oak planks after 90 blocks."), which
  JavaScript and Python can catch. Before, the program went on as if the build was done. The blocks set so far stay,
  and the robot still says why over its head.
- "agent count solid blocks" is now "agent count blocks" ("robot tell blokker") (issue #436). It counts what it did
  before, every block but air, grass, snow and water, and the help now says that torches, flowers and signs count too.
  The JavaScript name `agent.countSolid` stays.

### Fixed

- Robot actions that answer a value (move to, go home, go to robot, follow path, store and take items, craft,
  stonecut) can stand alone as a block too, without using the answer (issue #433). Before, a program had to put the
  block in a variable or an "if".
- Every block has a Norwegian text, also the math, text and list blocks that were English (issue #434). CI now
  checks it.
- Robot names may have any letter, also æ, ø and å, as in `/robot spawn Bærer` (issue #435).
- `/robot spawn` on a chest, a slab or a path puts the robot on top of it, not inside it (issue #437).

## [0.0.6] - 2026-11-26

The sixth pre-release, for testing. Codeling is now called Halison Code.

**Update the server and every client at the same time.** A 0.0.5 client cannot join a server with this version, and a client
with this version cannot join a 0.0.5 server (tested on Minecraft 26.3, Fabric and NeoForge). The game then says that
there are unknown registry entries or keys, not that the mod must be updated.

### Added

- Permissions (issue #51): with a permission mod, such as LuckPerms, a server decides per player who may use a robot
  (`halisoncode.robot`), build with place, fill, shapes and maze (`halisoncode.build`), get items free (`halisoncode.free_items`) and
  teleport (`halisoncode.teleport`). Without one nothing changes.
- Server limits in `config/halisoncode-server.json` (issue #52): how much robots may do each tick, how long they may take, and how
  many messages a player's browser may send. Robots use at most about 10 ms of a tick from the start; before, a build that
  broke blocks over a pile of items could stop the server for longer.

### Changed

- When another program uses the editor's port, `/halisoncode` says so and what to do, and tries the same port again (issue #8).
  Before, it did nothing.
- Codeling is now Halison Code, and the robot is called Kintro (issues #356, #359 and #422). The mod id is `halisoncode`, the
  editor opens with `/halisoncode`, and the entity is `halisoncode:kintro`. Game rules, permissions, the server config
  (`config/halisoncode-server.json`), jar files and the robot's player name (`HalisonCode[<owner>]`) follow. `/robot` is as before.
  **Robots, game rules, settings and editor projects from 0.0.5 and before are not carried over.** Before you change: empty
  your robots, copy your programs' JavaScript to a text file (old project files cannot be imported), and take the old jar out of the `mods` folder.
- The project is now at `github.com/halisonworks/mod-halisoncode` (issue #349). The links in the mod list and the downloads in
  `scripts/setup-multimc.ps1` use the new address.
- Players share each tick equally, whatever number of robots they have (issue #52). Before, a player with 16 robots got 16
  times as much as a player with one.
- 19 Norwegian editor texts have new wording (issue #59).

### Fixed

- `player.getOrientation()` answers -180 also when you look just beside due north (issue #22). It could answer 180 there.

## [0.0.5] - 2026-11-24

The fifth pre-release, for testing.

**Use the same Codeling version on the server and on every client.** Mixing versions (for example a 0.0.4 client
on a 0.0.5 server) is not supported and has not been tested.

### Added

- Game rule `codeling:pause_on_lost_focus` (issue #297): `false` keeps the game from pausing when its window loses focus, in
  this world. For the player who runs the world (singleplayer, or opened to LAN). `true`, the default, changes nothing. The
  player's own F3+P option is never changed.

### Changed

- The Norwegian editor has new texts for its own buttons, menus and standard blocks (issue #59), written for Codeling. The
  earlier ones came from makecode.com without a known licence and are removed. Rarely used texts are now in English.
- `/robot remove agent` removes your default robot (issue #324). Its items go to you and your program stops. The next
  `/robot spawn` gives it back its name, skin and color. Before, it could not be removed.
- Robots pick up only what their owner could (issue #53): not an item that is kept for another player, and not one that was
  thrown or dropped a moment ago. `agent.drop` followed at once by `agent.collectAll` leaves the item, so wait half a second
  first. Breaking a block still takes its drops at once.

### Fixed

- The editor is Norwegian from the start in a browser set to Norwegian (issue #315). Before, most of them got English.
- NeoForge: a client without NeoForge is refused when it connects to a server with Codeling, and told what to install
  (issue #371). Before, it could join and was disconnected when it saw a robot.
- A player with named robots but no default robot is no longer told "You have no robot" (issue #327).

## [0.0.4] - 2026-11-24

The fourth pre-release, for testing.

**Use the same Codeling version on the server and on every client.** Mixing versions (for example a 0.0.3 client
on a 0.0.4 server) is not supported and has not been tested.

### Added

- Extended logging and a report for bug reports (issue #279): `/codeling log on` writes more to the game's log for 15 minutes
  (1 to 60), and `/codeling report` writes the versions, the game rules and the newest Codeling lines to
  `logs/codeling-report.txt`, without known player names, chat or what robots say. Ops have `/codeling server log` and
  `/codeling server report` for the server.

### Changed

- Codeling is quiet in the game's log in normal play (issue #279): start, stop, the editor's connection, programs that start
  and end, refused commands and faults. The messages between the editor and the game, what programs say, and the first
  characters of the editor token are no longer logged.
- Less work and less memory use per frame when robots are in view (issue #284): the robot's animations are read from the
  model file once instead of for every frame. They look the same.
- Messages in the game look and read like Minecraft's own (issue #278): white for done, red for refused, the game's
  errors for wrong arguments, and above the hotbar when you click a robot that is not yours. The `/robot` commands answer in
  plain words without internal codes, a missing robot is always explained the same way, and `/robot give`,
  `/robot skin`, `/robot animation` and `/robot color` suggest what you can write. `/robot status` without a robot and a
  refused `/codeling signal` name now count as failed commands.
- `agent.getOrientation()` and `robot.getOrientation()` answer -180 when the robot faces north, as MakeCode's reference
  says (issue #22). They answered 180, so change a program that compares with 180.
- The project moved to `github.com/halisonworks/mod-codeling`. The links in the mod list (homepage, sources, issues) and
  the downloads in `scripts/setup-multimc.ps1` use the new address.

### Fixed

- Your default robot has a name in the `/robot` commands (issue #293): `agent`, and the name you give it with
  `/robot rename agent Bob` or a name tag. Tab suggests both. Before, only robots with a name could be picked by name.
  The name is kept when the robot is spawned again while it is not loaded, and no other robot of yours can take it. New
  robots cannot be called `agent`. `/robot rename Miner` without a new name says what is missing.
- `/codeling signal` works from the chat of an operator who plays on Fabric with Codeling (issue #279). It answered
  "Incorrect argument for command".
- The robot log in the editor keeps its newest 1000 lines (issue #283). Before, it kept every line, and the browser
  used more and more memory while a program ran.

## [0.0.3] - 2026-11-12

The third pre-release, for testing.

**Use the same Codeling version on the server and on every client.** Mixing versions (for example a 0.0.2 client
on a 0.0.3 server) is not supported and has not been tested.

### Added

- World queries (issue #87): `gameplay.timeOfDay()` and `gameplay.isRaining()` read the time and weather of your
  dimension, and `agent.lightLevel()`, `agent.biome()` and `agent.temperature()` read the robot's own block, in
  Blocks, JavaScript and Python, with a new "Torches in the dark" example.
- More events (issue #89): `blocks.onBlockBroken(block, handler)` ("on block broken") runs when you break that kind of
  block by hand, `agent.onRedstonePowered(handler)` ("on agent redstone powered") when your default robot's block gets
  a new redstone signal, and `gameplay.onDayStart(handler)` ("on day start") when the time of day wraps round to the
  morning, in Blocks, JavaScript and Python. Never for other players' breaks or your robot's own. At most 16 blocks
  per run; the 16 waiting events per player are shared with chat and death.
- Go to another robot (issue #243): `agent.goToRobot(robots.get("Miner"))` and `miner.goToRobot(other)` ("go to
  robot") walk like "move to" to a free block next to another of your own robots and face it, and the "robot (name)
  position" block gives a named robot's position, in Blocks, JavaScript and Python.
- Signals (issue #249): `signals.onReceived(name, handler)` ("on signal") runs when another mod on the server, or an
  op with `/codeling signal <players> <name> [number]`, sends you a signal with that name, and `signals.send(name)`
  ("send signal") sends a name to the server's other mods, in Blocks, JavaScript and Python. Only the name goes out,
  never to another player, and the same name at most once a second. Other mods use `codeling.api.Signals` in the core
  jar (`post` from any thread, `onSent` listeners). At most 16 names per run; the 16 waiting events per player are
  shared with the other events.
- Robot styles and movement (issue #242): the robot is an articulated model instead of an observer block, with
  three original styles (workshop, industrial, mint) chosen with `/robot skin` and a panel colour with `/robot color`,
  both saved per robot. It walks and turns with its moves, and `/robot animation` plays a head look or a wave.

### Fixed

- `scripts/setup-multimc.ps1 -Release` stopped with a bare 404 for a draft release or a tag without a release. It
  now says so and shows the `gh release download` and `-JarDir` commands that work instead.

## [0.0.2] - 2026-11-12

The second pre-release, for testing.

### Added

- `loops.pause(ms)` and `loops.forever` with the blocks "pause (ms)" and "forever" in the Loops category: wait in
  one part of a program, and repeat code until the program is stopped (issue #215).
- Position math in the Positions category: `distanceTo` (straight line, with decimals), `subtract` and `scale` on a
  position, and `positions.sortByDistance` and `positions.nearest` for a list of positions, so Blocks can pick the
  closest of a radar list without a function argument (issue #219).
- An Examples gallery on the editor's start page with the seven example programs the editor suite tests, each with a
  title, one sentence on what it teaches and an icon, in English and Norwegian. Opening one makes a new project in
  Blocks, JavaScript or Python. It works offline like the rest of the editor (issue #216).
- Debug mode in the editor (the bug button under the simulator): breakpoints and Step in Blocks, JavaScript and
  Python, the running block or line highlighted, the values of the variables while the program is stopped, and
  Slow-Mo, which highlights each block as it runs. A robot command always finishes before the program stops.
  "Exit Debug Mode" stops the program; pxt would have run it again from the start (issue #217).
- A robot command the game refuses throws an error into the program, the text `CODE: message`, so JavaScript
  `try`/`catch` and Python `try`/`except` can catch it and go on. Not caught, it stops the program with the "Robot
  error" dialog as before, and the robot log names the command. Stop, `/robot stop`, a lost connection, `STOPPED` and
  `VERSION` still end the program and cannot be caught. The player guide lists the error codes (issue #218).
- `control.assert(condition, message)` and `control.fail(message)`, with the blocks "check ... or stop with" and
  "stop with error" in the Logic category. Python `assert` and `raise` now compile, and they, JavaScript `throw` and
  the two blocks all stop the program with the message in a "Program error" dialog; debug mode marks the block or line
  it came from. The "Room and fence" example checks where its line of glass ends (issue #221).
- A Player category with MakeCode's `player.say`, `player.position`, `player.getOrientation`, `player.name` and
  `player.teleport` (issue #26). They are about the program's own player only and need no robot. Unlike MakeCode,
  `say` shows the message to that player only, as plain text of at most 256 characters. `teleport` works for the host
  of a singleplayer world or an op, in the player's own dimension, inside the world border and the build height;
  anyone else gets `NO_PERMISSION`. One player call per tick, at most 16 waiting (`RATE_LIMITED`).
- Chat and death events in the Player category (issue #27): `player.onChat(keyword, handler)` ("on chat command"),
  `player.onDied(handler)` ("on player died") and `player.runChatCommand(text)` ("run chat command"), in Blocks,
  JavaScript and Python. A chat line whose first word is a keyword of the sender's running program (or any line for
  `*`) is not broadcast; the program gets the keyword and up to three numbers parsed as JavaScript's `parseInt`
  (0 when missing or not a number), never the line. Lines from other players, and every line once the run ends, are
  broadcast as usual. `/codeling chat <text>` does the same for servers whose chat plugins take chat lines first.
  `runChatCommand` runs the program's own handlers in the editor and sends nothing to the game. A program with
  handlers keeps running after its last line until Stop or `/robot stop`. At most 16 keywords of 32 characters per
  run, and 16 events waiting per player.
- Mod jars for Minecraft 26.2 and for 26.1 to 26.1.2 (Fabric and NeoForge, Java 25), and for 1.21.11, 1.21.9 to 1.21.10 and 1.21.6 to 1.21.8, 1.21.5, 1.21.4 and 1.21.2 to 1.21.3 (Java 21).
- `scripts/setup-multimc.ps1` sets up a MultiMC instance for every loader and Minecraft version Codeling supports, to test the jars in a launcher.

### Changed

- `agent.say` turns the section sign (the colour code character) into a space, as `player.say` does.

### Fixed

- The Fabric jar for Minecraft 1.21.1 accepted every 1.21.x version and crashed on 1.21.2 and later. It now
  accepts 1.21 and 1.21.1 only, so Fabric names the right jar instead.
- A world moved from Minecraft 1.21.x to 26.x keeps its robots: 26.x reads the robot links from
  `data/codeling_robots.dat` when `data/codeling/robots.dat` is missing.
- Positions in the lists from `agent.radar` and `agent.findBounds` lost their methods, so `toString` or `toWorld` on
  one of them failed (issue #219).

## [0.0.1] - 2026-11-11

The first public version, a pre-release for testing.

### Added

- Mod jars for Fabric and NeoForge, on Minecraft 26.3 (Java 25) and 1.21.1 (Java 21).
- A robot per player, spawned with `/robot spawn`, with a 27-slot inventory. The game rule `codeling:max_robots`
  (1 to 16, default 1) allows more robots with names, which programs reach through `robots.get("name")`.
- A browser editor (a pxt target) with Blocks, JavaScript and Python, in English and Norwegian. `/codeling` opens it;
  the mod serves it from `127.0.0.1` with a per-session token, and it makes no requests to other hosts.
- Robot blocks and calls for movement (move, turn, move to, follow path, go home), building, inspecting, sensors
  (radar, liquids, safety), measuring, inventory, containers, crafting and stonecutting, and `say`.
- Positions relative to the player, to where the player looks, or in the world, and building helpers: `blocks.fill`,
  `blocks.place`, lines, circles, spheres, mazes and a builder.
- `control.runInParallel` to run several robots at the same time.
- `/robot` chat commands for every basic action, plus `list`, `remove`, `rename`, `nametag`, `come`, `sethome` and
  `stop all` for ops.
- A robot camera: look through the robot's eyes or follow it from behind (`/robot camera`, key binds). The game rule
  `codeling:robot_camera` (off, owner, ops) controls who may use it.
- The game rule `codeling:free_items` for free items in survival.
- Multiplayer: a player can only program their own robots. Robots act through a fake player with the owner's
  identity, so protection and claim mods apply to them.
- Per-tick limits on actions, scanned blocks, path search and placed blocks, and a stop after 10 seconds without
  contact from the editor.
- Projects saved by an older editor version load with their item and block values kept.
- A player guide in English and Norwegian.
- `SECURITY.md`: how to report a vulnerability privately, and what is in scope.
- Mod list links: source and issue tracker in `fabric.mod.json` and `neoforge.mods.toml`.
- A name for the `codeling:max_robots` game rule on the Edit Game Rules screen.
- `LICENSE` and `THIRD-PARTY-NOTICES.md` in every jar under `META-INF/`, and a full list of the libraries inside the
  packaged editor.
- A release workflow that builds the four jars from a `v*` tag and publishes them with SHA-256 sums.

### Security

- `/codeling` no longer puts the editor link with its token on a command line, where other users on the same computer
  could read it (issue #191). It opens a one-time launcher file that only you can read, deleted after a minute, and the
  chat line has a link that copies the editor link for browsers that cannot open the file.
- Robots follow their owner's rules (issue #53): no edits inside spawn protection unless the owner is an op, no placing
  or breaking for an owner in adventure mode, and nothing at all for an owner in spectator mode.

