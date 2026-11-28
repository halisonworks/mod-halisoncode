package halisoncode.fabric.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import halisoncode.HalisonCode;
import halisoncode.client.HalisonCodeClient;
import halisoncode.client.RobotCameraClient;
import halisoncode.client.RobotEffectsClient;
import halisoncode.client.RobotRenderer;
import halisoncode.client.RobotScreen;
import halisoncode.net.RelayPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;*/
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;*/
//? if >=26.3
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.gui.screens.MenuScreens;
//? if >=26.1 {
import net.minecraft.client.renderer.entity.EntityRenderers;
//?} else
/*import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;*/

/** Fabric client entrypoint: registers the renderer, screen, keys and HUD and wires Fabric's client events. */
public final class HalisonCodeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //? if >=26.1 {
        EntityRenderers.register(HalisonCode.ROBOT.get(), RobotRenderer::new);
        //?} else
        /*EntityRendererRegistry.register(HalisonCode.ROBOT.get(), RobotRenderer::new);*/
        MenuScreens.register(HalisonCode.ROBOT_MENU.get(), RobotScreen::new);
        //? if >=26.1 {
        KeyMappingHelper.registerKeyMapping(RobotCameraClient.KEY);
        KeyMappingHelper.registerKeyMapping(RobotCameraClient.FOLLOW_KEY);
        KeyMappingHelper.registerKeyMapping(HalisonCodeClient.EDITOR_KEY);
        //?} else {
        /*KeyBindingHelper.registerKeyBinding(RobotCameraClient.KEY);
        KeyBindingHelper.registerKeyBinding(RobotCameraClient.FOLLOW_KEY);
        KeyBindingHelper.registerKeyBinding(HalisonCodeClient.EDITOR_KEY);
        *///?}
        ClientPlayNetworking.registerGlobalReceiver(RelayPayloads.Effect.TYPE, (payload, context) -> RobotEffectsClient.receive(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(RelayPayloads.Camera.TYPE, (payload, context) -> RobotCameraClient.receive(context.client(), payload));
        ClientTickEvents.START_CLIENT_TICK.register(RobotCameraClient::tick);
        ClientTickEvents.END_CLIENT_TICK.register(HalisonCodeClient::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientCommandTree::repair);
        // Before 26.3 GameRendererMixin hides the hand (Fabric API for 26.1 has no level extraction events).
        //? if >=26.3
        LevelExtractionEvents.END_EXTRACTION.register(context -> RobotCameraClient.extracted(context.levelState()));
        //? if >=26.1 {
        HudElementRegistry.addLast(RobotCameraClient.HUD, RobotCameraClient::hud);
        //?} else
        /*HudRenderCallback.EVENT.register(RobotCameraClient::hud);*/
        ClientLifecycleEvents.CLIENT_STARTED.register(HalisonCodeClient::started);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> HalisonCodeClient.stopping());
        // Runs on the render thread.
        ClientPlayNetworking.registerGlobalReceiver(RelayPayloads.ToClient.TYPE, (payload, context) -> HalisonCodeClient.receive(payload.json()));
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> HalisonCodeClient.disconnected());
        // "/halisoncode chat", "signal" and "server" are the server's commands (issues #27, #249 and #279). Fabric sends a
        // line to the server only when the client tree says "unknown or incomplete command", so the client knows
        // "chat <text>" without running it.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> command = HalisonCodeClient.command();
            for (String server : new String[] {"chat", "signal", "server"}) {
                command.then(LiteralArgumentBuilder.<FabricClientCommandSource>literal(server)
                        .then(RequiredArgumentBuilder.argument("text", StringArgumentType.greedyString())));
            }
            dispatcher.register(command);
        });
    }
}
