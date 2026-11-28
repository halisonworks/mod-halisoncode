package halisoncode.robot;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;

/**
 * FIFO of commands for one robot plus its run id (architecture section 3, tick model). The run id is shared by the
 * queues of one owner's robots (issue #141): one editor run drives them all, each robot works through its own queue.
 * Not thread-safe: only touched on the server thread.
 */
public final class RobotQueue {
    /** Events that may wait for a program, and the default of {@link halisoncode.Limits#queueLength}: the editor sends one command at a time for each robot, so this mainly limits /robot. */
    public static final int MAX_LENGTH = 16;

    private static long lastRun;

    /** A queued command with the executor's progress on it. */
    public static final class Entry {
        public final RobotCommand command;
        /** Log only: the command's id in its run, or -1 for a /robot command; and that it has started. */
        public final long id;
        public boolean started;
        private final Consumer<Result> done;
        public int step;
        public int wait;
        /** Gravity mode: the robot falls one block per tick before the command goes on. */
        public boolean falling;
        /** The result to answer once the robot has landed, or null when the command goes on after the fall. */
        public Result landed;
        /** findNearest: the blocks still to check, nearest first; the scan goes on over later ticks. */
        public Iterator<BlockPos> scan;
        /** moveTo: the path search, then the path being walked ({@code step} is the index of the block the robot is on). */
        public RobotPath path;
        /** followPath: the route planned along the path, walked like moveTo's path. */
        public RobotTrail trail;
        /** findBounds: the blocks still to look around, the blocks already seen, and the box so far (min x, y, z, max x, y, z). */
        public ArrayDeque<BlockPos> fill;
        public Set<BlockPos> seen;
        public int[] bounds;
        /** shapes.maze: the layout, made once when the build starts ({@link RobotShapes#maze}). */
        public boolean[][] maze;
        /** Log only: blocks looked at and refused by a build, where a walk started, and why followPath cannot follow. */
        public int looked, refused;
        public BlockPos from;
        public String note;

        Entry(long id, RobotCommand command, Consumer<Result> done) {
            this.id = id;
            this.command = command;
            this.done = done;
        }
    }

    /** The current run id of one owner's robots. */
    static final class Run {
        private long id = nextRun();
    }

    private final ArrayDeque<Entry> entries = new ArrayDeque<>();
    private final Run run;
    private boolean paused;

    public RobotQueue() {
        this(new Run());
    }

    RobotQueue(Run run) {
        this.run = run;
    }

    private static synchronized long nextRun() {
        return ++lastRun;
    }

    public long run() {
        return run.id;
    }

    /** Paused: the executor skips this queue; nothing is dropped and the sim stays blocked on its pending command. */
    public boolean paused() {
        return paused;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public int size() {
        return entries.size();
    }

    /**
     * New run: drops the queue (answering {@code STOPPED}) and returns the fresh run id. The owner's other robots
     * share the run, so {@link RobotManager#start} drops their queues too.
     */
    public long start() {
        clear(Result.Code.STOPPED, "a new run started");
        paused = false;
        return run.id = nextRun();
    }

    /**
     * Drops the queue and invalidates the current run, so its later commands get {@code STOPPED}. The run is shared
     * by the owner's robots: {@link RobotManager#stop} stops them all.
     */
    public void stop() {
        clear(Result.Code.STOPPED, "stopped");
        paused = false;
        run.id = nextRun();
    }

    public void submit(long run, RobotCommand command, Consumer<Result> done) throws RobotException {
        submit(run, -1, command, MAX_LENGTH, done);
    }

    /** At most {@code max} commands wait (issue #52). */
    void submit(long run, long id, RobotCommand command, int max, Consumer<Result> done) throws RobotException {
        if (run != this.run.id) throw new RobotException(Result.Code.STOPPED, "run " + run + " is no longer active");
        if (entries.size() >= max) throw new RobotException(Result.Code.RATE_LIMITED, "queue is full (" + max + ")");
        entries.add(new Entry(id, command, done));
    }

    public Entry head() {
        return entries.peek();
    }

    /** Issue #81: told about each refused command this queue finishes, not about the ones a stop clears. */
    Runnable refused = () -> { };

    /** Removes the head and reports its result. */
    public void finish(Result result) {
        Entry e = entries.poll();
        if (e == null) return;
        if (!result.ok()) refused.run();
        e.done.accept(result);
    }

    /** Answers every queued command with the given error and empties the queue. */
    public void clear(Result.Code code, String message) {
        Entry e;
        while ((e = entries.poll()) != null) e.done.accept(Result.error(code, message));
    }
}
