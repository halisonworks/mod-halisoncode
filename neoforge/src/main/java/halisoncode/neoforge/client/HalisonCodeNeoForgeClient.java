package halisoncode.neoforge.client;

import halisoncode.HalisonCode;
import halisoncode.client.HalisonCodeClient;
import halisoncode.client.RobotCameraClient;
import halisoncode.client.RobotEffectsClient;
import halisoncode.client.RobotRenderer;
import halisoncode.client.RobotScreen;
import halisoncode.net.RelayPayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//? if >=26.3
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
//? if >=26.1 {
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
//?} else {
/*import halisoncode.neoforge.HalisonCodeNeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
*///?}
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge client entrypoint: registers the renderer, screen, keys and HUD and wires NeoForge's client events. */
@Mod(value = HalisonCode.MOD_ID, dist = Dist.CLIENT)
public final class HalisonCodeNeoForgeClient {
    //? if <26.1
    /*private static boolean started;*/

    public HalisonCodeNeoForgeClient(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(HalisonCode.ROBOT.get(), RobotRenderer::new));
        modBus.addListener((RegisterMenuScreensEvent event) -> event.register(HalisonCode.ROBOT_MENU.get(), RobotScreen::new));
        // RobotCameraClient's KeyMapping.Category.register already adds the Halison Code category to vanilla's sort order
        // (deprecated on NeoForge, but it works); registerCategory as well would list it twice.
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(RobotCameraClient.KEY);
            event.register(RobotCameraClient.FOLLOW_KEY);
            event.register(HalisonCodeClient.EDITOR_KEY);
        });
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(RobotCameraClient.HUD, RobotCameraClient::hud));
        // Handlers run on the render thread (NeoForge's default HandlerThread.MAIN), as Fabric's receivers do.
        //? if >=26.1 {
        modBus.addListener((RegisterClientPayloadHandlersEvent event) -> {
            event.register(RelayPayloads.Camera.TYPE, (payload, context) -> RobotCameraClient.receive(Minecraft.getInstance(), payload));
            event.register(RelayPayloads.Effect.TYPE, (payload, context) -> RobotEffectsClient.receive(Minecraft.getInstance(), payload));
            event.register(RelayPayloads.ToClient.TYPE, (payload, context) -> HalisonCodeClient.receive(payload.json()));
        });
        //?} else {
        /*HalisonCodeNeoForge.camera = (payload, context) -> RobotCameraClient.receive(Minecraft.getInstance(), payload);
        HalisonCodeNeoForge.effect = (payload, context) -> RobotEffectsClient.receive(Minecraft.getInstance(), payload);
        HalisonCodeNeoForge.toClient = (payload, context) -> HalisonCodeClient.receive(payload.json());
        *///?}

        IEventBus game = NeoForge.EVENT_BUS;
        //? if >=26.1 {
        game.addListener((ClientTickEvent.Pre event) -> RobotCameraClient.tick(Minecraft.getInstance()));
        //? if >=26.3
        game.addListener((ExtractLevelRenderStateEvent event) -> RobotCameraClient.extracted(event.getRenderState()));
        game.addListener((ClientStartedEvent event) -> HalisonCodeClient.started(event.getClient()));
        game.addListener((ClientStoppingEvent event) -> HalisonCodeClient.stopping());
        //?} else {
        /*// NeoForge 21.1 has no client started event: the first client tick comes right after start-up. The hand is
        // hidden by GameRendererMixin there (no level extraction).
        game.addListener((ClientTickEvent.Pre event) -> {
            if (!started) {
                started = true;
                HalisonCodeClient.started(Minecraft.getInstance());
            }
            RobotCameraClient.tick(Minecraft.getInstance());
        });
        game.addListener((GameShuttingDownEvent event) -> HalisonCodeClient.stopping());
        *///?}
        game.addListener((ClientTickEvent.Post event) -> HalisonCodeClient.tick(Minecraft.getInstance()));
        game.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> HalisonCodeClient.disconnected());
        game.addListener((RegisterClientCommandsEvent event) ->
                event.getDispatcher().register(HalisonCodeClient.command()));
    }
}
