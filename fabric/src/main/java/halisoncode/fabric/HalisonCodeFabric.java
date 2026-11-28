package halisoncode.fabric;

import halisoncode.HalisonCode;
import halisoncode.net.RelayPayloads;
import halisoncode.net.ServerRelay;
import halisoncode.robot.RobotCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.ChatType;

/** Fabric entrypoint: registers the payloads and wires Fabric's events to the common hooks in {@link HalisonCode}. */
public final class HalisonCodeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HalisonCode.init();
        //? if >=26.1 {
        PayloadTypeRegistry.serverboundPlay().register(RelayPayloads.ToServer.TYPE, RelayPayloads.ToServer.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RelayPayloads.ToClient.TYPE, RelayPayloads.ToClient.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RelayPayloads.Camera.TYPE, RelayPayloads.Camera.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RelayPayloads.Effect.TYPE, RelayPayloads.Effect.CODEC);
        //?} else {
        /*PayloadTypeRegistry.playC2S().register(RelayPayloads.ToServer.TYPE, RelayPayloads.ToServer.CODEC);
        PayloadTypeRegistry.playS2C().register(RelayPayloads.ToClient.TYPE, RelayPayloads.ToClient.CODEC);
        PayloadTypeRegistry.playS2C().register(RelayPayloads.Camera.TYPE, RelayPayloads.Camera.CODEC);
        PayloadTypeRegistry.playS2C().register(RelayPayloads.Effect.TYPE, RelayPayloads.Effect.CODEC);
        *///?}
        // Runs on the server thread (Fabric Javadoc).
        ServerPlayNetworking.registerGlobalReceiver(RelayPayloads.ToServer.TYPE, (payload, context) -> ServerRelay.receive(context.player(), payload.json()));
        ServerEntityEvents.ENTITY_LOAD.register(HalisonCode::entityLoaded);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> HalisonCode.entityUnloaded(entity));
        ServerTickEvents.END_SERVER_TICK.register(HalisonCode::serverTick);
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> HalisonCode.playerLeft(listener.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> HalisonCode.serverStopped());
        // Issue #27: chat lines (not /say or /me) on the server thread; false keeps a line for its sender's program.
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) ->
                !(params.chatType().is(ChatType.CHAT) && HalisonCode.chat(sender, message.signedContent())));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> HalisonCode.died(entity));
        // Issue #89: server side, after the block is gone; a break another mod refused in BEFORE never gets here.
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> HalisonCode.blockBroken(player, state));
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> RobotCommands.register(dispatcher));
    }
}
