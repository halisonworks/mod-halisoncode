package halisoncode.client;

import halisoncode.HalisonCode;
import halisoncode.Messages;
import halisoncode.Report;
import halisoncode.log.Log;
import halisoncode.net.Protocol;
import halisoncode.net.RelayPayloads;
import halisoncode.relay.EditorLauncher;
import halisoncode.relay.RelayServer;
import halisoncode.platform.ClientPlatform;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
//? if >=26.3 {
import com.mojang.blaze3d.Blaze3D;
//?} else if >=1.21.11 {
/*import net.minecraft.util.Util;
*///?} else
/*import net.minecraft.Util;*/
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;

/**
 * The client half of the mod: the editor and relay server. Each loader's client entrypoint registers the renderer,
 * screen, keys and HUD and calls these hooks from its own events (issue #186).
 */
public final class HalisonCodeClient {
    private static RelayServer relay;
    /** Issue #192: the linked editor was told that the game is paused. */
    private static boolean toldPaused;
    /** Issue #7: opens the editor as /halisoncode does. H by default; change it in Controls under Halison Code. */
    public static final KeyMapping EDITOR_KEY = new KeyMapping("key.halisoncode.editor", InputConstants.KEY_H, RobotCameraClient.KEY.getCategory());
    /** Opens the launcher file in the browser. The client GameTest puts its own here, so no browser starts. */
    //? if >=26.3 {
    public static Consumer<Path> browser = Blaze3D::openPath;
    //?} else
    /*public static Consumer<Path> browser = Util.getPlatform()::openPath;*/

    private HalisonCodeClient() {
    }

    /** The running relay, or null. For the end-to-end client GameTest: its editor URL holds the token, never log it. */
    public static RelayServer relay() {
        return relay;
    }

    /** The game has started: start the editor and relay server. */
    public static void started(Minecraft client) {
        EditorLauncher.deleteAll(EditorLauncher.DIR); // left by a crash
        start(client);
    }

    private static int port() {
        return Integer.getInteger("halisoncode.port", RelayServer.DEFAULT_PORT);
    }

    /** Starts the editor and relay server. Never on another port: the browser stores projects per address (issue #8). */
    public static boolean start(Minecraft client) {
        int port = port();
        try {
            relay = new RelayServer(port, new GameLink(client)).start();
            HalisonCode.LOG.info("Halison Code editor on http://127.0.0.1:{}/", port);
            return true;
        } catch (Exception e) {
            relay = null;
            HalisonCode.LOG.error("Halison Code could not start the editor server on port {}", port, e);
            return false;
        }
    }

    public static void stopping() {
        if (relay != null) relay.close();
        relay = null;
        EditorLauncher.deleteAll(EditorLauncher.DIR);
    }

    /** A {@code halisoncode:to_client} payload goes to the browser session. Runs on the render thread; deliver is thread-safe. */
    public static void receive(String json) {
        if (HalisonCode.LOG.detailed()) HalisonCode.LOG.detail("client: from the server: {}", Protocol.describe(json));
        if (relay != null) relay.deliver(json);
    }

    /** True while a program runs from the editor: losing focus to the browser must not pause the game (issue #192). */
    public static boolean keepRunning() {
        return relay != null && relay.linked();
    }

    /**
     * Whether the "pause on lost focus" option counts: not while a program runs (issue #192), and not in a world whose
     * game rule halisoncode:pause_on_lost_focus is false (issue #297). Only the player who runs the world has the rule here:
     * a server does not send its rules, and it does not stop for this. The option itself is never changed.
     */
    public static boolean mayPauseOnLostFocus() {
        if (keepRunning()) return false;
        Minecraft client = Minecraft.getInstance();
        MinecraftServer server = client.getSingleplayerServer();
        return server == null || client.level == null || HalisonCode.pauseOnLostFocus(server);
    }

    /**
     * Each client tick (issue #192): when singleplayer is paused (the pause menu is open) while a program runs, the
     * integrated server stops and the robot's commands wait. Tell the editor once, and again when the game goes on;
     * the waiting commands then run.
     */
    public static void tick(Minecraft client) {
        Log.tick();
        // In a world, with no screen open: the key does nothing on the title screen or while typing in chat.
        while (EDITOR_KEY.consumeClick()) {
            if (client.player != null) openEditor();
        }
        boolean paused = keepRunning() && client.isPaused();
        if (paused == toldPaused) return;
        toldPaused = paused;
        if (relay != null) relay.deliver(Protocol.control("notice", -1, paused ? "Minecraft is paused. Go back to the game and close the menu."
                : "Minecraft is running again."));
    }

    /** Leaving the world ends the run on the server; tell the browser so the sim does not wait forever. */
    public static void disconnected() {
        if (relay != null) relay.deliver(Protocol.control("stop", -1, "You left the world, so the program stopped."));
    }

    /** The {@code /halisoncode} client commands, for either loader's own command source. */
    public static <S> LiteralArgumentBuilder<S> command() {
        return LiteralArgumentBuilder.<S>literal("halisoncode").executes(c -> openEditor())
                .then(LiteralArgumentBuilder.<S>literal("log")
                        .then(LiteralArgumentBuilder.<S>literal("on").executes(c -> extendedLog(Log.DEFAULT_MINUTES))
                                .then(RequiredArgumentBuilder.<S, Integer>argument("minutes", IntegerArgumentType.integer(1, Log.MAX_MINUTES))
                                        .executes(c -> extendedLog(IntegerArgumentType.getInteger(c, "minutes")))))
                        .then(LiteralArgumentBuilder.<S>literal("off").executes(c -> extendedLog(0))))
                .then(LiteralArgumentBuilder.<S>literal("report").executes(c -> report()))
                .then(LiteralArgumentBuilder.<S>literal("effects")
                        .then(LiteralArgumentBuilder.<S>literal("on").executes(c -> effects(true)))
                        .then(LiteralArgumentBuilder.<S>literal("off").executes(c -> effects(false))));
    }

    /** Issue #81: the robot's particles and sounds on this computer. */
    private static int effects(boolean on) {
        RobotEffectsClient.set(on);
        show(on ? Messages.of("commands.halisoncode.effects.on") : Messages.of("commands.halisoncode.effects.off", "/halisoncode effects on"));
        return 1;
    }

    private static int extendedLog(int minutes) {
        if (minutes == 0) {
            Log.extendedOff("/halisoncode log off");
            show(Messages.of("commands.halisoncode.log.off"));
        } else {
            Log.extendedOn(minutes);
            show(Messages.of("commands.halisoncode.log.on", minutes, "/halisoncode report"));
        }
        return 1;
    }

    /** Writes the report (issue #279); the chat line copies its text for a bug report. */
    private static int report() {
        Minecraft client = Minecraft.getInstance();
        Log.person(client.getUser().getName());
        try {
            String text = Report.write(client.gameDirectory.toPath(), client.getSingleplayerServer(),
                    client.getConnection() == null ? "the game, in no world" : client.hasSingleplayerServer() ? "the game, in its own world" : "the game, on a server",
                    relay == null ? "Editor server: not running" : "Editor server: running on port " + relay.port() + (relay.linked() ? ", a program is running" : ""));
            show(Messages.of("message.halisoncode.report", Report.FILE, copy("message.halisoncode.report.copy", text)));
            return 1;
        } catch (IOException e) {
            HalisonCode.LOG.error("Could not write the report", e);
            show(Messages.of("commands.halisoncode.report.failed", Report.FILE));
            return 0;
        }
    }

    /**
     * The {@code /halisoncode} client command: opens the editor in the browser through a one-time launcher file, so the
     * token is never in a command line (issue #191). The chat line copies the link for a browser that cannot read the
     * file, such as a snap or Flatpak browser on Linux (no access to /tmp).
     */
    public static int openEditor() {
        if (relay == null && !start(Minecraft.getInstance())) { // the port may be free now
            show(Messages.of("message.halisoncode.editor.port", port(), "/halisoncode", "-Dhalisoncode.port=" + (port() + 1)).withStyle(ChatFormatting.RED));
            return 0;
        }
        try {
            EditorLauncher.launch(EditorLauncher.DIR, relay.editorUrl(), browser);
        } catch (IOException e) {
            HalisonCode.LOG.error("Halison Code could not write the editor launcher", e); // a path, never the token
        }
        show(Messages.of("message.halisoncode.editor.opening", copy("message.halisoncode.editor.copy", relay.editorUrl())));
        return 1;
    }

    /** A text whose click copies {@code copied}; the chat log gets only the visible text. */
    private static Component copy(String key, String copied) {
        //? if >=1.21.5 {
        return Messages.of(key).withStyle(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(copied)).withUnderlined(true));
        //?} else
        /*return Messages.of(key).withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copied)).withUnderlined(true));*/
    }

    /** A line in this player's chat. */
    private static void show(Component message) {
        Minecraft client = Minecraft.getInstance();
        //? if >=26.2 {
        client.gui.hud.getChat().addClientSystemMessage(message);
        //?} else if >=26.1 {
        /*client.gui.getChat().addClientSystemMessage(message);
        *///?} else {
        /*client.gui.getChat().addMessage(message);
        *///?}
    }

    /** Sends browser messages to the logical server as payloads (singleplayer or a server with Halison Code). */
    private record GameLink(Minecraft client) implements RelayServer.Link {
        @Override
        public String playerName() {
            return client.getUser().getName();
        }

        // Called on the relay's Netty thread: checks here are best effort, the send itself runs on the render thread.
        @Override
        public String send(String json) {
            if (client.getConnection() == null) return "Not in a world. Open a world in Minecraft, then run the program again.";
            try {
                if (!ClientPlatform.INSTANCE.canSend(RelayPayloads.ToServer.TYPE)) return "This server does not have Halison Code installed.";
            } catch (IllegalStateException e) {
                return "Not in a world. Open a world in Minecraft, then run the program again.";
            }
            client.execute(() -> {
                if (client.getConnection() == null) return;
                if (HalisonCode.LOG.detailed() && !json.contains("\"heartbeat\"")) HalisonCode.LOG.detail("client: to the server: {}", Protocol.describe(json));
                ClientPlatform.INSTANCE.send(new RelayPayloads.ToServer(json));
            });
            return null;
        }
    }
}
