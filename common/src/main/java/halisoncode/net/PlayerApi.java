package halisoncode.net;

import halisoncode.Messages;
import halisoncode.HalisonCode;
import halisoncode.McCompat;
import halisoncode.Permission;
import halisoncode.robot.Result;
import halisoncode.robot.RobotCommand;
import halisoncode.robot.RobotException;
import halisoncode.robot.RobotManager;
import halisoncode.robot.RobotQueue;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * The player API (issue #26): calls about the program's owner, never about another player, and the time and weather of
 * the owner's dimension (issue #87). They need no robot, so they skip the robot queues, but they get the same pace and
 * cap: one call per owner per tick, at most
 * {@link halisoncode.Limits#queueLength} waiting, else {@code RATE_LIMITED}. Game mode does not matter: the calls only read
 * the owner, tell the owner something, or move the owner as /tp would, so spectator and adventure owners may use them
 * all (teleport still needs the rights below). Server thread only.
 * <p>
 * Issue #249: signals.send goes the same way, to the listeners other mods added through {@link halisoncode.api.Signals},
 * never to a program, and at most once a second for each name and player.
 */
final class PlayerApi {
    private static final Map<UUID, ArrayDeque<Runnable>> WAITING = new HashMap<>();
    // Issue #249: the tick each player last sent each signal name, for one per name per second. Kept across runs, so
    // pressing Run again does not skip the wait.
    private static final Map<UUID, Map<String, Integer>> SENT = new HashMap<>();
    private static final int TICKS_PER_SIGNAL = 20;

    private PlayerApi() {
    }

    /** Queues a player call from {@code player}'s browser; false when {@code cmd} is not one. */
    static boolean submit(ServerPlayer player, Protocol.Cmd cmd, Consumer<String> out) throws RobotException {
        RobotCommand c = cmd.command();
        if (c.op().compareTo(RobotCommand.Op.PLAYER_SAY) < 0 || c.op().compareTo(RobotCommand.Op.SIGNAL_SEND) > 0) return false;
        UUID owner = player.getUUID();
        MinecraftServer server = player.level().getServer();
        if (cmd.run() != RobotManager.queue(owner).run()) throw new RobotException(Result.Code.STOPPED, "run " + cmd.run() + " is no longer active");
        ArrayDeque<Runnable> waiting = WAITING.computeIfAbsent(owner, o -> new ArrayDeque<>());
        int max = HalisonCode.limits().queueLength();
        if (waiting.size() >= max) throw new RobotException(Result.Code.RATE_LIMITED, "too many player calls waiting (" + max + ")");
        waiting.add(() -> {
            Result result;
            ServerPlayer now = server.getPlayerList().getPlayer(owner);
            if (cmd.run() != RobotManager.queue(owner).run()) result = Result.error(Result.Code.STOPPED, "run " + cmd.run() + " is no longer active");
            else if (now == null || now.hasDisconnected()) result = Result.error(Result.Code.UNLOADED, "you are not in the game");
            else result = run(now, c);
            ServerRelay.reply(player.getGameProfile().name(), cmd, result, out);
        });
        return true;
    }

    static void tick() {
        WAITING.values().removeIf(waiting -> {
            waiting.poll().run();
            return waiting.isEmpty();
        });
    }

    static void playerLeft(UUID owner) {
        WAITING.remove(owner);
        SENT.remove(owner);
    }

    static void serverStopped() {
        WAITING.clear();
        SENT.clear();
    }

    private static Result run(ServerPlayer player, RobotCommand c) {
        return switch (c.op()) {
            case PLAYER_SAY -> {
                // Literal text to the owner only: no formatting, click or hover events (RobotCommand.sayText strips the rest).
                player.sendSystemMessage(Component.literal(c.text()));
                yield Result.ok(null);
            }
            case PLAYER_POSITION -> {
                BlockPos pos = player.blockPosition();
                yield Result.ok(List.of(pos.getX(), pos.getY(), pos.getZ()));
            }
            // MakeCode's convention, as agent.getOrientation: south 0, west 90, north -180, east -90; one decimal.
            // Rounded first: 179.97 becomes 180, which is north and answers -180.
            case PLAYER_ORIENTATION -> Result.ok(Mth.wrapDegrees(Math.round(Mth.wrapDegrees(player.getYRot()) * 10) / 10.0));
            case PLAYER_NAME -> Result.ok(player.getGameProfile().name());
            case PLAYER_TELEPORT -> teleport(player, new BlockPos(c.a(), c.b(), c.c()));
            // Issue #87: what the owner sees anyway. The time is the Overworld's clock in every dimension (as /time query daytime).
            case TIME_OF_DAY -> Result.ok(Math.floorMod(McCompat.dayTime(player.level()), 24000L));
            // Rain or thunder in the owner's dimension (snow counts), not whether it falls on one block; never in the Nether or the End.
            case IS_RAINING -> Result.ok(player.level().isRaining());
            case SIGNAL_SEND -> signal(player, c.text());
            default -> throw new IllegalStateException("not a player call: " + c.op());
        };
    }

    // Only the name leaves, and only to other mods' listeners. false with a message is an answer, not an error: no mod
    // listens, none takes the name, or the same name went less than a second ago (a signal may switch real equipment).
    private static Result signal(ServerPlayer player, String name) {
        int now = player.level().getServer().getTickCount();
        Map<String, Integer> sent = SENT.computeIfAbsent(player.getUUID(), o -> new HashMap<>());
        sent.values().removeIf(tick -> now - tick >= TICKS_PER_SIGNAL || now < tick);
        if (sent.containsKey(name)) return Result.ok(false, Messages.robot("robot.halisoncode.signal.again", name));
        sent.put(name, now);
        if (!SignalHub.hasListeners()) return Result.ok(false, SignalHub.nobodyListens(name));
        boolean taken = SignalHub.send(player.getUUID(), name);
        HalisonCode.LOG.detail("signals: {}'s program sent a signal, {}.", player.getGameProfile().name(), taken ? "taken" : "not taken by any listener");
        return taken ? Result.ok(true) : Result.ok(false, Messages.robot("robot.halisoncode.signal.not_taken", name));
    }

    // As /tp would allow it: by default the host of a singleplayer world or an op (game master), in the owner's own dimension,
    // inside the world border and the build height. To the middle of the block, keeping the owner's facing.
    private static Result teleport(ServerPlayer player, BlockPos pos) {
        String name = player.getGameProfile().name();
        if (!Permission.TELEPORT.allows(player)) {
            return Result.error(Result.Code.NO_PERMISSION, "player.teleport needs singleplayer, operator rights or the permission " + Permission.TELEPORT.node());
        }
        ServerLevel level = McCompat.level(player);
        if (!Level.isInSpawnableBounds(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return Result.error(Result.Code.OUT_OF_RANGE, "x=" + pos.getX() + " y=" + pos.getY() + " z=" + pos.getZ() + " is outside the world border or the build height");
        }
        player.stopRiding();
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        HalisonCode.LOG.detail("player: {}'s program teleported them to x={} y={} z={} in {}.", name, pos.getX(), pos.getY(), pos.getZ(), McCompat.dimension(level).getPath());
        return Result.ok(null);
    }
}
