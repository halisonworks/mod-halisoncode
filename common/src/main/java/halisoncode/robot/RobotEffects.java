package halisoncode.robot;

import halisoncode.net.RelayPayloads;
import halisoncode.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Issues #81 and #82: tells the owner's client what the robot just did, so it can show particles and play a sound
 * (or not: {@code /halisoncode effects off}). Only the owner, only near the robot: a classroom of robots stays quiet.
 * Placing and breaking need nothing here, the game's own sounds and particles already play for them.
 */
public final class RobotEffects {
    /** The robot stepped to this block. */
    public static final int MOVE = 0;
    /** A command was refused; the block is where the robot stands. */
    public static final int ERROR = 1;
    /** detect or inspect looked at this block. */
    public static final int INSPECT = 2;
    /** Farther away the owner would not see the particles anyway. */
    static final double RANGE = 48;

    private RobotEffects() {
    }

    static void send(RobotEntity robot, int kind, BlockPos pos) {
        if (robot == null || !(robot.level() instanceof ServerLevel level)) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(robot.owner());
        // GameTest mock players have no Halison Code client, so nothing is sent to them.
        if (owner == null || owner.level() != level || owner.distanceToSqr(robot) > RANGE * RANGE) return;
        if (Platform.INSTANCE.canSend(owner, RelayPayloads.Effect.TYPE)) Platform.INSTANCE.send(owner, new RelayPayloads.Effect(kind, pos));
    }
}
