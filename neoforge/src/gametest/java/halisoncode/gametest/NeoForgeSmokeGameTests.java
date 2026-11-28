package halisoncode.gametest;

import halisoncode.HalisonCode;
import halisoncode.Permission;
import halisoncode.net.RelayPayloads;
import halisoncode.net.ServerRelay;
import halisoncode.neoforge.HalisonCodeNeoForge;
import halisoncode.platform.Platform;
import halisoncode.robot.Result;
import halisoncode.robot.RobotCamera;
import halisoncode.robot.RobotCommand;
import halisoncode.robot.RobotException;
import halisoncode.robot.RobotManager;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=1.21.5 {
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
//?} else {
/*import java.io.File;
import java.util.Collection;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;
*///?}
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
//? if >=1.21.5
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.11 {
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;
//?} else
/*import net.minecraft.world.level.GameRules;*/
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
//? if >=1.21.5
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
//? if >=1.21.5
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandler;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

/**
 * Smoke GameTests for the NeoForge glue (issue #187): spawn, move, place, game rules (incl. the enum rule), payloads and
 * the FakePlayer inside a claim. The full suite (fabric/src/gametest) stays on Fabric; these bodies use the same
 * vanilla GameTestHelper. Run with ./gradlew :neoforge:runGameTestServer (part of `build`). The structure
 * halisoncode:gametest_empty is 8x8x8 air, like Fabric's fabric-gametest-api-v1:empty. Before 1.21.5 (issue #189) the
 * tests come from a @GameTestGenerator in a @GameTestHolder instead of the test registries.
 */
//? if <1.21.5
/*@GameTestHolder(HalisonCode.MOD_ID)*/
@EventBusSubscriber(modid = HalisonCode.MOD_ID)
public final class NeoForgeSmokeGameTests {
    private static final BlockPos START = new BlockPos(1, 2, 1);
    private static final Identifier STRUCTURE = id("gametest_empty");
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("robot_spawns", NeoForgeSmokeGameTests::robotSpawns);
        TESTS.put("robot_moves", NeoForgeSmokeGameTests::robotMoves);
        TESTS.put("robot_places_block", NeoForgeSmokeGameTests::robotPlacesBlock);
        TESTS.put("game_rules_registered", NeoForgeSmokeGameTests::gameRulesRegistered);
        TESTS.put("payload_types_registered", NeoForgeSmokeGameTests::payloadTypesRegistered);
        TESTS.put("client_without_neoforge_is_refused", NeoForgeSmokeGameTests::clientWithoutNeoForgeIsRefused);
        TESTS.put("fake_player_respects_claims", NeoForgeSmokeGameTests::fakePlayerRespectsClaims);
        TESTS.put("fake_player_contract", NeoForgeSmokeGameTests::fakePlayerContract);
        TESTS.put("adventure_owner_cannot_build", NeoForgeSmokeGameTests::adventureOwnerCannotBuild);
        TESTS.put("chat_line_for_the_program", NeoForgeSmokeGameTests::chatLineForTheProgram);
        TESTS.put("block_broken_for_the_program", NeoForgeSmokeGameTests::blockBrokenForTheProgram);
        TESTS.put("permission_handler_decides", NeoForgeSmokeGameTests::permissionHandlerDecides);
    }

    private NeoForgeSmokeGameTests() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(HalisonCode.MOD_ID, path);
    }

    //? if >=1.21.5 {
    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        TESTS.forEach((name, test) -> event.register(Registries.TEST_FUNCTION, id(name), () -> test));
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("smoke"));
        TESTS.keySet().forEach(name -> event.registerTest(id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, id(name)), new TestData<>(environment, STRUCTURE, 100, 0, true))));
    }
    //?} else {
    /*@GameTestGenerator
    public static Collection<TestFunction> tests() {
        // No --report argument before 1.21.5: the build passes the report file as a system property.
        String report = System.getProperty("halisoncode.gameTestReport");
        if (report != null) {
            try {
                new File(report).getParentFile().mkdirs();
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File(report)));
            } catch (Exception e) {
                throw new IllegalStateException("GameTest report " + report, e);
            }
        }
        List<TestFunction> tests = new ArrayList<>();
        TESTS.forEach((name, test) -> tests.add(new TestFunction("halisoncode", "halisoncode." + name, STRUCTURE.toString(), 100, 0, true, test)));
        return tests;
    }
    *///?}

    // ---- helpers, as in fabric/src/gametest/java/halisoncode/gametest/RobotGameTests.java

    private static UUID spawn(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        RobotManager.spawn(helper.getLevel(), owner, Vec3.atBottomCenterOf(helper.absolutePos(START)), Direction.SOUTH);
        return owner;
    }

    private static List<Result> send(GameTestHelper helper, UUID owner, String op, Object... args) {
        List<Result> results = new ArrayList<>();
        try {
            RobotManager.submit(owner, RobotManager.queue(owner).run(), RobotCommand.parse(op, List.of(args)), results::add);
        } catch (RobotException e) {
            helper.fail(op + " rejected: " + e.result());
        }
        return results;
    }

    // ---- tests

    private static void robotSpawns(GameTestHelper helper) {
        UUID owner = spawn(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(RobotManager.robot(owner) != null, "robot loaded (EntityJoinLevelEvent reached RobotManager)");
            helper.assertValueEqual(RobotManager.robot(owner).blockPosition(), helper.absolutePos(START), "position");
            helper.assertValueEqual(RobotManager.robot(owner).getType(), HalisonCode.ROBOT.get(), "entity type from the DeferredRegister");
        });
    }

    private static void robotMoves(GameTestHelper helper) {
        UUID owner = spawn(helper);
        List<Result> results = send(helper, owner, "agent.move", RobotCommand.FORWARD, 5);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(results, List.of(Result.ok(true)), "results");
            helper.assertValueEqual(RobotManager.robot(owner).blockPosition(), helper.absolutePos(START.south(5)), "position");
        });
    }

    private static void robotPlacesBlock(GameTestHelper helper) {
        UUID owner = spawn(helper);
        RobotManager.robot(owner).inventory().setItem(0, new ItemStack(Items.STONE, 5));
        // Issue #53: the accessor from halisoncode-common.mixins.json; an item kept for somebody else stays.
        for (UUID keptFor : new UUID[] {UUID.randomUUID(), owner}) {
            ItemEntity item = new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(keptFor == owner ? Items.GOLD_INGOT : Items.DIAMOND), 0, 0, 0);
            item.setPos(Vec3.atBottomCenterOf(helper.absolutePos(START)));
            item.setNoGravity(true);
            item.setTarget(keptFor);
            helper.getLevel().addFreshEntity(item);
        }
        List<Result> collect = send(helper, owner, "agent.collectAll");
        List<Result> results = send(helper, owner, "agent.place", RobotCommand.FORWARD);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(collect, List.of(Result.ok(null)), "collect");
            helper.assertValueEqual(RobotManager.robot(owner).inventory().countItem(Items.GOLD_INGOT), 1, "the owner's item");
            helper.assertValueEqual(RobotManager.robot(owner).inventory().countItem(Items.DIAMOND), 0, "somebody else's item");
            helper.assertValueEqual(results, List.of(Result.ok(true)), "results");
            helper.assertBlockPresent(Blocks.STONE, START.south());
            helper.assertValueEqual(RobotManager.robot(owner).inventory().getItem(0).getCount(), 4, "stone left");
        });
    }

    private static void gameRulesRegistered(GameTestHelper helper) {
        //? if >=1.21.11 {
        GameRules rules = helper.getLevel().getGameRules();
        helper.assertValueEqual(BuiltInRegistries.GAME_RULE.getKey(HalisonCode.FREE_ITEMS.get()), id("free_items"), "free_items id");
        helper.assertValueEqual(BuiltInRegistries.GAME_RULE.getKey(HalisonCode.MAX_ROBOTS.get()), id("max_robots"), "max_robots id");
        helper.assertValueEqual(BuiltInRegistries.GAME_RULE.getKey(HalisonCode.ROBOT_CAMERA.get()), id("robot_camera"), "robot_camera id");
        helper.assertValueEqual(rules.get(HalisonCode.FREE_ITEMS.get()), false, "free_items default");
        helper.assertValueEqual(rules.get(HalisonCode.MAX_ROBOTS.get()), 1, "max_robots default");
        helper.assertValueEqual(BuiltInRegistries.GAME_RULE.getKey(HalisonCode.PAUSE_ON_LOST_FOCUS.get()), id("pause_on_lost_focus"), "pause_on_lost_focus id");
        helper.assertValueEqual(rules.get(HalisonCode.PAUSE_ON_LOST_FOCUS.get()), true, "pause_on_lost_focus default");
        GameRule<RobotCamera.Mode> camera = HalisonCode.ROBOT_CAMERA.get();
        helper.assertValueEqual(rules.get(camera), RobotCamera.Mode.owner, "robot_camera default");
        helper.assertValueEqual(camera.gameRuleType(), GameRuleType.valueOf("HALISONCODE_ENUM"), "enum rule type from enumextensions.json");
        // Saved as the constant name, as on Fabric, so a world keeps its value when it moves between loaders.
        helper.assertValueEqual(camera.valueCodec().encodeStart(JsonOps.INSTANCE, RobotCamera.Mode.ops).getOrThrow().getAsString(), "ops", "saved form");
        // /gamerule parses the value with NeoForge's EnumArgument; out-of-range ints are refused by the int rule.
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule halisoncode:robot_camera ops");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule halisoncode:max_robots 99");
        try {
            helper.assertValueEqual(rules.get(camera), RobotCamera.Mode.ops, "robot_camera after /gamerule");
            helper.assertValueEqual(rules.get(HalisonCode.MAX_ROBOTS.get()), 1, "max_robots keeps its range");
        } finally {
            rules.set(camera, RobotCamera.Mode.owner, server);
        }
        //?} else {
        /*// Before 1.21.11 a rule is a GameRules key named by the id (NeoForgePlatform).
        MinecraftServer server = helper.getLevel().getServer();
        GameRules rules = helper.getLevel().getGameRules();
        helper.assertValueEqual(HalisonCode.FREE_ITEMS.get().getId(), "halisoncode:free_items", "free_items name");
        helper.assertValueEqual(HalisonCode.MAX_ROBOTS.get().getId(), "halisoncode:max_robots", "max_robots name");
        helper.assertValueEqual(HalisonCode.ROBOT_CAMERA.get().getId(), "halisoncode:robot_camera", "robot_camera name");
        helper.assertValueEqual(HalisonCode.freeItems(server), false, "free_items default");
        helper.assertValueEqual(HalisonCode.maxRobots(server), 1, "max_robots default");
        helper.assertValueEqual(HalisonCode.PAUSE_ON_LOST_FOCUS.get().getId(), "halisoncode:pause_on_lost_focus", "pause_on_lost_focus name");
        helper.assertValueEqual(HalisonCode.pauseOnLostFocus(server), true, "pause_on_lost_focus default");
        helper.assertValueEqual(HalisonCode.robotCamera(server), RobotCamera.Mode.owner, "robot_camera default");
        // Saved as the constant name, as on 26.x and on Fabric.
        helper.assertValueEqual(rules.getRule(HalisonCode.ROBOT_CAMERA.get()).serialize(), "owner", "saved form");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule halisoncode:robot_camera ops");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule halisoncode:max_robots 99");
        try {
            helper.assertValueEqual(HalisonCode.robotCamera(server), RobotCamera.Mode.ops, "robot_camera after /gamerule");
            helper.assertValueEqual(HalisonCode.maxRobots(server), 1, "max_robots keeps its range");
        } finally {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule halisoncode:robot_camera owner");
        }
        *///?}
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void payloadTypesRegistered(GameTestHelper helper) {
        roundTrip(helper, new RelayPayloads.ToServer("{\"t\":\"hello\"}"), PacketFlow.SERVERBOUND);
        roundTrip(helper, new RelayPayloads.ToClient("{\"t\":\"ack\"}"), PacketFlow.CLIENTBOUND);
        roundTrip(helper, new RelayPayloads.Camera(42, true), PacketFlow.CLIENTBOUND);
        roundTrip(helper, new RelayPayloads.Effect(2, new net.minecraft.core.BlockPos(1, -64, 3)), PacketFlow.CLIENTBOUND);
        // A FakePlayer has no client, so the common code never sends it a payload.
        ServerPlayer fake = Platform.INSTANCE.fakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "HalisonCode[smoke]"));
        helper.assertFalse(Platform.INSTANCE.canSend(fake, RelayPayloads.ToClient.TYPE), "canSend to a FakePlayer");
        helper.succeed();
    }

    // Issue #371: a client without NeoForge is refused when it connects; a NeoForge client (registry sync checks it) is not.
    private static void clientWithoutNeoForgeIsRefused(GameTestHelper helper) {
        for (ConnectionType type : ConnectionType.values()) {
            var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), "HalisonCode_join"), 0, ClientInformation.createDefault(), false, type);
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection); // NeoForge's own listeners look the client's channels up on it
            var listener = new ServerConfigurationPacketListenerImpl(helper.getLevel().getServer(), connection, cookie);
            var tasks = ModLoader.postEventWithReturn(new RegisterConfigurationTasksEvent(listener)).getConfigurationTasks();
            boolean refused = tasks.stream().anyMatch(task -> task.type().equals(HalisonCodeNeoForge.REFUSE_UNMODDED));
            helper.assertTrue(refused == type.isOther(), type + " refused: " + refused);
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void roundTrip(GameTestHelper helper, CustomPacketPayload payload, PacketFlow flow) {
        var codec = (StreamCodec<FriendlyByteBuf, CustomPacketPayload>) NetworkRegistry.getCodec(payload.type().id(), ConnectionProtocol.PLAY, flow);
        helper.assertTrue(codec != null, payload.type().id() + " registered for " + flow);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, payload);
        helper.assertValueEqual(codec.decode(buf), payload, payload.type().id() + " round trip");
    }

    /** Absolute positions a claim mod protects: it refuses the FakePlayer's click there, as claim mods do. */
    private static final Set<BlockPos> CLAIMED = ConcurrentHashMap.newKeySet();
    /** Who the claim mod saw clicking; the robot's FakePlayer carries the owner's UUID. */
    private static final Set<UUID> CLICKERS = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void claimMod(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof FakePlayer player && CLAIMED.contains(event.getPos())) {
            CLICKERS.add(player.getUUID());
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private static void fakePlayerRespectsClaims(GameTestHelper helper) {
        BlockPos claimed = helper.absolutePos(START.south());
        CLAIMED.add(claimed);
        UUID owner = spawn(helper);
        RobotManager.robot(owner).inventory().setItem(0, new ItemStack(Items.STONE, 5));
        List<Result> results = send(helper, owner, "agent.place", RobotCommand.FORWARD);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(results, List.of(Result.ok(false)), "results");
            CLAIMED.remove(claimed);
            helper.assertBlockPresent(Blocks.AIR, START.south());
            helper.assertValueEqual(RobotManager.robot(owner).inventory().getItem(0).getCount(), 5, "stone kept");
            helper.assertTrue(CLICKERS.contains(owner), "the claim mod saw the owner's UUID");
        });
    }

    /** Owners whose robots' clicks another mod looks at, and what that mod saw in RightClickBlock (issue #246). */
    private static final Set<UUID> WATCHED = ConcurrentHashMap.newKeySet();
    private static final Set<Use> USES = ConcurrentHashMap.newKeySet();

    private record Use(UUID owner, String name, BlockPos pos) {
    }

    @SubscribeEvent
    public static void otherMod(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof FakePlayer player && WATCHED.contains(player.getUUID())) {
            USES.add(new Use(player.getUUID(), player.getGameProfile().name(), event.getPos().immutable()));
        }
    }

    // Issue #246, a contract with other mods (docs/architecture.md, section 3): the robot uses a block as a FakePlayer
    // with the owner's UUID and the name HalisonCode[<owner name>], and RightClickBlock fires with it.
    private static void fakePlayerContract(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(helper.absolutePos(START).above(100)));
        UUID owner = player.getUUID();
        WATCHED.add(owner);
        helper.setBlock(START.south(), Blocks.OAK_FENCE_GATE);
        helper.setBlock(START.east(), Blocks.BARREL);
        RobotManager.spawn(helper.getLevel(), owner, Vec3.atBottomCenterOf(helper.absolutePos(START)), Direction.SOUTH);
        RobotManager.robot(owner).inventory().setItem(0, new ItemStack(Items.STONE, 5));
        List<Result> use = send(helper, owner, "agent.interact", RobotCommand.FORWARD);
        List<Result> place = send(helper, owner, "agent.place", RobotCommand.UP);
        List<Result> size = send(helper, owner, "agent.containerSize", RobotCommand.LEFT);
        String name = "HalisonCode[" + player.getGameProfile().name() + "]";
        helper.succeedWhen(() -> {
            helper.assertValueEqual(List.of(use, place, size), List.of(List.of(Result.ok(true)), List.of(Result.ok(true)),
                    List.of(Result.ok(27))), "results");
            Set<Use> seen = USES.stream().filter(u -> u.owner().equals(owner)).collect(java.util.stream.Collectors.toSet());
            helper.assertValueEqual(seen, Set.of(new Use(owner, name, helper.absolutePos(START.south())),
                    new Use(owner, name, helper.absolutePos(START.above())), new Use(owner, name, helper.absolutePos(START.east()))), "clicks");
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    // Issue #53: the FakePlayer from FakePlayerFactory follows the owner's adventure rule: no placing, no breaking.
    private static void adventureOwnerCannotBuild(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atCenterOf(helper.absolutePos(START).above(100)));
        player.setGameMode(GameType.ADVENTURE);
        helper.setBlock(START.west(), Blocks.STONE);
        UUID owner = player.getUUID();
        RobotManager.spawn(helper.getLevel(), owner, Vec3.atBottomCenterOf(helper.absolutePos(START)), Direction.SOUTH);
        RobotManager.robot(owner).inventory().setItem(0, new ItemStack(Items.STONE, 5));
        List<Result> place = send(helper, owner, "agent.place", RobotCommand.FORWARD);
        List<Result> destroy = send(helper, owner, "agent.destroy", RobotCommand.RIGHT);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(place, List.of(Result.ok(false)), "place");
            helper.assertValueEqual(destroy, List.of(Result.ok(false)), "destroy");
            helper.assertBlockPresent(Blocks.AIR, START.south());
            helper.assertBlockPresent(Blocks.STONE, START.west());
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    // Issue #27: ServerChatEvent reaches the common hook; a line the running program listens for is cancelled (not
    // broadcast) and becomes an event, any other line is left alone.
    private static void chatLineForTheProgram(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<String> inbox = new ArrayList<>();
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"control\",\"op\":\"start\"}", inbox::add);
        long run = RobotManager.queue(player.getUUID()).run();
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"control\",\"op\":\"subscribe\",\"run\":" + run + ",\"chat\":[\"jump\"]}", inbox::add);
        ServerChatEvent jump = NeoForge.EVENT_BUS.post(new ServerChatEvent(player, "jump 3", Component.literal("jump 3")));
        ServerChatEvent hello = NeoForge.EVENT_BUS.post(new ServerChatEvent(player, "hello 3", Component.literal("hello 3")));
        helper.assertTrue(jump.isCanceled(), "jump held back");
        helper.assertTrue(!hello.isCanceled(), "hello broadcast");
        helper.succeedWhen(() -> {
            helper.assertTrue(inbox.contains("{\"v\":2,\"t\":\"event\",\"name\":\"chat\",\"args\":[\"jump\",3.0,0.0,0.0]}"), "event: " + inbox);
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    // Issue #89: the block break event reaches the common hook through a real break (NeoForge fires it inside
    // destroyBlock); the stone the program listens for becomes an event and the break is not changed.
    private static void blockBrokenForTheProgram(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.setBlock(START.east(2), Blocks.STONE);
        List<String> inbox = new ArrayList<>();
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"control\",\"op\":\"start\"}", inbox::add);
        long run = RobotManager.queue(player.getUUID()).run();
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"control\",\"op\":\"subscribe\",\"run\":" + run + ",\"broken\":[1]}", inbox::add);
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(START.east(2))), "broken");
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, START.east(2));
            helper.assertTrue(inbox.contains("{\"v\":2,\"t\":\"event\",\"name\":\"blockBroken\",\"args\":[1]}"), "event: " + inbox);
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    // Issue #51: Halison Code's permissions are registered, and what the server's permission handler answers is followed.
    // NeoForge picks the handler from its server config, so the test swaps the field for one test player.
    private static void permissionHandlerDecides(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<String> names = PermissionAPI.getRegisteredNodes().stream().map(PermissionNode::getNodeName).toList();
        for (Permission p : Permission.values()) helper.assertTrue(names.contains(p.node()), p.node() + " is registered");
        helper.assertTrue(Permission.ROBOT.allows(player), "robot by default");
        helper.assertTrue(!Permission.TELEPORT.allows(player), "teleport by default");
        try {
            Field field = PermissionAPI.class.getDeclaredField("activeHandler");
            field.setAccessible(true);
            IPermissionHandler before = (IPermissionHandler) field.get(null);
            // A proxy, because Stonecutter would rename the interface's getIdentifier as it renames the class.
            field.set(null, Proxy.newProxyInstance(IPermissionHandler.class.getClassLoader(), new Class<?>[] {IPermissionHandler.class}, (proxy, method, args) -> {
                Object value = method.invoke(before, args);
                return method.getName().equals("getPermission") && args[0] == player ? (Object) !(Boolean) value : value;
            }));
            try {
                helper.assertTrue(!Permission.ROBOT.allows(player), "robot refused");
                helper.assertTrue(Permission.TELEPORT.allows(player), "teleport allowed");
                List<String> inbox = new ArrayList<>();
                ServerRelay.handle(player, "{\"v\":2,\"t\":\"control\",\"op\":\"start\"}", inbox::add);
                long run = RobotManager.queue(player.getUUID()).run();
                ServerRelay.handle(player, "{\"v\":2,\"t\":\"cmd\",\"id\":1,\"run\":" + run + ",\"op\":\"agent.move\",\"args\":[0,1]}", inbox::add);
                helper.assertTrue(inbox.getLast().contains("NO_PERMISSION") && inbox.getLast().contains("halisoncode.robot"), "refused: " + inbox.getLast());
            } finally {
                field.set(null, before);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        helper.assertTrue(Permission.ROBOT.allows(player), "robot by default again");
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }
}
