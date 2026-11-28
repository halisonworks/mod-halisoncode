# Commands and game rules

Every command and game rule of Halison Code in one place (issue #273). The player guides
([English](player/guide-en.md), [Norwegian](player/guide-nb.md)) explain them for players, and the
[server admin guide](server-admin.md) explains limits, permissions and protection.

"Ops" means operators (the gamemaster permission level, as for `/gamerule`). A robot name at the end of a command (shown
as `[name]`) picks one of your own robots; without it the command is for your default robot, `agent`.

## /halisoncode

| Command | Who | What it does |
|---|---|---|
| `/halisoncode` | Every player (client command) | Opens the editor in the browser. The key H (Controls, under Halison Code) does the same. |
| `/halisoncode log on [minutes]`, `/halisoncode log off` | Every player (client command) | Extended logging in your own game for 15 minutes, or 1 to 60. Also ends after 50,000 lines. |
| `/halisoncode report` | Every player (client command) | Writes what a bug report needs to `logs/halisoncode-report.txt`. |
| `/halisoncode chat <text>` | Every player | Sends the text to your own running program's "on chat command" code, as typing it in the chat would. For servers whose chat plugins take chat lines first. |
| `/halisoncode signal <players> <name> [number]` | Ops, command blocks, the console | Sends a signal to the programs of the players named, with the number (0 without one). |
| `/halisoncode server log on [minutes]`, `/halisoncode server log off` | Ops, the console | Extended logging on the server, as above. |
| `/halisoncode server report` | Ops, the console | The report of the server, in the server's `logs` folder. |

## /robot

Without the permission `halisoncode.robot` (see the [server admin guide](server-admin.md)), `/robot` is an unknown command
for that player. The console may always use it.

| Command | What it does |
|---|---|
| `/robot spawn [name]` | Spawns your robot at your feet facing your way, or moves it there. A new named robot needs room under `halisoncode:max_robots`. Ends your running program. |
| `/robot list` | Your robots, loaded or not, with positions. |
| `/robot remove <name>` | Removes a loaded robot. Its items go to your inventory (the rest drop at your feet). Removing `agent` also ends your program. |
| `/robot rename <name> <new name>`, `/robot rename <new name>` | Renames a robot, or your default robot. A named robot's queue is cleared. Name tag items work too (owner and ops). |
| `/robot nametag <on\|off> [name]` | Shows or hides the name tag of one robot, or of all your loaded robots. |
| `/robot stop [name]` | Without a name: drops the queues of all your robots and ends your program. With a name: only that robot's queue. |
| `/robot stop all` | Ops only: stops every robot on the server. |
| `/robot status [name]` | Position, facing, queue length and run id. |
| `/robot move <dir> <blocks> [name]` | Moves 1 to 64 blocks. `<dir>` is forward, back, left, right, up or down. Stops early at a solid block. |
| `/robot turn <left\|right> [name]` | Turns 90 degrees. |
| `/robot place <dir> [name]` | Places the block from the selected slot. |
| `/robot destroy <dir> [name]` | Breaks the block; the drops go into the robot. Bedrock and protected blocks stay. |
| `/robot inspect <dir> [name]` | The block's number (`id \| data << 16`), `-1` for a block not in the table. |
| `/robot detect <dir> [name]` | `true` if the block would stop a move. |
| `/robot slot <1-27> [name]` | Selects a slot. |
| `/robot give <block> <count> [slot] [name]` | Gives items. Only in creative mode, with the game rule `halisoncode:free_items` or with the permission `halisoncode.free_items`. `<block>` is a name such as `oak_planks` or `iron_ingot`, or a block number. |
| `/robot inventory [name]` | Opens the robot's inventory when you are close; otherwise lists the items. |
| `/robot collect [name]` | Picks up items within one block. |
| `/robot drop <dir> [name]` | Drops everything. |
| `/robot assist <true\|false> [name]` | Place on move: each step places the selected block where the robot was. |
| `/robot gravity <true\|false> [name]` | Gravity on or off. Off (the start) means the robot flies. Saved on the robot. |
| `/robot sethome [name]` | Saves where the robot stands as its home. |
| `/robot come [name]` | The robot walks to you, or jumps to you when there is no path. Same dimension and loaded chunks only. |
| `/robot camera [name]`, `/robot camera follow [name]`, `/robot camera off` | Looks through the robot's eyes, or from behind it. Also keys under Controls, Halison Code (no key at first). `halisoncode:robot_camera` decides who may. |
| `/robot skin <workshop\|industrial\|mint> [name]` | The robot's skin. |
| `/robot color <RRGGBB\|reset> [name]` | The colour of the main panels, or back to the skin's own. |
| `/robot animation <look_left\|look_right\|look_up\|look_down\|wave\|idle> [name]` | A short gesture; `idle` ends it. |

All `/robot` commands except `spawn`, `stop`, `status` and `inventory` go through the same checks and queue as the
editor's commands. Right-clicking a robot opens its inventory, for its owner and ops only.

## Game rules

Set with `/gamerule` (ops) or on the Edit Game Rules screen when a world is made. They are saved with the world.

| Game rule | Values | Default | What it does |
|---|---|---|---|
| `halisoncode:max_robots` | 1 to 16 | 1 | How many robots each player may have. 1 is the default robot without a name. |
| `halisoncode:free_items` | `true`, `false` | `false` | Robots get items free (`agent.setItem`, `/robot give`, building without items) also outside creative mode. |
| `halisoncode:robot_camera` | `owner`, `ops`, `off` | `owner` | Who may use the robot camera: every player through their own robot, only ops, or nobody. On NeoForge 26.3 the Edit Game Rules screen shows a text field; on NeoForge 1.21.1 use the command. |
| `halisoncode:pause_on_lost_focus` | `true`, `false` | `true` | `false` keeps the game of the player who runs the world (singleplayer, or opened to LAN) from pausing when its window loses focus. Escape still pauses. |
