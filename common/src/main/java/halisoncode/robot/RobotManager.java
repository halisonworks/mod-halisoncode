package halisoncode.robot;

import halisoncode.McCompat;
import halisoncode.HalisonCode;
import halisoncode.Limits;
import halisoncode.Messages;
import halisoncode.Permission;
import halisoncode.mixin.ItemEntityAccessor;
import halisoncode.platform.Platform;
import com.mojang.authlib.GameProfile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server side owner of all robots: per player a default robot and, up to the game rule halisoncode:max_robots, named
 * robots (issue #140); per-robot queues, and the tick executor. Server thread only.
 */
public final class RobotManager {
    // The limits per tick are the server's (issue #52, Limits). By the defaults a radius-32 search that finds nothing takes
    // about 34 ticks, a path search that hits RobotPath.MAX_NODES 8, and a full 32768-block fill 128.
    /** What one measuring command takes of the scan budget at once, at most: agent.measureGap looks both ways. */
    private static final int MIN_SCAN = 2 * RobotCommand.MAX_MEASURE;
    /** BlockCategory in the editor, in order (RobotCommand.CATEGORIES). c:ores is the convention tag of Fabric and NeoForge, so modded ores match too. */
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final List<TagKey<Block>> CATEGORIES = List.of(BlockTags.LOGS, BlockTags.LEAVES, ORES,
            BlockTags.PLANKS, BlockTags.WOOL, BlockTags.FLOWERS);
    /** Liquid in the editor: None 0, Water 1, Lava 2. */
    private static final int WATER = 1, LAVA = 2;
    /** EntityKind.Mob in the editor. */
    private static final int MOB = 0;
    /** agent.isSafe (issue #161): a fall onto the ground over at most this many free blocks is safe. */
    public static final int SAFE_DROP = 3;

    private static final Map<RobotLinks.Key, RobotEntity> LOADED = new LinkedHashMap<>();
    private static final Map<RobotLinks.Key, RobotQueue> QUEUES = new HashMap<>();
    /** The run id each owner's robots share (issue #141). */
    private static final Map<UUID, RobotQueue.Run> RUNS = new HashMap<>();
    private static final List<RobotEntity> STALE = new ArrayList<>();
    private static int roundRobin;
    /** The player's part of the tick, while their robots act; what all robots used, for {@link TickCost}. */
    private static long deadline;
    private static int scanned, pathNodes;
    private static int scanLeft;
    private static int pathLeft;
    private static int buildLeft;
    private static TickCost lastTick = new TickCost(0, 0, 0, 0);

    /**
     * What the last server tick cost the robots (issues #52 and #138): the time spent in the executor and how much of
     * each shared budget it used. The performance GameTest reads it; a server config or command could show it later.
     */
    public record TickCost(long nanos, int actions, int scanned, int pathNodes) {
    }

    public static TickCost lastTick() {
        return lastTick;
    }

    private RobotManager() {
    }

    /** Common hook for the loader's entity load event. */
    public static void entityLoaded(Entity entity, ServerLevel level) {
        if (entity instanceof RobotEntity robot) onLoad(robot, level);
    }

    /** Common hook for the loader's entity unload event. */
    public static void entityUnloaded(Entity entity) {
        if (entity instanceof RobotEntity robot && robot.key() != null && LOADED.remove(robot.key(), robot)) {
            queue(robot.key()).clear(Result.Code.UNLOADED, "robot unloaded");
        }
    }

    public static void serverStopped() {
        for (UUID owner : new HashSet<>(RUN_INFO.keySet())) endRun(owner, "the world was closed");
        lastTickNanos = 0;
        LOADED.clear();
        QUEUES.clear();
        RUNS.clear();
        STALE.clear();
    }

    private static void onLoad(RobotEntity robot, ServerLevel level) {
        UUID owner = robot.owner();
        // Which robot this is comes from robots.dat, so a rename while unloaded is no problem. A robot the owner has
        // replaced (respawned while this one was unloaded) or removed is no one's robot any more and goes.
        RobotLinks.Key key = owner == null ? null : RobotLinks.get(level.getServer()).key(robot.getUUID());
        if (key == null || !key.owner().equals(owner)) {
            STALE.add(robot);
            return;
        }
        robot.setKey(key);
        showDefaultName(RobotLinks.get(level.getServer()), robot);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) robot.setOwnerName(player.getGameProfile().name());
        robot.rememberAppearance();
        LOADED.put(key, robot);
        HalisonCode.LOG.detail("spawn: {} was loaded into the world at {} in {}.", label(level.getServer(), key), fmt(robot.position()), dim(level));
    }

    // Issue #293: the default robot's name is in robots.dat. A robot from a world saved before that has it only as its
    // tag, which is kept as the name when it is one and no other robot of the owner has it (else it stays only a tag).
    private static void showDefaultName(RobotLinks links, RobotEntity robot) {
        RobotLinks.Key key = robot.key();
        if (!key.name().isEmpty()) return;
        String name = links.defaultName(key.owner());
        if (!name.isEmpty()) {
            robot.showName(name);
        } else if (robot.getCustomName() != null) {
            String tag = robot.getCustomName().getString();
            if (nameProblem(tag) == null && links.sameName(key.owner(), tag) == null) links.setDefaultName(key.owner(), tag);
        }
    }

    // "Player303's robot "Miner"" or "Player303's default robot".
    static String label(MinecraftServer server, RobotLinks.Key key) {
        ServerPlayer p = server.getPlayerList().getPlayer(key.owner());
        return (p != null ? halisoncode.log.Log.person(p.getGameProfile().name()) : key.owner().toString()) + "'s "
                + (key.name().isEmpty() ? "default robot" : "robot \"" + key.name() + "\"");
    }

    static String label(RobotEntity robot) {
        return robot.key() == null ? "a robot" : label(robot.level().getServer(), robot.key());
    }

    private static String at(BlockPos p) {
        return "x=" + p.getX() + " y=" + p.getY() + " z=" + p.getZ();
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    private static String fmt(Vec3 v) {
        return String.format(java.util.Locale.ROOT, "x=%.2f y=%.2f z=%.2f", v.x, v.y, v.z);
    }

    private static String dim(ServerLevel level) {
        return McCompat.dimension(level).getPath();
    }

    private static RobotLinks.Key key(UUID owner) {
        return new RobotLinks.Key(owner, "");
    }

    /** The queue of the owner's default robot. */
    public static RobotQueue queue(UUID owner) {
        return queue(key(owner));
    }

    public static RobotQueue queue(RobotLinks.Key key) {
        return QUEUES.computeIfAbsent(key, k -> {
            RobotQueue queue = new RobotQueue(RUNS.computeIfAbsent(k.owner(), o -> new RobotQueue.Run()));
            queue.refused = () -> {
                RobotEntity robot = LOADED.get(k);
                if (robot != null) RobotEffects.send(robot, RobotEffects.ERROR, robot.blockPosition());
            };
            return queue;
        });
    }

    /** The owner's default robot, or null when it is not loaded. */
    public static RobotEntity robot(UUID owner) {
        return LOADED.get(key(owner));
    }

    public static RobotEntity robot(RobotLinks.Key key) {
        return LOADED.get(key);
    }

    /** The owner's loaded robots, the default robot first if it is loaded. */
    public static List<RobotEntity> robots(UUID owner) {
        List<RobotEntity> robots = new ArrayList<>();
        for (Map.Entry<RobotLinks.Key, RobotEntity> e : LOADED.entrySet()) {
            if (e.getKey().owner().equals(owner)) robots.add(e.getKey().name().isEmpty() ? 0 : robots.size(), e.getValue());
        }
        return robots;
    }

    /** Spawns the owner's default robot, or moves the existing one, and ends any running program. */
    public static RobotEntity spawn(ServerLevel level, UUID owner, Vec3 pos, Direction facing) {
        return spawn(level, key(owner), pos, facing);
    }

    /**
     * Spawns one of the owner's robots, or moves it if it exists, and ends any running program of that owner (the
     * owner's robots share one run). The limit (halisoncode:max_robots) is checked by {@link #spawnRefusal} first.
     */
    public static RobotEntity spawn(ServerLevel level, RobotLinks.Key key, Vec3 pos, Direction facing) {
        stop(key.owner(), "/robot spawn put a robot in a new place");
        RobotEntity robot = LOADED.get(key);
        if (robot != null && robot.level() == level) {
            String before = fmt(robot.position());
            robot.snapTo(pos, facing.toYRot(), 0);
            spawned(level, key, robot, "Moved from " + before, pos, facing);
            return robot;
        }
        RobotEntity old = robot;
        robot = new RobotEntity(HalisonCode.ROBOT.get(), level);
        robot.setOwner(key.owner());
        robot.setKey(key);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(key.owner());
        if (player != null) robot.setOwnerName(player.getGameProfile().name());
        robot.snapTo(pos, facing.toYRot(), 0);
        RobotLinks links = RobotLinks.get(level.getServer());
        showDefaultName(links, robot);
        RobotLinks.Appearance previousAppearance = links.previousAppearance(key);
        if (old == null) {
            robot.setSkin(previousAppearance.skin());
            robot.setPrimaryColor(previousAppearance.color());
        }
        if (old != null) {
            // Another dimension: the items move to the new robot, so the old one drops nothing when it is discarded.
            for (int i = 0; i < RobotCommand.SLOTS; i++) robot.inventory().setItem(i, old.inventory().removeItemNoUpdate(i));
            robot.setSkin(old.skin());
            robot.setPrimaryColor(old.primaryColor());
            robot.setCustomName(old.getCustomName());
            robot.setCustomNameVisible(old.isCustomNameVisible());
            old.discard();
        } else if (links.robot(key) != null) {
            // The old robot is unloaded and keeps the only copy of its items until its chunk loads again (see tick).
            links.orphan(links.robot(key));
        }
        links.set(key, robot.getUUID());
        robot.rememberAppearance();
        level.addFreshEntity(robot);
        spawned(level, key, robot, "Created", pos, facing);
        return robot;
    }

    private static void spawned(ServerLevel level, RobotLinks.Key key, RobotEntity robot, String how, Vec3 pos, Direction facing) {
        HalisonCode.LOG.detail("spawn: {}: {} to {}, facing {}, in {}.", label(level.getServer(), key), how, fmt(pos), facing.getName(), dim(level));
        if (robot.position().distanceTo(pos) > 0.5) HalisonCode.LOG.warn("spawn: {} stands at {}, not at {} where it was put.", label(level.getServer(), key), fmt(robot.position()), fmt(pos));
    }

    /**
     * Why the owner may not spawn a robot called {@code name} now, or null. The default robot (empty name) and a robot
     * the owner already has are always allowed (spawn moves it). A new named robot needs a free name and room under
     * halisoncode:max_robots, which counts the default robot as one. Lowering the rule never removes robots.
     */
    public static Component spawnRefusal(MinecraftServer server, UUID owner, String name) {
        RobotLinks links = RobotLinks.get(server);
        if (name.isEmpty() || links.robot(new RobotLinks.Key(owner, name)) != null) return null;
        Component problem = nameProblem(name);
        if (problem != null) return problem;
        String same = links.sameName(owner, name);
        if (same != null) return Messages.of("message.halisoncode.name.taken", same);
        int max = HalisonCode.maxRobots(server);
        if (links.names(owner).size() + 1 < max) return null;
        return max == 1 ? Messages.of("commands.halisoncode.spawn.one", "halisoncode:max_robots", "/robot spawn")
                : Messages.of("commands.halisoncode.spawn.limit", max, max - 1, "halisoncode:max_robots", "/robot remove");
    }

    // The same rules as RobotCommand.nameProblem, in the player's language. A new name may not be agent either (issue
    // #293); that is not in RobotCommand, which also checks commands for robots an older world already has.
    private static Component nameProblem(String name) {
        if (name.equalsIgnoreCase(RobotLinks.AGENT)) return Messages.of("message.halisoncode.name.reserved", name);
        if (RobotCommand.nameProblem(name) == null) return null;
        return RobotCommand.reservedName(name) ? Messages.of("message.halisoncode.name.reserved", name) : Messages.of("message.halisoncode.name.invalid", RobotCommand.MAX_NAME);
    }

    /** The name of the block with this code, or null when Halison Code has no number for it. */
    static Component blockName(int code) {
        String id = BlockIds.toModern(code);
        return id == null ? null : McCompat.block(Identifier.parse(id)).getName();
    }

    /** The name of the item with this code. */
    static Component itemName(int code) {
        return new ItemStack(item(code)).getHoverName();
    }

    /** Queues a validated command for the owner's default robot; {@code done} gets the result on a later tick. */
    public static void submit(UUID owner, long run, RobotCommand command, Consumer<Result> done) throws RobotException {
        submit(key(owner), run, command, done);
    }

    /** Queues a validated command for one of the owner's robots; {@code done} gets the result on a later tick. */
    public static void submit(RobotLinks.Key key, long run, RobotCommand command, Consumer<Result> done) throws RobotException {
        submit(key, run, -1, command, done);
    }

    /** As {@link #submit(RobotLinks.Key, long, RobotCommand, Consumer)}; {@code id} is the command's number in the run, for the log. */
    public static void submit(RobotLinks.Key key, long run, long id, RobotCommand command, Consumer<Result> done) throws RobotException {
        if (!LOADED.containsKey(key)) {
            throw new RobotException(Result.Code.UNLOADED, key.name().isEmpty() ? "robot is not loaded (or not spawned: /robot spawn)"
                    : "robot " + key.name() + " is not loaded (or not spawned: /robot spawn " + key.name() + ", see /robot list)");
        }
        queue(key).submit(run, id, command, HalisonCode.limits().queueLength(), done);
    }

    /** New editor run for all of the owner's robots: their queues are dropped and the shared run id is new. */
    public static long start(UUID owner) {
        for (RobotQueue queue : queues(owner)) {
            queue.clear(Result.Code.STOPPED, "a new run started");
            queue.resume();
        }
        return queue(owner).start();
    }

    /** Stops all of the owner's robots and ends the run they share, so its later commands get {@code STOPPED}. */
    public static void stop(UUID owner) {
        stop(owner, "stopped");
    }

    /** As {@link #stop(UUID)}; {@code why} goes to the log. */
    public static void stop(UUID owner, String why) {
        endRun(owner, why);
        for (RobotQueue queue : queues(owner)) queue.stop();
        queue(owner).stop();
    }

    /** The queues of the owner's robots that have had commands. */
    public static List<RobotQueue> queues(UUID owner) {
        List<RobotQueue> queues = new ArrayList<>();
        QUEUES.forEach((key, queue) -> {
            if (key.owner().equals(owner)) queues.add(queue);
        });
        return queues;
    }

    /**
     * Renames one of the owner's robots (issue #151), from /robot rename or a name tag. A named robot changes its
     * name everywhere (robots.get, the commands, the tag) and its queued commands stop; the default robot stays the
     * default robot (agent in programs) and only its name for the player and the commands is new. Answers why not, or
     * null. Names are unique per owner.
     */
    public static Component rename(MinecraftServer server, RobotLinks.Key key, String name) {
        RobotLinks links = RobotLinks.get(server);
        UUID id = links.robot(key);
        if (id == null) return Messages.absent(server, key.owner(), key.name());
        Component problem = nameProblem(name);
        if (problem != null) return problem;
        String same = links.sameName(key.owner(), name, key);
        if (same != null) return Messages.of("message.halisoncode.name.taken", same);
        RobotEntity robot = LOADED.get(key);
        if (key.name().isEmpty()) {
            links.setDefaultName(key.owner(), name);
            if (robot != null) {
                robot.showName(name);
                robot.setCustomNameVisible(true);
            }
            return null;
        }
        RobotLinks.Key renamed = new RobotLinks.Key(key.owner(), name);
        RobotQueue queue = QUEUES.remove(key);
        if (queue != null) queue.clear(Result.Code.STOPPED, "robot " + key.name() + " was renamed to " + name);
        links.remove(key);
        links.set(renamed, id);
        if (robot != null) {
            LOADED.remove(key);
            LOADED.put(renamed, robot);
            robot.setKey(renamed);
        }
        return null;
    }

    /** A vanilla name tag used on a robot (issue #151): the owner or an op renames it, and the tag is used up. */
    static void nameTag(ServerPlayer player, RobotEntity robot, ItemStack stack) {
        if (!mayOpen(player, robot)) {
            Messages.actionBar(player, Messages.of("message.halisoncode.not_yours"));
            return;
        }
        if (robot.key() == null) return; // a stale copy, removed this tick
        Component refusal = rename(player.level().getServer(), robot.key(), stack.get(DataComponents.CUSTOM_NAME).getString());
        if (refusal != null) {
            Messages.actionBar(player, refusal);
            return;
        }
        if (!player.hasInfiniteMaterials()) stack.shrink(1);
    }

    /**
     * /robot remove (issue #140): one of the owner's robots goes, and every item it holds moves to the player,
     * or drops at the player's feet when the inventory is full (the way a closed crafting grid gives items back), so
     * nothing is lost. Only a loaded robot: an unloaded one holds the only copy of its items. Removing the default robot
     * (issue #324) also ends the owner's program; its name and look wait for the next one. Answers why not, or null.
     */
    public static Component remove(ServerPlayer player, String name) {
        RobotLinks.Key key = new RobotLinks.Key(player.getUUID(), name);
        RobotLinks links = RobotLinks.get(player.level().getServer());
        if (links.robot(key) == null) return Messages.absent(player.level().getServer(), key.owner(), name);
        RobotEntity robot = LOADED.get(key);
        if (robot == null) return Messages.absent(player.level().getServer(), key.owner(), name);
        if (name.isEmpty()) {
            links.setAppearance(key.owner(), robot.skin(), robot.primaryColor());
            for (RobotQueue queue : queues(key.owner())) queue.clear(Result.Code.STOPPED, "the default robot was removed with /robot remove");
            stop(key.owner(), "/robot remove");
        } else {
            RobotQueue queue = QUEUES.remove(key);
            if (queue != null) queue.clear(Result.Code.STOPPED, "robot " + name + " was removed");
        }
        for (int i = 0; i < robot.inventory().getContainerSize(); i++) {
            ItemStack stack = robot.inventory().removeItemNoUpdate(i);
            if (!stack.isEmpty()) McCompat.giveBack(player, stack);
        }
        links.remove(key);
        LOADED.remove(key);
        robot.discard(); // empty now, so it drops nothing
        return null;
    }

    /**
     * Opens the robot's inventory screen for its owner or an op; anyone else gets "not your robot". The screen works
     * on the robot's own container, so a running program and the screen share the same items.
     */
    public static boolean openInventory(ServerPlayer player, RobotEntity robot) {
        if (!mayOpen(player, robot)) {
            Messages.actionBar(player, Messages.of("message.halisoncode.not_yours"));
            return false;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new RobotMenu(id, inventory, robot), robot.getDisplayName()));
        return true;
    }

    /** The owner or an op (game master level). Never a FakePlayer: ours carries the owner's UUID. */
    static boolean mayOpen(Player player, RobotEntity robot) {
        return !Platform.INSTANCE.isFakePlayer(player) && player instanceof ServerPlayer p
                && (p.getUUID().equals(robot.owner()) || McCompat.isGamemaster(p));
    }

    public static void stopAll() {
        for (UUID owner : new HashSet<>(RUN_INFO.keySet())) endRun(owner, "/robot stop all");
        QUEUES.values().forEach(RobotQueue::stop);
    }

    private record RunInfo(String name, long nanos) {
    }

    /** For the report: robots in the loaded world. */
    public static int loadedCount() {
        return LOADED.size();
    }

    /** For the report: programs started from an editor and not ended. */
    public static int runCount() {
        return RUN_INFO.size();
    }

    private static final Map<UUID, RunInfo> RUN_INFO = new HashMap<>();
    private static long lastTickNanos;

    /** Log only: the editor started a program for the owner's robots. */
    public static void runStarted(UUID owner, String name) {
        endRun(owner, "a new run started");
        RUN_INFO.put(owner, new RunInfo(name, System.nanoTime()));
        List<String> names = new ArrayList<>();
        for (RobotEntity r : robots(owner)) names.add(r.key() == null || r.key().name().isEmpty() ? "default robot" : "robot \"" + r.key().name() + "\"");
        HalisonCode.LOG.info("run: {}'s program started (run {}) with {}.", name, queue(owner).run(), names.isEmpty() ? "no robot loaded" : String.join(", ", names));
    }

    private static void endRun(UUID owner, String why) {
        RunInfo info = RUN_INFO.remove(owner);
        if (info != null) HalisonCode.LOG.info("run: {}'s program ended after {} s: {}.", info.name(),
                String.format(java.util.Locale.ROOT, "%.1f", (System.nanoTime() - info.nanos()) / 1e9), why);
    }

    public static void tick(MinecraftServer server) {
        long start = System.nanoTime();
        // The integrated server runs no tick while the pause menu is open or the window has lost focus, so a long gap
        // between two ticks is the pause; it is logged when the game goes on.
        if (lastTickNanos != 0 && start - lastTickNanos > 2_000_000_000L) pauseEnded((start - lastTickNanos) / 1e9);
        lastTickNanos = start;
        int actions = execute(server);
        lastTick = new TickCost(System.nanoTime() - start, actions, scanned, pathNodes);
    }

    private static void pauseEnded(double seconds) {
        for (RobotEntity robot : LOADED.values()) {
            RobotQueue queue = QUEUES.get(robot.key());
            RobotQueue.Entry entry = queue == null ? null : queue.head();
            if (entry != null) HalisonCode.LOG.detail("pause: Minecraft was paused (or stalled) for {} s; {} was waiting to run {}. It goes on now.",
                    String.format(java.util.Locale.ROOT, "%.1f", seconds), label(robot), entry.command.op().name());
        }
    }

    /**
     * Runs one tick of every robot's queue and answers the number of actions used. The players whose robots have work
     * share the tick's budgets and time equally, whatever number of robots they have (issue #52): each in turn gets an
     * equal part of what is left, so what one does not use goes to the next. Who is first changes every tick.
     */
    private static int execute(MinecraftServer server) {
        scanned = pathNodes = 0;
        for (RobotEntity robot : STALE) {
            // Only an orphan still owns its items and drops them. Any other stale copy (an older save of a robot that
            // moved on, a structure-block copy) holds items that exist elsewhere, so dropping them would duplicate.
            if (!RobotLinks.get(server).takeOrphan(robot.getUUID())) robot.inventory().clearContent();
            robot.discard();
        }
        STALE.clear();
        Map<UUID, List<RobotEntity>> ready = new LinkedHashMap<>();
        for (RobotEntity robot : LOADED.values()) {
            RobotQueue.Entry entry = next(robot);
            if (entry == null) continue;
            if (entry.wait > 0) entry.wait--;
            else ready.computeIfAbsent(robot.owner(), o -> new ArrayList<>()).add(robot);
        }
        if (ready.isEmpty()) return 0;
        Limits limits = HalisonCode.limits();
        List<List<RobotEntity>> players = new ArrayList<>(ready.values());
        int n = players.size(), turn = roundRobin++;
        int actions = limits.actionsPerTick(), scan = limits.scanBlocksPerTick(), path = limits.pathNodesPerTick(), build = limits.buildBlocksPerTick();
        long now = System.nanoTime(), end = now + limits.millisPerTick() * 1_000_000L;
        for (int i = 0; i < n && (i == 0 || now < end); i++, now = System.nanoTime()) {
            List<RobotEntity> robots = players.get(Math.floorMod(turn + i, n));
            int m = robots.size(), first = Math.floorMod(turn / n, m);
            int mine = Limits.share(actions, n - i, 1);
            int scanShare = scanLeft = Limits.share(scan, n - i, MIN_SCAN);
            int pathShare = pathLeft = Limits.share(path, n - i, 1);
            int buildShare = buildLeft = Limits.share(build, n - i, 1);
            deadline = now + (end - now) / (n - i);
            for (int j = 0; j < Math.min(m, mine) && (j == 0 || System.nanoTime() < deadline); j++) {
                RobotEntity robot = robots.get((first + j) % m);
                RobotQueue.Entry entry = next(robot);
                // Gone when an earlier robot's action this tick stopped the program or removed the robot.
                if (entry == null || robot.isRemoved()) continue;
                actions--;
                step(robot, QUEUES.get(robot.key()), entry);
            }
            scan -= scanShare - scanLeft;
            path -= pathShare - pathLeft;
            build -= buildShare - buildLeft;
        }
        scanned = limits.scanBlocksPerTick() - scan;
        pathNodes = limits.pathNodesPerTick() - path;
        return limits.actionsPerTick() - actions;
    }

    // What the robot is to do now, or null.
    private static RobotQueue.Entry next(RobotEntity robot) {
        RobotQueue queue = QUEUES.get(robot.key());
        return queue == null || queue.paused() ? null : queue.head();
    }

    // One action step: one block of a move, or a whole other action.
    private static void step(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry) {
        RobotCommand command = entry.command;
        if (!entry.started) {
            entry.started = true;
            if (HalisonCode.LOG.detailed()) HalisonCode.LOG.detail("robot: {} starts {} {}", label(robot), command.op(), entry.id < 0 ? "(a /robot command)" : "(run " + queue.run() + " command " + entry.id + ")");
        }
        // shortcut: a stop mid-fall leaves the robot in the air until its next sideways move; fine while falls are short.
        if (entry.falling) {
            if (fall(robot)) return;
            entry.falling = false;
            if (entry.landed != null) {
                queue.finish(entry.landed);
                return;
            }
        }
        if (command.op() == RobotCommand.Op.FIND_BLOCK || command.op() == RobotCommand.Op.FIND_CATEGORY) {
            find(robot, queue, entry);
            return;
        }
        if (command.op() == RobotCommand.Op.MOVE_TO || command.op() == RobotCommand.Op.COME) {
            navigate(robot, queue, entry, new BlockPos(command.a(), command.b(), command.c()));
            return;
        }
        if (command.op() == RobotCommand.Op.GO_TO_ROBOT) {
            goToRobot(robot, queue, entry);
            return;
        }
        if (command.op() == RobotCommand.Op.FOLLOW_PATH) {
            follow(robot, queue, entry);
            return;
        }
        if (command.op() == RobotCommand.Op.GO_HOME) {
            // Issue #78: walks only, never across dimensions; no home or a home elsewhere answers false.
            GlobalPos home = robot.home();
            if (home == null || !home.dimension().equals(robot.level().dimension())) queue.finish(Result.ok(false));
            else navigate(robot, queue, entry, home.pos());
            return;
        }
        if (command.op().compareTo(RobotCommand.Op.MEASURE) >= 0 && command.op().compareTo(RobotCommand.Op.FIND_BOUNDS) <= 0) {
            try {
                measure(robot, queue, entry);
            } catch (RobotException e) {
                queue.finish(e.result());
            }
            return;
        }
        if (command.op().compareTo(RobotCommand.Op.FILL) >= 0) {
            try {
                build(robot, queue, entry);
            } catch (RobotException e) {
                buildEnd(robot, queue, entry, e.getMessage(), e.result());
            }
            return;
        }
        if (command.op() != RobotCommand.Op.MOVE) {
            Result result = act(robot, command);
            // Gravity mode: the robot drops into the hole it dug below itself.
            if (command.op() == RobotCommand.Op.DESTROY && command.direction() == RobotCommand.DOWN
                    && robot.gravityMode() && canFall(robot)) {
                entry.falling = true;
                entry.landed = result;
            } else {
                queue.finish(result);
            }
            return;
        }
        moveOne(robot, queue, entry, relative(robot.getDirection(), command.direction()), entry.step + 1 >= command.blocks(), Result.ok(false));
    }

    private static boolean isPath(RobotQueue.Entry entry) {
        return switch (entry.command.op()) {
            case MOVE_TO, GO_TO_ROBOT, COME, GO_HOME, FOLLOW_PATH -> true;
            default -> false;
        };
    }

    private static void pathStart(RobotEntity robot, RobotQueue.Entry entry, String to) {
        entry.from = robot.blockPosition();
        if (HalisonCode.LOG.detailed()) HalisonCode.LOG.detail("path: {} starts {} at {} (standing on {}){}.", label(robot), entry.command.op().name(), at(entry.from),
                name(robot.level().getBlockState(entry.from.below()).getBlock()), to);
    }

    private static void pathEnd(RobotEntity robot, RobotQueue.Entry entry, String why) {
        BlockPos now = robot.blockPosition();
        if (HalisonCode.LOG.detailed()) HalisonCode.LOG.detail("path: {} {} ended after {} steps at {}, {} blocks from where it started: {}.", label(robot), entry.command.op().name(), entry.step,
                at(now), entry.from == null ? 0 : entry.from.distManhattan(now), why);
    }

    /**
     * One block of a move in dir, the same for agent.move and each step of agent.moveTo and followPath: answers
     * {@code blocked} when blocked, true after the last block, else waits {@link Limits#ticksPerBlock} ticks (and falls
     * first in gravity mode).
     */
    private static void moveOne(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry, Direction dir, boolean last, Result blocked) {
        Level level = robot.level();
        boolean sideways = dir.getAxis().isHorizontal();
        BlockPos from = robot.blockPosition();
        BlockPos target = from.relative(dir);
        if (!level.isLoaded(target)) {
            if (isPath(entry)) pathEnd(robot, entry, "the next block is in an unloaded chunk");
            queue.finish(Result.error(Result.Code.UNLOADED, "next block is in an unloaded chunk"));
            return;
        }
        // Gravity mode steps up a 1-block bump when there is head room above both blocks; a higher wall stops it.
        if (robot.gravityMode() && sideways && blocked(level, target) && !blocked(level, from.above()) && !blocked(level, target.above())) {
            target = target.above();
        }
        if (blocked(level, target)) {
            if (isPath(entry)) pathEnd(robot, entry, "the next block " + at(target) + " is blocked by " + name(level.getBlockState(target).getBlock()));
            queue.finish(blocked);
            return;
        }
        robot.setPos(Vec3.atBottomCenterOf(target));
        RobotEffects.send(robot, RobotEffects.MOVE, target);
        // PLACE_ON_MOVE: an empty slot or a failed place does not stop the move.
        if (robot.placeOnMove()) place(robot, from, dir.getOpposite());
        entry.step++;
        // Gravity mode falls after a sideways step, never after up, so "move up, place down" pillars still work.
        if (robot.gravityMode() && sideways && canFall(robot)) {
            entry.falling = true;
            if (last) entry.landed = Result.ok(true);
        } else if (last) {
            queue.finish(Result.ok(true));
        } else {
            entry.wait = HalisonCode.limits().ticksPerBlock() - 1;
        }
    }

    /**
     * Checks up to the tick's scan budget of blocks in the cube around the robot, nearest first by block steps
     * (Manhattan distance), and answers [x, y, z] for the first match or no value when the cube holds none.
     * Unloaded chunks are skipped, never loaded. Stop or unload drops the entry, which ends the scan.
     */
    private static void find(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry) {
        RobotCommand command = entry.command;
        int r = command.b();
        //? if >=26.3 {
        if (entry.scan == null) entry.scan = BlockPos.withinBoxByManhattanDistance(robot.blockPosition(), r, r, r).iterator();
        //?} else
        /*if (entry.scan == null) entry.scan = BlockPos.withinManhattan(robot.blockPosition(), r, r, r).iterator();*/
        Iterator<BlockPos> scan = entry.scan;
        Level level = robot.level();
        Block block = command.op() == RobotCommand.Op.FIND_BLOCK
                ? McCompat.block(Identifier.parse(BlockIds.toModern(command.a()))) : null;
        TagKey<Block> tag = block == null ? CATEGORIES.get(command.a()) : null;
        for (; scanLeft > 0 && scan.hasNext(); scanLeft--) {
            BlockPos pos = scan.next();
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (block != null ? state.is(block) : state.is(tag)) {
                queue.finish(Result.ok(List.of(pos.getX(), pos.getY(), pos.getZ())));
                return;
            }
        }
        if (!scan.hasNext()) queue.finish(Result.ok(null));
    }

    /**
     * Measuring (issues #127 to #132): reads blocks without moving the robot, in loaded chunks only (an unloaded block
     * answers {@code UNLOADED}), costed against the shared scan budget like findNearest. A line read (at most 128
     * blocks) waits for a tick with enough budget left, then runs at once; a box read goes on over later ticks.
     * measure, measureRun and measureGap use the move rule (a block with a collision shape stops them); the box reads
     * and findBounds count every block that is not air or replaceable (grass, snow layer, water). Reads at a given
     * position (groundHeight, isAreaFree, countSolid) must lie within {@link RobotCommand#MAX_FIND_RADIUS} of the robot
     * on each axis, else {@code OUT_OF_RANGE}, so a program on a server cannot probe hidden blocks far away. findBounds
     * stays within that range by its own cap: its box is under {@link RobotCommand#MAX_AREA} wide around the block in front.
     */
    private static void measure(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry) throws RobotException {
        RobotCommand c = entry.command;
        Level level = robot.level();
        BlockPos at = robot.blockPosition();
        Direction facing = robot.getDirection();
        switch (c.op()) {
            case MEASURE -> {
                if (reserve(c.b())) queue.finish(Result.ok(free(level, at, relative(facing, c.a()), c.b())));
            }
            case MEASURE_RUN -> {
                if (!reserve(c.c())) return;
                BlockPos start = at.relative(relative(facing, c.a()));
                Direction along = relative(facing, c.b());
                int n = 0;
                while (n < c.c() && blocked(level, loaded(level, start.relative(along, n)))) n++;
                queue.finish(Result.ok(n));
            }
            case MEASURE_GAP -> {
                if (!reserve(2 * c.b())) return;
                Direction.Axis axis = Direction.Axis.values()[c.a()];
                int plus = free(level, at, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE), c.b());
                int minus = free(level, at, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE), c.b());
                queue.finish(Result.ok(plus == c.b() || minus == c.b() ? c.b() : Math.min(c.b(), plus + minus + 1)));
            }
            case GROUND_HEIGHT -> {
                near(at, c.a(), at.getY(), c.b());
                if (reserve(1)) queue.finish(Result.ok(ground(level, c.a(), c.b())));
            }
            case HEIGHT_PROFILE -> {
                if (!reserve(c.b())) return;
                Direction dir = relative(facing, c.a());
                List<Integer> heights = new ArrayList<>();
                for (int i = 1; i <= c.b(); i++) heights.add(ground(level, at.getX() + dir.getStepX() * i, at.getZ() + dir.getStepZ() * i));
                queue.finish(Result.ok(heights));
            }
            case IS_AREA_FREE, COUNT_SOLID -> {
                if (entry.scan == null) {
                    near(at, c.a(), c.b(), c.c());
                    near(at, c.d(), c.e(), c.f());
                    entry.scan = BlockPos.betweenClosed(c.a(), c.b(), c.c(), c.d(), c.e(), c.f()).iterator();
                }
                boolean free = c.op() == RobotCommand.Op.IS_AREA_FREE;
                for (; scanLeft > 0 && entry.scan.hasNext(); scanLeft--) {
                    if (!solid(level, loaded(level, entry.scan.next()))) continue;
                    if (free) {
                        queue.finish(Result.ok(false));
                        return;
                    }
                    entry.step++;
                }
                if (!entry.scan.hasNext()) queue.finish(Result.ok(free ? (Object) true : entry.step));
            }
            case FIND_BOUNDS -> bounds(level, queue, entry, at.relative(facing), c.b());
            default -> throw new IllegalStateException("not a measuring op " + c.op());
        }
    }

    // The owner's rule for reads at a given position: within findNearest's radius of the robot on every axis.
    private static void near(BlockPos robot, int x, int y, int z) throws RobotException {
        int r = RobotCommand.MAX_FIND_RADIUS;
        if (Math.abs(x - robot.getX()) > r || Math.abs(y - robot.getY()) > r || Math.abs(z - robot.getZ()) > r) {
            throw new RobotException(Result.Code.OUT_OF_RANGE, "position " + x + " " + y + " " + z + " is more than " + r + " blocks from the robot");
        }
    }

    // Takes n blocks from this tick's scan budget, or answers false so the command waits for the next tick.
    private static boolean reserve(int n) {
        if (scanLeft < n) return false;
        scanLeft -= n;
        return true;
    }

    private static BlockPos loaded(Level level, BlockPos pos) throws RobotException {
        if (!level.isLoaded(pos) && !level.isOutsideBuildHeight(pos)) {
            throw new RobotException(Result.Code.UNLOADED, "block " + pos.toShortString() + " is in an unloaded chunk");
        }
        return pos;
    }

    // Free blocks from the robot in dir before the first one that would stop a move, or max when none does.
    private static int free(Level level, BlockPos from, Direction dir, int max) throws RobotException {
        for (int i = 1; i <= max; i++) if (blocked(level, loaded(level, from.relative(dir, i)))) return i - 1;
        return max;
    }

    // Not air and not replaceable: what a builder would have to clear first.
    private static boolean solid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }

    // The vanilla heightmap without leaves: the y of the highest block that blocks motion (water counts), below the world when there is none.
    private static int ground(Level level, int x, int z) throws RobotException {
        loaded(level, new BlockPos(x, level.getMinY(), z));
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    /**
     * findBounds (issue #132): a flood fill over the solid blocks touching the block in front (sides, not edges), as
     * long as the box stays at most max blocks on each side. Six reads per block, from the scan budget, over ticks.
     * Answers [min x, y, z, max x, y, z], or no value when the block in front is not solid. Unloaded blocks are an edge.
     */
    private static void bounds(Level level, RobotQueue queue, RobotQueue.Entry entry, BlockPos start, int max) throws RobotException {
        if (entry.fill == null) {
            if (!solid(level, loaded(level, start))) {
                queue.finish(Result.ok(null));
                return;
            }
            entry.fill = new ArrayDeque<>(List.of(start.immutable()));
            entry.seen = new HashSet<>(entry.fill);
            entry.bounds = new int[]{start.getX(), start.getY(), start.getZ(), start.getX(), start.getY(), start.getZ()};
        }
        int[] b = entry.bounds;
        while (scanLeft >= 6 && !entry.fill.isEmpty()) {
            BlockPos pos = entry.fill.poll();
            scanLeft -= 6;
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (Math.max(b[3], next.getX()) - Math.min(b[0], next.getX()) >= max || Math.max(b[4], next.getY()) - Math.min(b[1], next.getY()) >= max
                        || Math.max(b[5], next.getZ()) - Math.min(b[2], next.getZ()) >= max) continue;
                if (!level.isLoaded(next) || !solid(level, next) || !entry.seen.add(next)) continue;
                entry.fill.add(next);
                b[0] = Math.min(b[0], next.getX());
                b[1] = Math.min(b[1], next.getY());
                b[2] = Math.min(b[2], next.getZ());
                b[3] = Math.max(b[3], next.getX());
                b[4] = Math.max(b[4], next.getY());
                b[5] = Math.max(b[5], next.getZ());
            }
        }
        if (entry.fill.isEmpty()) queue.finish(Result.ok(List.of(b[0], b[1], b[2], b[3], b[4], b[5])));
    }

    /**
     * blocks.fill, shapes.line, shapes.circle, shapes.sphere (issue #90) and shapes.maze (#182). Every block of the shape must be within
     * {@link RobotCommand#MAX_FIND_RADIUS} of the robot on each axis (the measuring rule), else {@code OUT_OF_RANGE}.
     * Each block is set through the FakePlayer like agent.place (and cleared like agent.destroy, drops go into the
     * robot), so claim and protection mods apply. The blocks come from the robot's inventory, any slot, unless the
     * owner gets items free (setItem's rule, see {@link #freeItems}); with no block left the command ends there.
     * Keep only fills air and replaceable blocks. Works over ticks: at most {@link Limits#buildBlocksPerTick} blocks set or
     * cleared and the scan budget for the blocks looked at. Answers the number of blocks changed.
     */
    private static void build(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry) throws RobotException {
        RobotCommand c = entry.command;
        Level level = robot.level();
        boolean fresh = entry.scan == null;
        if (entry.scan == null) {
            int[] b = RobotShapes.box(c);
            near(robot.blockPosition(), b[0], b[1], b[2]);
            near(robot.blockPosition(), b[3], b[4], b[5]);
            entry.scan = c.op() == RobotCommand.Op.LINE
                    ? RobotShapes.line(c).stream().map(p -> new BlockPos(p[0], p[1], p[2])).iterator()
                    : BlockPos.betweenClosed(b[0], b[1], b[2], b[3], b[4], b[5]).iterator();
            if (c.op() == RobotCommand.Op.MAZE) entry.maze = RobotShapes.maze(c.d(), c.e(), c.f());
        }
        Block block = c.g() == 0 ? Blocks.AIR : McCompat.block(Identifier.parse(BlockIds.toModern(c.g())));
        Block floor = c.op() == RobotCommand.Op.MAZE ? McCompat.block(Identifier.parse(BlockIds.toModern(c.h()))) : block;
        boolean free = freeItems(robot);
        if (fresh && HalisonCode.LOG.detailed()) HalisonCode.LOG.detail("build: {} starts {} with {}; {}.", label(robot), c.op().name().toLowerCase(java.util.Locale.ROOT), name(block),
                !free ? "it builds from the robot's inventory (the owner does not get items free)"
                        : HalisonCode.freeItems(robot.level().getServer()) ? "it builds free of charge because game rule halisoncode:free_items is on"
                        : "it builds free of charge because the owner is in creative mode or has the permission halisoncode.free_items");
        boolean overwrite = c.op() != RobotCommand.Op.FILL || c.h() != RobotShapes.KEEP;
        // A block at least, then as long as this player's part of the tick's time lasts: drops that lie about make it slow.
        for (boolean first = true; buildLeft > 0 && scanLeft > 0 && entry.scan.hasNext() && (first || System.nanoTime() < deadline); scanLeft--, first = false) {
            BlockPos pos = entry.scan.next().immutable();
            entry.looked++;
            int action = switch (c.op()) {
                case LINE -> RobotShapes.SET;
                case MAZE -> RobotShapes.mazeAction(entry.maze, pos.getX() - c.a(), pos.getY() - (c.b() - 1), pos.getZ() - c.c());
                default -> RobotShapes.action(c, pos.getX(), pos.getY(), pos.getZ());
            };
            if (action == RobotShapes.SKIP) continue;
            BlockState state = level.getBlockState(loaded(level, pos));
            if (action == RobotShapes.CLEAR || block == Blocks.AIR) {
                if (!state.isAir() && !state.canBeReplaced()) {
                    buildLeft--;
                    if (destroy(robot, pos)) entry.step++;
                    else entry.refused++;
                }
                continue;
            }
            Block target = action == RobotShapes.FLOOR ? floor : block;
            if (state.is(target) || !state.canBeReplaced() && !overwrite) continue;
            buildLeft--;
            if (!state.canBeReplaced() && !destroy(robot, pos)) {
                entry.refused++;
                continue;
            }
            int slot = free ? -1 : slotWith(robot.inventory(), target.asItem());
            if (!free && slot < 0) {
                // In survival the robot gathers its own blocks: say why it stopped, and (issue #438) answer with an error the
                // program can catch, so it can fetch more and go on. The blocks set so far stay.
                // Issue #447: in the player's language, over the robot and in the editor.
                String key = entry.step == 0 ? "robot.halisoncode.no_items" : entry.step == 1 ? "robot.halisoncode.no_items.after_one" : "robot.halisoncode.no_items.after";
                Object[] args = entry.step <= 1 ? new Object[]{target} : new Object[]{target, entry.step};
                robot.say(Messages.robotSaid(key, args));
                buildEnd(robot, queue, entry, "the robot has no " + name(target) + " in its inventory (the owner does not get items free)", Result.error(Result.Code.NO_ITEMS, Messages.robot(key, args)));
                return;
            }
            ItemStack stack = free ? new ItemStack(target.asItem()) : robot.inventory().getItem(slot);
            if (place(robot, stack, pos, Direction.UP, rest -> {
                if (slot >= 0) robot.inventory().setItem(slot, rest);
            })) entry.step++;
            else entry.refused++;
        }
        if (!entry.scan.hasNext()) buildEnd(robot, queue, entry, null, Result.ok(entry.step));
    }

    private static void buildEnd(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry, String why, Result result) {
        if (HalisonCode.LOG.detailed()) HalisonCode.LOG.detail("build: {} finished {}: changed {} of {} blocks looked at.{}{}", label(robot), entry.command.op().name().toLowerCase(java.util.Locale.ROOT),
                entry.step, entry.looked, why == null ? "" : " It stopped early: " + why + ".",
                entry.refused == 0 ? "" : " " + entry.refused + " blocks were refused (a claim or protection, or nothing to place against).");
        queue.finish(result);
    }

    private static int slotWith(SimpleContainer inv, Item item) {
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) return i;
        return -1;
    }

    /**
     * agent.moveTo: plans the path first (over several ticks, at most {@link Limits#pathNodesPerTick} nodes per tick for
     * all robots together), then walks it with {@link #moveOne}, one normal move per step, opening closed wooden doors
     * and gates on the way through the FakePlayer. Answers false when there is no path, or when the world changed so
     * the robot is not where the path says. agent.goHome walks to the home the same way; /robot come teleports instead
     * of answering false.
     */
    private static void navigate(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry, BlockPos target) {
        RobotCommand command = entry.command;
        Level level = robot.level();
        if (entry.path == null) {
            entry.path = new RobotPath(grid(level), robot.blockPosition(), target, robot.gravityMode(), command.op() == RobotCommand.Op.GO_TO_ROBOT);
            pathStart(robot, entry, " to " + at(target));
        }
        if (!entry.path.finished()) {
            pathLeft -= entry.path.search(pathLeft);
            if (!entry.path.finished()) return;
        }
        List<BlockPos> path = entry.path.path();
        if (path == null && entry.path.capped() && command.op() == RobotCommand.Op.GO_TO_ROBOT) {
            pathEnd(robot, entry, "the search limit was reached");
            queue.finish(Result.error(Result.Code.OUT_OF_RANGE, "no way to " + robotName(command.text())
                    + " within the path search limit of " + RobotPath.MAX_NODES + " blocks (it is too far, or there is no way)"));
            return;
        }
        if (path == null || !robot.blockPosition().equals(path.get(entry.step))) {
            pathEnd(robot, entry, path == null ? "no route was found" : "the robot is not where the planned route says (the world changed)");
            if (command.op() == RobotCommand.Op.COME) teleport(robot, queue, entry, target);
            else queue.finish(Result.ok(false));
            return;
        }
        BlockPos at = path.get(entry.step);
        if (entry.step == path.size() - 1) {
            if (command.op() == RobotCommand.Op.GO_TO_ROBOT && at.distManhattan(target) != 1) {
                // The other robot moved while this one walked: it got to where the other robot was, not to it.
                pathEnd(robot, entry, "the other robot moved away meanwhile");
                queue.finish(Result.ok(false, robotName(command.text()) + " moved away while this robot walked to it"));
                return;
            }
            // Next to a solid target (or the other robot): face it, so destroy forward or interact forward reaches it.
            Direction toTarget = RobotPath.direction(at, target);
            if (!at.equals(target) && toTarget.getAxis().isHorizontal()) robot.setYRot(toTarget.toYRot());
            pathEnd(robot, entry, "reached the target");
            queue.finish(Result.ok(command.op() == RobotCommand.Op.COME ? "walked" : true));
            return;
        }
        walk(robot, queue, entry, path, Result.ok(false));
    }

    /**
     * agent.goToRobot (issue #243): walks like moveTo to a free block next to another of the owner's robots, turns to
     * face it when it ends beside it (above or below it, the robot keeps its facing), and answers true. The other robot
     * is looked up by this robot's owner and the name only, so another player's robot is never the target, and again
     * every tick, so it must stay loaded and in this dimension; if it moves meanwhile, the answer is false. Going to
     * itself answers true at once. No free block next to it answers false at once; the search limit answers OUT_OF_RANGE.
     */
    private static void goToRobot(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry) {
        RobotLinks.Key key = new RobotLinks.Key(robot.key().owner(), entry.command.text());
        String name = robotName(key.name());
        if (key.equals(robot.key())) {
            queue.finish(Result.ok(true, name + " is this robot itself, so it is already there"));
            return;
        }
        RobotEntity other = LOADED.get(key);
        Level level = robot.level();
        if (other == null) {
            RobotLinks links = RobotLinks.get(level.getServer());
            if (links.robot(key) != null) {
                queue.finish(Result.error(Result.Code.UNLOADED, name + " is not loaded (see /robot list)"));
                return;
            }
            // Only the owner's own robot names, never another player's.
            List<String> mine = new ArrayList<>(links.names(key.owner()));
            if (links.robot(key.owner()) != null) mine.add(0, "agent (the default robot)");
            queue.finish(Result.error(Result.Code.BAD_ARGS, (key.name().isEmpty() ? "you have no default robot" : "you have no robot called " + key.name())
                    + "; your robots are " + String.join(", ", mine)));
            return;
        }
        if (other.level() != level) {
            queue.finish(Result.error(Result.Code.OUT_OF_RANGE, name + " is in another dimension"));
            return;
        }
        BlockPos target = other.blockPosition();
        if (entry.path == null && Arrays.stream(Direction.values()).map(target::relative)
                .noneMatch(p -> level.isLoaded(p) && (!blocked(level, p) || opens(level, p)))) {
            HalisonCode.LOG.detail("path: {} cannot go to {}: no free block next to it at {}.", label(robot), label(other), at(target));
            queue.finish(Result.ok(false, Messages.robot("robot.halisoncode.no_free_block", name)));
            return;
        }
        navigate(robot, queue, entry, target);
    }

    // "robot Miner", or "agent" for the default robot, as programs call them.
    private static String robotName(String name) {
        return name.isEmpty() ? "agent" : "robot " + name;
    }

    // One move along a planned path, from the block at entry.step to the next; the robot turns to each sideways step.
    private static void walk(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry, List<BlockPos> path, Result blocked) {
        BlockPos at = path.get(entry.step);
        Direction dir = RobotPath.direction(at, path.get(entry.step + 1));
        if (dir.getAxis().isHorizontal()) robot.setYRot(dir.toYRot());
        BlockPos next = at.relative(dir);
        // Issue #109: a closed door on the path; a claim that refuses the click leaves it shut and the move blocked.
        if (opens(robot.level(), next)) interact(robot, next, dir.getOpposite());
        moveOne(robot, queue, entry, dir, false, blocked);
    }

    /**
     * agent.followPath (issue #179): plans the route with {@link RobotTrail} (from the shared scan budget, over ticks
     * when it runs out), then walks it like moveTo and answers the PathStop. The path block is the given block, or the
     * block under the robot; air is never a path. With a side (#180) it keeps to that side like a wall follower. A robot that is not where the route says (the world changed) or a
     * blocked move answers BLOCKED, where it stands. A robot not on the given block steps onto it when it is right next
     * to it (as the route starts any way the path goes); with none next to it, it answers END with the message "I am
     * not on the path" and says it.
     */
    private static void follow(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry) {
        Level level = robot.level();
        if (entry.trail == null) {
            BlockPos at = robot.blockPosition();
            int code = entry.command.a();
            Block block = code == 0 ? level.getBlockState(at.below()).getBlock()
                    : McCompat.block(Identifier.parse(BlockIds.toModern(code)));
            Block under = level.getBlockState(at.below()).getBlock();
            if (code != 0 && !under.equals(block)) entry.note = "the robot stands on " + name(under) + ", not " + name(block);
            pathStart(robot, entry, "");
            entry.trail = new RobotTrail(grid(level), pos -> {
                BlockState state = level.getBlockState(pos);
                return !state.isAir() && state.is(block);
            }, at, robot.getDirection(), robot.gravityMode(), entry.command.b());
        }
        if (!entry.trail.finished()) {
            scanLeft -= entry.trail.plan(scanLeft);
            if (!entry.trail.finished()) return;
        }
        List<BlockPos> route = entry.trail.route();
        Result blocked = Result.ok(RobotTrail.BLOCKED);
        if (!robot.blockPosition().equals(route.get(entry.step))) {
            pathEnd(robot, entry, "the robot is not where the route says (the world changed)");
            queue.finish(blocked);
        } else if (entry.step == route.size() - 1) {
            int stop = entry.trail.stop();
            String why = switch (stop) {
                case RobotTrail.JUNCTION -> "it came to a junction";
                case RobotTrail.BLOCKED -> "the way ahead is blocked";
                case RobotTrail.LIMIT -> "it reached the " + RobotTrail.MAX_BLOCKS + " block limit (follow again to go on)";
                default -> "the path ends here";
            };
            pathEnd(robot, entry, entry.step == 0 ? "stopped at once: " + (entry.note != null ? entry.note : why) : why);
            if (entry.step == 0 && entry.note != null) {
                // Not on the path and none next to it: say so instead of a bare End.
                Block block = McCompat.block(Identifier.parse(BlockIds.toModern(entry.command.a())));
                robot.say(Messages.robotSaid("robot.halisoncode.not_on_path", block));
                queue.finish(Result.ok(stop, Messages.robot("robot.halisoncode.not_on_path", block)));
            } else {
                queue.finish(Result.ok(stop));
            }
        }
        else walk(robot, queue, entry, route, blocked);
    }

    /**
     * /robot come without a path (issue #77): the robot appears in the player's block, as /robot spawn puts it. The
     * rules of teleportToPlayer (architecture section 5): same dimension (RobotCommands checks it) and loaded chunks.
     */
    private static void teleport(RobotEntity robot, RobotQueue queue, RobotQueue.Entry entry, BlockPos target) {
        if (!robot.level().isLoaded(target)) {
            queue.finish(Result.error(Result.Code.UNLOADED, "your block is in an unloaded chunk"));
            return;
        }
        robot.setPos(Vec3.atBottomCenterOf(target));
        Result result = Result.ok("teleported");
        if (robot.gravityMode() && canFall(robot)) {
            entry.falling = true;
            entry.landed = result;
        } else {
            queue.finish(result);
        }
    }

    private static RobotPath.Grid grid(Level level) {
        return new RobotPath.Grid() {
            public boolean loaded(BlockPos pos) {
                return level.isLoaded(pos);
            }

            public boolean blocked(BlockPos pos) {
                return RobotManager.blocked(level, pos);
            }

            public boolean opens(BlockPos pos) {
                return RobotManager.opens(level, pos);
            }
        };
    }

    // A closed wooden door or fence gate (iron doors need redstone; trapdoors would let the robot fall).
    private static boolean opens(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return (DoorBlock.isWoodenDoor(state) || state.getBlock() instanceof FenceGateBlock) && !state.getValue(BlockStateProperties.OPEN);
    }

    /**
     * Outside the build height, or a block with a collision shape (water, lava and grass tufts let the robot through).
     * An open door or trapdoor lets the robot through too, as it lets a player through; an open gate has no shape.
     */
    /**
     * Issue #437: the block a robot spawned at the player's feet goes in. A player on a chest, slab, path or other
     * block lower than a full block has that block at the feet, so the robot goes in the free block above it instead.
     */
    public static BlockPos spawnBlock(Level level, BlockPos feet) {
        return blocked(level, feet) ? feet.above() : feet;
    }

    private static boolean blocked(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return true;
        BlockState state = level.getBlockState(pos);
        if ((state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) && state.getValue(BlockStateProperties.OPEN)) return false;
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean canFall(RobotEntity robot) {
        BlockPos below = robot.blockPosition().below();
        return robot.level().isLoaded(below) && !blocked(robot.level(), below);
    }

    // One block of a fall; false once the robot stands on a block, the world bottom or an unloaded chunk.
    private static boolean fall(RobotEntity robot) {
        if (!canFall(robot)) return false;
        robot.setPos(Vec3.atBottomCenterOf(robot.blockPosition().below()));
        return true;
    }

    private static Result act(RobotEntity robot, RobotCommand command) {
        SimpleContainer inv = robot.inventory();
        Level level = robot.level();
        Direction facing = robot.getDirection();
        switch (command.op()) {
            case TURN -> {
                robot.setYRot((command.direction() == RobotCommand.LEFT_TURN ? facing.getCounterClockWise() : facing.getClockWise()).toYRot());
                return Result.ok(null);
            }
            case SET_SLOT -> {
                robot.setSelectedSlot(command.a());
                return Result.ok(null);
            }
            case GET_ITEM_COUNT -> {
                return Result.ok(inv.getItem(command.a() - 1).getCount());
            }
            case GET_ITEM_SPACE -> {
                ItemStack stack = inv.getItem(command.a() - 1);
                // shortcut: an empty slot reports 64 although some items stack to 16 or 1; fine until getItemDetail exists.
                return Result.ok(stack.isEmpty() ? 64 : stack.getMaxStackSize() - stack.getCount());
            }
            case SET_ITEM -> {
                if (!freeItems(robot)) return Result.error(Result.Code.NO_PERMISSION, "setItem needs creative mode, the gamerule halisoncode:free_items or the permission halisoncode.free_items");
                ItemStack stack = new ItemStack(command.a() == 0 ? Items.AIR : item(command.a()));
                stack.setCount(Math.min(command.b(), stack.getMaxStackSize()));
                inv.setItem(command.c() - 1, stack);
                return Result.ok(null);
            }
            case SET_ASSIST -> {
                robot.setPlaceOnMove(command.b() == 1);
                return Result.ok(null);
            }
            case SET_GRAVITY -> {
                robot.setGravityMode(command.a() == 1);
                return Result.ok(null);
            }
            case COLLECT_ALL -> {
                collect(robot, robot.getBoundingBox().inflate(1), item -> !item.hasPickUpDelay());
                return Result.ok(null);
            }
            case TRANSFER -> {
                return Result.ok(transfer(inv, command.a(), command.b() - 1, command.c() - 1));
            }
            case GET_POSITION -> {
                BlockPos pos = robot.blockPosition();
                return Result.ok(List.of(pos.getX(), pos.getY(), pos.getZ()));
            }
            case GET_ORIENTATION -> {
                return Result.ok(orientation(facing));
            }
            case SAY -> {
                robot.say(command.text());
                return Result.ok(null);
            }
            case CRAFT, STONECUT -> {
                return Result.ok(RobotCrafting.craft(robot, item(command.a()), command.b(), true, command.op() == RobotCommand.Op.STONECUT));
            }
            case CAN_CRAFT -> {
                return Result.ok(RobotCrafting.craft(robot, item(command.a()), command.b(), false, false) >= command.b());
            }
            case SET_HOME -> {
                robot.setHome(GlobalPos.of(level.dimension(), robot.blockPosition()));
                return Result.ok(null);
            }
            case COUNT_NEARBY -> {
                return Result.ok(entities(level, new AABB(robot.blockPosition()).inflate(command.b()), command.a()).size());
            }
            case RADAR -> {
                return Result.ok(radar(robot, command.a(), command.b()));
            }
            case FREE_SLOTS -> {
                return Result.ok(freeSlots(inv));
            }
            case IS_INVENTORY_FULL -> {
                return Result.ok(freeSlots(inv) == 0);
            }
            // Issue #87: the robot's own block only, which is loaded while the robot is. Light as F3 and mob spawning see
            // it: the brighter of block light and sky light, the sky darkened at night and in rain.
            case LIGHT_LEVEL -> {
                return Result.ok(level.getMaxLocalRawBrightness(robot.blockPosition()));
            }
            case BIOME -> {
                String id = level.getBiome(robot.blockPosition()).getRegisteredName();
                return Result.ok(id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id);
            }
            // shortcut: the biome's base temperature (plains 0.8), not lowered with height; use Biome.getTemperature if players need that.
            case TEMPERATURE -> {
                return Result.ok(Math.round(level.getBiome(robot.blockPosition()).value().getBaseTemperature() * 100) / 100.0);
            }
            default -> {
            }
        }
        // The rest act on the neighbouring block in a SixDirection.
        Direction dir = relative(facing, command.direction());
        BlockPos target = robot.blockPosition().relative(dir);
        if (!level.isLoaded(target)) return Result.error(Result.Code.UNLOADED, "block is in an unloaded chunk");
        BlockState state = level.getBlockState(target);
        return switch (command.op()) {
            case PLACE -> {
                boolean placed = place(robot, target, dir.getOpposite());
                if (HalisonCode.LOG.detailed()) HalisonCode.LOG.detail("build: {} {} {} with agent.place{}.", label(robot), placed ? "placed a block at" : "could NOT place a block at", at(target),
                        placed ? "" : !(robot.inventory().getItem(robot.selectedSlot() - 1).getItem() instanceof BlockItem) ? ": the selected slot holds no block"
                                : !state.canBeReplaced() ? ": the target is " + name(state.getBlock()) + ", not empty" : ": a claim or protection refused it");
                yield Result.ok(placed);
            }
            case DESTROY -> Result.ok(destroy(robot, target));
            // Block: true where move would be blocked (non-empty collision shape), so water and grass tufts are false.
            case DETECT -> {
                RobotEffects.send(robot, RobotEffects.INSPECT, target);
                yield Result.ok(command.b() == RobotCommand.KIND_BLOCK
                        ? !state.getCollisionShape(level, target).isEmpty()
                        : level.hasNeighborSignal(target));
            }
            case INSPECT -> {
                RobotEffects.send(robot, RobotEffects.INSPECT, target);
                int code = BlockIds.fromModern(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
                yield Result.ok(command.b() == RobotCommand.KIND_BLOCK ? code : BlockIds.data(code));
            }
            case STORE_ITEMS, TAKE_ITEMS, TAKE_ITEM, CONTAINER_COUNT, CONTAINER_SIZE, SORT_CONTAINER ->
                    container(robot, command, target, dir.getOpposite());
            case INTERACT -> Result.ok(interact(robot, target, dir.getOpposite()));
            case IS_OPEN -> Result.ok(state.getOptionalValue(BlockStateProperties.OPEN).orElse(false));
            case DETECT_ENTITY -> Result.ok(!entities(level, new AABB(target), command.b()).isEmpty());
            case DETECT_LIQUID -> Result.ok(liquid(level, target));
            case IS_SAFE -> Result.ok(safe(level, target));
            case REDSTONE_POWER -> Result.ok(redstonePower(level, target));
            case DROP -> Result.ok(drop(level, target, inv.removeItem(command.b() - 1, command.c())));
            case DROP_ALL -> {
                boolean any = false;
                for (int i = 0; i < inv.getContainerSize(); i++) any |= drop(level, target, inv.removeItemNoUpdate(i));
                yield Result.ok(any);
            }
            default -> throw new IllegalStateException("unhandled op " + command.op());
        };
    }

    /**
     * The robot's FakePlayer: the owner's UUID with its own name, so claim and protection mods treat robot edits as
     * the owner's (Fabric's FakePlayer Javadoc recommends this; Platform.fakePlayer). Fetched per action, never kept: a strong reference
     * would keep the level loaded. No instabuild, so breaking drops items and placing uses up the stack.
     * <p>
     * Null when the owner may not act at pos (issue #53), and every caller then answers its normal refusal. The robot
     * calls the game mode directly, so it does here what vanilla's packet handler does first: {@code Level.mayInteract}
     * (spawn protection, which ops pass by UUID, and the world border). A spectator owner's robot acts on nothing. An
     * adventure owner's FakePlayer gets {@code mayBuild = false}, the ability vanilla's placing checks; breaking checks
     * it in {@link #destroy}. An offline owner counts as survival: programs and /robot commands only reach a robot
     * over its owner's connection, and leaving stops the queue (ServerRelay.playerLeft), so only GameTests act for
     * an offline owner. Spawn protection still applies to them.
     */
    private static ServerPlayer fakePlayer(RobotEntity robot, BlockPos pos) {
        ServerLevel level = (ServerLevel) robot.level();
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(robot.owner());
        GameType mode = owner != null ? owner.gameMode.getGameModeForPlayer() : GameType.DEFAULT_MODE;
        if (mode == GameType.SPECTATOR) return null;
        String name = owner != null ? owner.getGameProfile().name() : robot.owner().toString().substring(0, 8);
        ServerPlayer player = Platform.INSTANCE.fakePlayer(level, new GameProfile(robot.owner(), "HalisonCode[" + name + "]"));
        // The owner's build rule without setGameMode, which would broadcast a tab-list update for a player clients
        // never saw. Never instabuild, also for a creative owner.
        player.getAbilities().instabuild = false;
        player.getAbilities().mayBuild = !mode.isBlockPlacingRestricted();
        player.snapTo(robot.position(), robot.getYRot(), 0);
        return level.mayInteract(player, pos) ? player : null;
    }

    // Places the selected slot's block at pos, as the FakePlayer clicking that (replaceable) position on face.
    private static boolean place(RobotEntity robot, BlockPos pos, Direction face) {
        int slot = robot.selectedSlot() - 1;
        ItemStack stack = robot.inventory().getItem(slot);
        if (!(stack.getItem() instanceof BlockItem) || !robot.level().getBlockState(pos).canBeReplaced()) return false;
        return place(robot, stack, pos, face, rest -> robot.inventory().setItem(slot, rest));
    }

    // The FakePlayer holds stack and clicks pos on face; rest gets what is left in its hand afterwards.
    private static boolean place(RobotEntity robot, ItemStack stack, BlockPos pos, Direction face, Consumer<ItemStack> rest) {
        ServerPlayer player = fakePlayer(robot, pos);
        if (player == null) return false;
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        boolean placed = player.gameMode.useItemOn(player, robot.level(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false)).consumesAction();
        rest.accept(player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return placed;
    }

    // Breaks the block through the normal player path (Fabric break events fire, so protection applies).
    private static boolean destroy(RobotEntity robot, BlockPos pos) {
        Level level = robot.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getDestroySpeed(level, pos) < 0) return false;
        ServerPlayer player = fakePlayer(robot, pos);
        // The FakePlayer's own game mode is survival, so destroyBlock skips vanilla's adventure rule
        // (Player.blockActionRestricted); the pickaxe has no can_break, so that rule refuses every block.
        if (player == null || !player.mayBuild()) return false;
        // shortcut: always a diamond pickaxe so stone and ores drop like a player with the right tool; no silk touch or tool choice.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        AABB space = new AABB(pos).inflate(0.25);
        Set<ItemEntity> before = Set.copyOf(level.getEntitiesOfClass(ItemEntity.class, space));
        boolean broken = player.gameMode.destroyBlock(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // Drops pop out inside the broken block's space and are taken at once; what does not fit stays in the world.
        if (broken) collect(robot, space, item -> !before.contains(item) || !item.hasPickUpDelay());
        return broken;
    }

    /**
     * agent.interact: the FakePlayer uses the block with an empty hand, so claim mods and block events apply. Only
     * doors, trapdoors, fence gates, buttons and levers: an empty-handed click on a bed, a cake or a respawn anchor
     * would make the FakePlayer sleep, eat or blow up. True when the block changed (a pressed button does not).
     */
    static boolean interact(RobotEntity robot, BlockPos pos, Direction face) {
        Level level = robot.level();
        BlockState before = level.getBlockState(pos);
        Block block = before.getBlock();
        if (!(block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock
                || block instanceof ButtonBlock || block instanceof LeverBlock)) return false;
        ServerPlayer player = fakePlayer(robot, pos);
        return player != null && player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false)).consumesAction() && level.getBlockState(pos) != before;
    }

    /**
     * The container ops. A refused or missing container answers false or 0, as an empty hand would find nothing.
     * Items only ever move: what does not fit stays where it was.
     */
    private static Result container(RobotEntity robot, RobotCommand command, BlockPos pos, Direction face) {
        Item item = command.op() == RobotCommand.Op.TAKE_ITEM || command.op() == RobotCommand.Op.CONTAINER_COUNT ? item(command.b()) : null;
        Container c = openContainer(robot, pos, face);
        SimpleContainer inv = robot.inventory();
        return switch (command.op()) {
            case CONTAINER_SIZE -> Result.ok(c == null ? 0 : c.getContainerSize());
            case CONTAINER_COUNT -> Result.ok(c == null ? 0 : c.countItem(item));
            case STORE_ITEMS -> Result.ok(c != null && store(c, inv, face, command.b()));
            case TAKE_ITEMS -> Result.ok(c != null && take(c, inv, face, command.b(), null, command.c()) > 0);
            case TAKE_ITEM -> Result.ok(c == null ? 0 : take(c, inv, face, 0, item, command.c()));
            case SORT_CONTAINER -> Result.ok(c != null && sortable(robot.level().getBlockEntity(pos)) && sort(c));
            default -> throw new IllegalStateException("unhandled op " + command.op());
        };
    }

    /**
     * The container at pos, opened as the owner would open it with an empty hand, or null. The click goes through the
     * FakePlayer's normal player path, so claim and protection mods can refuse it (a FakePlayer opens no screen); a
     * locked container needs its key and a chest under a solid block stays shut, as for a player. Only containers a
     * player opens as a screen (chests, barrels, hoppers, shulker boxes, furnaces, ...): never jukeboxes or shelves,
     * where an empty-handed click would take something out.
     */
    private static Container openContainer(RobotEntity robot, BlockPos pos, Direction face) {
        Level level = robot.level();
        if (!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity entity)) return null;
        ServerPlayer player = fakePlayer(robot, pos);
        if (player == null || !entity.canOpen(player)) return null;
        if (!player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false)).consumesAction()) return null;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof ChestBlock chest ? ChestBlock.getContainer(chest, state, level, pos, false) : entity;
    }

    // storeItems: one robot slot or all (0), each stack put in by vanilla hopper rules for that face (a furnace takes
    // fuel from the side and what to smelt from above). True when anything moved.
    private static boolean store(Container c, SimpleContainer inv, Direction face, int slot) {
        boolean moved = false;
        for (int i = slot == 0 ? 0 : slot - 1; i < (slot == 0 ? inv.getContainerSize() : slot); i++) {
            int before = inv.getItem(i).getCount();
            if (before == 0) continue;
            ItemStack rest = HopperBlockEntity.addItem(null, c, inv.removeItemNoUpdate(i), face);
            inv.setItem(i, rest);
            moved |= rest.getCount() < before;
        }
        return moved;
    }

    /**
     * Moves up to count items into the robot from the slots a hopper on that face could take from (a furnace gives
     * its output downwards). With an item: from any slot until count or the robot is full. Without: from one stack,
     * the given 1-based slot or the first one (0). Answers how many moved.
     */
    private static int take(Container c, SimpleContainer inv, Direction face, int slot, Item item, int count) {
        int[] slots = c instanceof WorldlyContainer w ? w.getSlotsForFace(face) : IntStream.range(0, c.getContainerSize()).toArray();
        int taken = 0;
        for (int i : slots) {
            ItemStack stack = c.getItem(i);
            if (slot != 0 && i != slot - 1 || stack.isEmpty() || item != null && !stack.is(item) || !c.canTakeItem(inv, i, stack)
                    || c instanceof WorldlyContainer w && !w.canTakeItemThroughFace(i, stack, face)) continue;
            int n = Math.min(count - taken, stack.getCount());
            int moved = n - inv.addItem(stack.copyWithCount(n)).getCount();
            if (moved > 0) c.removeItem(i, moved);
            taken += moved;
            if (item == null || moved < n || taken == count) break;
        }
        if (taken > 0) c.setChanged();
        return taken;
    }

    // Plain storage only: sorting a furnace, brewing stand or crafter would mix up slots that mean something.
    private static boolean sortable(BlockEntity entity) {
        return entity instanceof RandomizableContainerBlockEntity && !(entity instanceof CrafterBlockEntity);
    }

    // Merges equal stacks and orders them by item id (minecraft:cobblestone before minecraft:dirt); empty slots go last.
    private static boolean sort(Container c) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack stack = c.removeItemNoUpdate(i);
            for (ItemStack into : stacks) {
                if (stack.isEmpty()) break;
                if (!ItemStack.isSameItemSameComponents(into, stack)) continue;
                int n = Math.min(stack.getCount(), c.getMaxStackSize(into) - into.getCount());
                into.grow(n);
                stack.shrink(n);
            }
            if (!stack.isEmpty()) stacks.add(stack);
        }
        stacks.sort(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        for (int i = 0; i < c.getContainerSize(); i++) c.setItem(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        c.setChanged();
        return true;
    }

    /**
     * Issue #86: living entities of an EntityKind in the box. Mob: the hostile ones (mob category monster). Animal: the
     * other natural mobs (animals, fish, bats, axolotls); villagers and golems are neither. Player: not in spectator
     * mode and not invisible (issue #158), so no program can find a hiding player. Item: dropped items. The robot
     * itself is none of these.
     */
    private static List<Entity> entities(Level level, AABB box, int kind) {
        return level.getEntities((Entity) null, box, e -> e.isAlive() && !e.isSpectator() && switch (kind) {
            case MOB -> e.getType().getCategory() == MobCategory.MONSTER;
            case 1 -> e.getType().getCategory() != MobCategory.MONSTER && e.getType().getCategory() != MobCategory.MISC;
            case 2 -> e instanceof Player && !e.isInvisible();
            default -> e instanceof ItemEntity;
        });
    }

    /**
     * agent.radar (issue #159): the entities of a kind in the cube of that radius around the robot, as countNearby
     * finds them, nearest first (straight-line distance from the robot), at most {@link RobotCommand#MAX_RADAR}. Answers
     * their block positions in one flat list, [x1, y1, z1, x2, y2, z2, ...], empty when there is none.
     */
    private static List<Integer> radar(RobotEntity robot, int kind, int radius) {
        List<Integer> found = new ArrayList<>();
        entities(robot.level(), new AABB(robot.blockPosition()).inflate(radius), kind).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(robot)))
                .limit(RobotCommand.MAX_RADAR)
                .forEach(e -> found.addAll(List.of(e.getBlockX(), e.getBlockY(), e.getBlockZ())));
        return found;
    }

    // agent.detectLiquid (issue #161): the liquid in the block, waterlogged blocks count as water.
    private static int liquid(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.LAVA) ? LAVA : fluid.is(FluidTags.WATER) ? WATER : 0;
    }

    /**
     * agent.isSafe (issue #161): false when the block holds lava; when the block is free and lava or more than
     * {@link #SAFE_DROP} free blocks lie under it before the ground (a fall of 4 or more); or when a hostile mob is in
     * the block or touches one of the 26 blocks around it. A block that would stop a move is checked for lava and mobs only.
     */
    // shortcut: water at the bottom of a deep drop still counts as a fall; add it when players ask for diving.
    private static boolean safe(Level level, BlockPos pos) {
        if (liquid(level, pos) == LAVA) return false;
        if (!blocked(level, pos)) {
            for (int i = 1; ; i++) {
                BlockPos below = pos.below(i);
                if (liquid(level, below) == LAVA) return false;
                if (blocked(level, below)) break;
                if (i > SAFE_DROP) return false;
            }
        }
        return entities(level, new AABB(pos).inflate(1), MOB).isEmpty();
    }

    /**
     * F3's value at pos: what a source gives out (redstone dust its power, a lever 15), otherwise what the block
     * receives. agent.redstonePower and the onRedstonePowered event (issue #89) both read it. The chunk must be loaded.
     */
    public static int redstonePower(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        //? if >=26.2 {
        return state.isSignalSource() ? state.getOwnSignal(level, pos) : level.getBestNeighborSignal(pos);
        //?} else {
        /*// shortcut: before 26.2 there is no getOwnSignal, so a source's power is its strongest side; matches F3 for
        // dust, levers, buttons and torches, may differ for a block that powers one side only.
        return state.isSignalSource() ? ownSignal(level, pos, state) : level.getBestNeighborSignal(pos);
        *///?}
    }

    //? if <26.2 {
    /*private static int ownSignal(Level level, BlockPos pos, BlockState state) {
        int power = 0;
        for (Direction d : Direction.values()) power = Math.max(power, state.getSignal(level, pos, d));
        return power;
    }

    *///?}
    private static int freeSlots(SimpleContainer inv) {
        int free = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).isEmpty()) free++;
        return free;
    }

    // An item code the parser has validated (ItemIds), so the registry lookup always finds it.
    private static Item item(int code) {
        return McCompat.item(McCompat.itemId(ItemIds.toModern(code)));
    }

    // Picks up as the owner would (issue #53): never an item that is kept for somebody else (ItemEntity.playerTouch).
    private static void collect(RobotEntity robot, AABB box, Predicate<ItemEntity> ready) {
        for (ItemEntity item : robot.level().getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive)) {
            UUID keptFor = ((ItemEntityAccessor) item).halisoncode$target();
            if (!ready.test(item) || keptFor != null && !keptFor.equals(robot.owner())) continue;
            ItemStack rest = robot.inventory().addItem(item.getItem());
            if (rest.isEmpty()) item.discard();
            else item.setItem(rest);
        }
    }

    private static boolean drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Vec3 at = Vec3.atCenterOf(pos);
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        return true;
    }

    private static boolean transfer(SimpleContainer inv, int quantity, int from, int to) {
        ItemStack source = inv.getItem(from);
        ItemStack dest = inv.getItem(to);
        if (from == to || source.isEmpty() || !dest.isEmpty() && !ItemStack.isSameItemSameComponents(source, dest)) return false;
        int room = dest.isEmpty() ? source.getMaxStackSize() : dest.getMaxStackSize() - dest.getCount();
        int n = Math.min(quantity, Math.min(source.getCount(), room));
        if (n <= 0) return false;
        if (dest.isEmpty()) {
            inv.setItem(to, source.split(n));
        } else {
            dest.grow(n);
            source.shrink(n);
        }
        inv.setChanged();
        return true;
    }

    /**
     * In survival the robot gathers its own resources. setItem and building without items are allowed only when the
     * owner is online and in creative, or when the world's gamerule halisoncode:free_items is true, unless the server's
     * permission mod says otherwise ({@link Permission#FREE_ITEMS}).
     */
    private static boolean freeItems(RobotEntity robot) {
        MinecraftServer server = robot.level().getServer();
        ServerPlayer owner = server.getPlayerList().getPlayer(robot.owner());
        return owner == null ? HalisonCode.freeItems(server) : Permission.FREE_ITEMS.allows(owner);
    }

    /**
     * MakeCode's yaw convention (its agent.getOrientation page): south 0, west 90, north -180, east -90, which is Java's
     * yaw folded to -180 to 180. shortcut: not yet confirmed in Minecraft Education (issue #22).
     */
    public static int orientation(Direction facing) {
        return Mth.wrapDegrees((int) facing.toYRot());
    }

    static Direction relative(Direction facing, int sixDirection) {
        return switch (sixDirection) {
            case RobotCommand.FORWARD -> facing;
            case RobotCommand.BACK -> facing.getOpposite();
            case RobotCommand.LEFT -> facing.getCounterClockWise();
            case RobotCommand.RIGHT -> facing.getClockWise();
            case RobotCommand.UP -> Direction.UP;
            case RobotCommand.DOWN -> Direction.DOWN;
            default -> throw new IllegalArgumentException("SixDirection " + sixDirection);
        };
    }
}
