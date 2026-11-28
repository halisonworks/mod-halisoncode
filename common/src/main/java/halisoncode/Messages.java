package halisoncode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import halisoncode.robot.Result;
import halisoncode.robot.RobotLinks;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

/**
 * Issue #278: everything Halison Code says to a player goes through here, so it looks and reads like the game's own
 * messages (docs/architecture.md section 11 lists the kinds). PlayerMessageTest fails when other code sends one itself.
 */
public final class Messages {
    private static final JsonObject ENGLISH = english();

    private Messages() {
    }

    private static JsonObject english() {
        try (InputStream in = Messages.class.getResourceAsStream("/assets/halisoncode/lang/en_us.json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A message in the reader's language. It carries the English text, for vanilla clients that have no Halison Code keys. */
    public static MutableComponent of(String key, Object... args) {
        return Component.translatableWithFallback(key, ENGLISH.get(key).getAsString(), args);
    }

    // Issue #447: what a robot says in its own words is kept in the robot's synced text as KEY, the key, and each
    // argument after ARG; a block argument is BLOCK and the block's translation key. A program's own text (agent.say)
    // has no control characters (RobotCommand turns them into spaces), so it never starts with KEY.
    private static final String KEY = "", ARG = "", BLOCK = "";

    /**
     * A sentence of the robot's own (key {@code robot.halisoncode.*}), for the editor: the English text and each argument
     * as text, a block by its id with spaces ("oak planks").
     */
    public static Result.Text robot(String key, Object... args) {
        List<String> texts = java.util.Arrays.stream(args).map(a -> a instanceof Block b
                ? BuiltInRegistries.BLOCK.getKey(b).getPath().replace('_', ' ') : String.valueOf(a)).toList();
        return new Result.Text(key, texts, String.format(java.util.Locale.ROOT, ENGLISH.get(key).getAsString(), texts.toArray()));
    }

    /** {@link #robot} as the robot's synced text, with block arguments kept as blocks so the reader sees its own name for them. */
    public static String robotSaid(String key, Object... args) {
        StringBuilder s = new StringBuilder(KEY).append(key);
        for (Object a : args) s.append(ARG).append(a instanceof Block b ? BLOCK + b.getDescriptionId() : String.valueOf(a));
        return s.toString();
    }

    /** The line over a robot: a program's text as it is, or the robot's own sentence in the reader's language. */
    public static Component said(String said) {
        if (!said.startsWith(KEY)) return Component.literal(said);
        String[] parts = said.substring(1).split(ARG, -1);
        Object[] args = java.util.Arrays.stream(parts, 1, parts.length)
                .map(a -> a.startsWith(BLOCK) ? (Object) Component.translatable(a.substring(1)) : a).toArray();
        return of(parts[0], args);
    }

    /** Success: white, and silent when the game rule sendCommandFeedback is off. Only the caller sees it. */
    public static void success(CommandSourceStack source, Component text) {
        source.sendSuccess(() -> text, false);
    }

    /** Success of an operator's command for the whole server: other operators are told too, as vanilla does. */
    public static void operatorSuccess(CommandSourceStack source, Component text) {
        source.sendSuccess(() -> text, true);
    }

    /** Refusal: red. Says what happened and what to do next. Answers 0, a failed command's result. */
    public static int failure(CommandSourceStack source, Component text) {
        source.sendFailure(text);
        return 0;
    }

    /** A wrong argument: red, as the game's own argument errors. */
    public static DynamicCommandExceptionType argumentError(String key) {
        return new DynamicCommandExceptionType(value -> of(key, value));
    }

    /** A refusal of something the player did with the mouse: above the hotbar, as for a locked chest. */
    public static void actionBar(ServerPlayer player, Component text) {
        player.sendSystemMessage(text, true);
    }

    /** One message of several lines. */
    public static Component lines(List<Component> lines) {
        MutableComponent all = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) all.append("\n");
            all.append(lines.get(i));
        }
        return all;
    }

    /** "Kintro" or "Robot Miner". */
    public static Component robot(String name) {
        return name.isEmpty() ? of("entity.halisoncode.kintro") : of("message.halisoncode.robot.named", name);
    }

    /** As {@link #robot(String)}, with the name the owner gave the default robot, if any (issue #293). */
    public static Component robot(MinecraftServer server, UUID owner, String name) {
        return robot(name.isEmpty() ? RobotLinks.get(server).defaultName(owner) : name);
    }

    /** "/robot spawn" or "/robot spawn Miner". */
    public static String spawnCommand(String name) {
        return name.isEmpty() ? "/robot spawn" : "/robot spawn " + name;
    }

    /** Why a robot that is not in the loaded world cannot be used, and what to do; the same words everywhere. */
    public static Component absent(MinecraftServer server, UUID owner, String name) {
        if (RobotLinks.get(server).robot(new RobotLinks.Key(owner, name)) != null) {
            return of("message.halisoncode.robot.unloaded", robot(server, owner, name), spawnCommand(name));
        }
        if (!name.isEmpty()) return of("message.halisoncode.robot.unknown", name);
        return of(RobotLinks.get(server).names(owner).isEmpty() ? "message.halisoncode.robot.none" : "message.halisoncode.robot.no_default", spawnCommand(name));
    }
}
