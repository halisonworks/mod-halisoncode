package halisoncode.fabric.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
//?} else
/*import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;*/
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Issue #456: the chat screen checks what the player types against the server's command tree, and Fabric copies the
 * client commands into it. When the server has a /halisoncode too (chat, signal), Brigadier merges Fabric's copy of
 * our node into the server's node before Fabric has given the copy its children, so log, report and effects never
 * get there and the chat marks them red, although they run. This puts them in, once per command tree.
 */
final class ClientCommandTree {
    private static final String ROOT = "halisoncode";

    private ClientCommandTree() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static void repair(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        //? if >=26.1 {
        CommandDispatcher<?> active = ClientCommands.getActiveDispatcher();
        //?} else
        /*CommandDispatcher<?> active = ClientCommandManager.getActiveDispatcher();*/
        if (connection == null || active == null) return;
        CommandNode<?> mine = active.getRoot().getChild(ROOT);
        CommandNode served = connection.getCommands().getRoot().getChild(ROOT);
        if (mine == null || served == null) return;
        for (CommandNode<?> child : mine.getChildren()) {
            if (served.getChild(child.getName()) == null) served.addChild(copy(child));
        }
    }

    // As Fabric's copy (it only completes, the client dispatcher runs it), but children first, so nothing is lost.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CommandNode copy(CommandNode<?> node) {
        ArgumentBuilder builder = node.createBuilder();
        builder.requires(s -> true);
        if (builder.getCommand() != null) builder.executes(c -> 0);
        CommandNode result = builder.build();
        for (CommandNode<?> child : node.getChildren()) result.addChild(copy(child));
        return result;
    }
}
