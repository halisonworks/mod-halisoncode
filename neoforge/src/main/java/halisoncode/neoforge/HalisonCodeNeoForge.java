package halisoncode.neoforge;

import halisoncode.HalisonCode;
import halisoncode.Messages;
import halisoncode.net.RelayPayloads;
import halisoncode.net.ServerRelay;
import halisoncode.robot.RobotCommands;
import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
//? if >=26.1 {
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
//?} else
/*import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;*/
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
//? if <26.1
/*import net.neoforged.neoforge.network.handling.IPayloadHandler;*/

/** NeoForge entrypoint: registers the payloads and wires NeoForge's events to the common hooks in {@link HalisonCode}. */
@Mod(HalisonCode.MOD_ID)
public final class HalisonCodeNeoForge {
    /** Payload protocol version; NeoForge compares it between client and server. */
    private static final String NETWORK_VERSION = "1";
    public static final ConfigurationTask.Type REFUSE_UNMODDED = new ConfigurationTask.Type("halisoncode:refuse_unmodded");

    //? if <26.1 {
    /*// Before RegisterClientPayloadHandlersEvent the client handlers are given at registration, here; the client
    // entrypoint (constructed before the event) sets these, so this class never names client classes.
    public static IPayloadHandler<RelayPayloads.ToClient> toClient = (payload, context) -> { };
    public static IPayloadHandler<RelayPayloads.Camera> camera = (payload, context) -> { };
    public static IPayloadHandler<RelayPayloads.Effect> effect = (payload, context) -> { };
    *///?}

    public HalisonCodeNeoForge(IEventBus modBus) {
        HalisonCode.init(); // creates the DeferredRegisters on modBus (NeoForgePlatform)
        modBus.addListener(HalisonCodeNeoForge::registerPayloads);
        modBus.addListener(HalisonCodeNeoForge::refuseUnmodded);
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener((ServerTickEvent.Post event) -> HalisonCode.serverTick(event.getServer()));
        game.addListener((ServerStoppedEvent event) -> HalisonCode.serverStopped());
        game.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) HalisonCode.playerLeft(player);
        });
        // Lowest priority and not when cancelled: only an entity that really joins the level counts as loaded.
        game.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent event) -> {
            if (event.getLevel() instanceof ServerLevel level) HalisonCode.entityLoaded(event.getEntity(), level);
        });
        game.addListener((EntityLeaveLevelEvent event) -> {
            if (event.getLevel() instanceof ServerLevel) HalisonCode.entityUnloaded(event.getEntity());
        });
        // Issue #27: fired on the server thread, not when another mod cancelled the line; cancelled keeps it for its sender's program.
        game.addListener((ServerChatEvent event) -> {
            if (HalisonCode.chat(event.getPlayer(), event.getRawText())) event.setCanceled(true);
        });
        // Lowest priority and not when cancelled: only a death that happens counts.
        game.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> HalisonCode.died(event.getEntity()));
        // Issue #89: fired before the block goes; lowest priority and not when cancelled, so only a break that happens.
        // 26.1 renamed BlockEvent.BreakEvent to BreakBlockEvent.
        //? if >=26.1 {
        game.addListener(EventPriority.LOWEST, (BreakBlockEvent event) -> HalisonCode.blockBroken(event.getPlayer(), event.getState()));
        //?} else
        /*game.addListener(EventPriority.LOWEST, (BreakEvent event) -> HalisonCode.blockBroken(event.getPlayer(), event.getState()));*/
        game.addListener(NeoForgePlatform::gatherPermissions);
        game.addListener((RegisterCommandsEvent event) -> RobotCommands.register(event.getDispatcher()));
    }

    // Issue #371: NeoForge's registry sync refuses NeoForge without Halison Code, but does not ask an unmodified client, which
    // would then be disconnected by the first robot it sees. The task never finishes, so nobody gets past it.
    private static void refuseUnmodded(RegisterConfigurationTasksEvent event) {
        var listener = event.getListener();
        if (!listener.getConnectionType().isOther()) return;
        event.register(new ConfigurationTask() {
            @Override
            public void start(Consumer<Packet<?>> send) {
                listener.disconnect(Messages.of("message.halisoncode.join.neoforge"));
            }

            @Override
            public Type type() {
                return REFUSE_UNMODDED;
            }
        });
    }

    // Optional: a client with Halison Code may join a server without it, and canSend tells the common code which it is.
    // The client's handlers for ToClient and Camera are registered in HalisonCodeNeoForgeClient.
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION).optional();
        // Handlers run on the server thread (NeoForge's default HandlerThread.MAIN), as Fabric's receiver does.
        registrar.playToServer(RelayPayloads.ToServer.TYPE, RelayPayloads.ToServer.CODEC,
                (payload, context) -> ServerRelay.receive((ServerPlayer) context.player(), payload.json()));
        //? if >=26.1 {
        registrar.playToClient(RelayPayloads.ToClient.TYPE, RelayPayloads.ToClient.CODEC);
        registrar.playToClient(RelayPayloads.Camera.TYPE, RelayPayloads.Camera.CODEC);
        registrar.playToClient(RelayPayloads.Effect.TYPE, RelayPayloads.Effect.CODEC);
        //?} else {
        /*registrar.playToClient(RelayPayloads.ToClient.TYPE, RelayPayloads.ToClient.CODEC, (payload, context) -> toClient.handle(payload, context));
        registrar.playToClient(RelayPayloads.Camera.TYPE, RelayPayloads.Camera.CODEC, (payload, context) -> camera.handle(payload, context));
        registrar.playToClient(RelayPayloads.Effect.TYPE, RelayPayloads.Effect.CODEC, (payload, context) -> effect.handle(payload, context));
        *///?}
    }
}
