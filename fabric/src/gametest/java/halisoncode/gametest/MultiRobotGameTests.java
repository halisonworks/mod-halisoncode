package halisoncode.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import halisoncode.HalisonCode;
import halisoncode.Messages;
import halisoncode.net.ServerRelay;
import halisoncode.robot.Result;
import halisoncode.robot.RobotCommand;
import halisoncode.robot.RobotEntity;
import halisoncode.robot.RobotException;
import halisoncode.robot.RobotLinks;
import halisoncode.robot.RobotManager;
import halisoncode.robot.RobotPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Several robots per player (issues #140, #141 and #151). Tests that need more than one robot per player run in the
 * test environment halisoncode-gametest:max_robots (the game rule at 3); the rest run with the default rule, 1.
 */
public class MultiRobotGameTests {
    private static final BlockPos START = new BlockPos(1, 2, 1);
    private static final String MORE = "halisoncode-gametest:max_robots";

    //? if <1.21.5 {
    /*// Before 1.21.5 there are no test environments: MORE is a batch, and the rule is set for it and put back after.
    @net.minecraft.gametest.framework.BeforeBatch(batch = MORE)
    public void raiseRule(net.minecraft.server.level.ServerLevel level) {
        GameTestCompat.setMaxRobots(level.getServer(), 3);
    }

    @net.minecraft.gametest.framework.AfterBatch(batch = MORE)
    public void resetRule(net.minecraft.server.level.ServerLevel level) {
        GameTestCompat.setMaxRobots(level.getServer(), 1);
    }
    *///?}

    /** A mock player (creative, not op) standing next to START. */
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(START.east())));
        return player;
    }

    /** Runs a /robot command as the player and returns what it printed. */
    private static List<String> run(ServerPlayer player, String command) {
        List<String> out = new ArrayList<>();
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                out.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withSource(capture), command);
        return out;
    }

    private static RobotLinks.Key key(ServerPlayer player, String name) {
        return new RobotLinks.Key(player.getUUID(), name);
    }

    private static RobotEntity spawn(GameTestHelper helper, ServerPlayer player, String name, BlockPos at) {
        return RobotManager.spawn(helper.getLevel(), key(player, name), Vec3.atBottomCenterOf(helper.absolutePos(at)), Direction.SOUTH);
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) helper.getLevel().getServer().getPlayerList().remove(p);
    }

    // Issue #278: messages read like the game's own: its errors for wrong arguments, plain words and no internal codes.
    @GameTest(maxTicks = 100)
    public void commandMessagesReadLikeTheGames(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        List<String> all = new ArrayList<>();
        says(helper, all, run(player, "robot move forward 1"), "You have no robot. Use /robot spawn to get one");
        says(helper, all, run(player, "robot status"), "You have no robot. Use /robot spawn to get one");
        says(helper, all, run(player, "robot status Miner"), "You have no robot called Miner");
        says(helper, all, run(player, "robot spawn"), "Kintro spawned, facing ");
        RobotManager.robot(player.getUUID()).snapTo(Vec3.atBottomCenterOf(helper.absolutePos(START)));
        says(helper, all, run(player, "robot move forward 0"), "must not be less than 1");
        says(helper, all, run(player, "robot move forward 65"), "must not be more than 64");
        says(helper, all, run(player, "robot slot 28"), "must not be more than 27");
        says(helper, all, run(player, "robot give nosuchblock 1"), "Unknown block or item \"nosuchblock\"");
        says(helper, all, run(player, "robot give 99999999999 1"), "Unknown block or item");
        says(helper, all, run(player, "robot color blue"), "Invalid color \"blue\"");
        says(helper, all, run(player, "robot animation dance"), "Incorrect argument");
        says(helper, all, run(player, "robot status"), "Commands waiting: ");
        says(helper, all, run(player, "robot list"), "default robot (agent)");
        List<String> give = run(player, "robot give stone 5 3");
        List<String> place = run(player, "robot place forward");
        List<String> detect = run(player, "robot detect up");
        List<String> inspect = run(player, "robot inspect up");
        List<String> turn = run(player, "robot turn left");
        List<String> move = run(player, "robot move up 1");
        helper.succeedWhen(() -> {
            says(helper, all, give, "Gave Kintro 5 [Stone] in slot 3");
            says(helper, all, place, "Kintro could not place a block (forward)");
            says(helper, all, detect, "No block in the way (up)");
            says(helper, all, inspect, "The block (up) is Air");
            says(helper, all, turn, "Kintro turned left");
            says(helper, all, move, "Kintro moved up, blocks: 1");
            for (String text : all) {
                helper.assertTrue(!text.matches("(?s).*(agent\\.|\\bok (true|false|null)|[A-Z]+_[A-Z]+|UNLOADED|STOPPED).*"), "internal code in: " + text);
            }
            leave(helper, player);
        });
    }

    private static void says(GameTestHelper helper, List<String> all, List<String> out, String text) {
        all.addAll(out);
        helper.assertTrue(out.toString().contains(text), "expected \"" + text + "\", got " + out);
    }

    // A vanilla client on a Halison Code server has no Halison Code keys, so every message carries its English text.
    @GameTest
    public void messagesCarryEnglishForVanillaClients(GameTestHelper helper) {
        Component text = Messages.of("message.halisoncode.robot.none", "/robot spawn");
        helper.assertTrue(text.getContents() instanceof TranslatableContents t && "You have no robot. Use %s to get one".equals(t.getFallback()), "fallback of " + text);
        helper.succeed();
    }

    // With the default rule (1) a player has the default robot only: a named robot is refused with the rule's name.
    @GameTest(maxTicks = 100)
    public void defaultRuleRefusesANamedRobot(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertValueEqual(HalisonCode.maxRobots(helper.getLevel().getServer()), 1, "default rule");
        run(player, "robot spawn");
        helper.assertTrue(RobotManager.robot(player.getUUID()) != null, "the default robot spawns as before");
        List<String> out = run(player, "robot spawn Miner");
        helper.assertTrue(RobotManager.robot(key(player, "Miner")) == null, "no second robot");
        helper.assertTrue(out.toString().contains("halisoncode:max_robots"), "the refusal names the game rule: " + out);
        leave(helper, player);
        helper.succeed();
    }

    // Issue #437: a player on a chest (0.875 high) has the chest at the feet. /robot spawn, with and without a name, puts
    // the robot in the free block above, so setHome is above the chest and storeItems(Down, 0) fills it. A slab and a
    // path go the same way; on a full block the robot stands where the player does.
    @GameTest(maxTicks = 100, environment = MORE)
    public void spawnOnAChestGoesOnTopOfIt(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos chest = START.east(2);
        helper.setBlock(chest, Blocks.CHEST);
        player.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(chest)).add(0, 0.875, 0));
        helper.assertValueEqual(player.blockPosition(), helper.absolutePos(chest), "the chest is the block at the feet");
        run(player, "robot spawn");
        run(player, "robot spawn Baerer");
        RobotEntity agent = RobotManager.robot(player.getUUID());
        RobotEntity baerer = RobotManager.robot(key(player, "Baerer"));
        helper.assertValueEqual(agent.blockPosition(), helper.absolutePos(chest.above()), "agent on top of the chest");
        helper.assertValueEqual(baerer.blockPosition(), helper.absolutePos(chest.above()), "Baerer on top of the chest");
        BlockPos slab = START.south(2);
        helper.setBlock(slab, Blocks.STONE_SLAB);
        helper.assertValueEqual(RobotManager.spawnBlock(helper.getLevel(), helper.absolutePos(slab)), helper.absolutePos(slab.above()), "on a slab");
        BlockPos path = START.south(3);
        helper.setBlock(path, Blocks.DIRT_PATH);
        helper.assertValueEqual(RobotManager.spawnBlock(helper.getLevel(), helper.absolutePos(path)), helper.absolutePos(path.above()), "on a path");
        helper.assertValueEqual(RobotManager.spawnBlock(helper.getLevel(), helper.absolutePos(START)), helper.absolutePos(START), "in a free block");
        agent.inventory().setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        RobotLinks.Key key = key(player, "");
        List<Result> results = new ArrayList<>();
        try {
            RobotManager.submit(key, RobotManager.queue(key).run(), RobotCommand.parse("agent.setHome", List.of()), results::add);
            RobotManager.submit(key, RobotManager.queue(key).run(), RobotCommand.parse("agent.storeItems", List.of(RobotCommand.DOWN, 0)), results::add);
        } catch (RobotException e) {
            helper.fail("rejected: " + e.result());
        }
        helper.runAfterDelay(40, () -> {
            helper.assertValueEqual(results, List.of(Result.ok(null), Result.ok(true)), "setHome, then storeItems(Down, 0)");
            helper.assertTrue(agent.home() != null && agent.home().pos().equals(helper.absolutePos(chest.above())), "home above the chest: " + agent.home());
            var box = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(chest));
            helper.assertValueEqual(box.getItem(0).getCount(), 5, "cobblestone in the chest");
            leave(helper, player);
            helper.succeed();
        });
    }

    // Issue #297: /gamerule turns the pause on lost focus off and on again; the client reads it (RobotClientGameTest).
    @GameTest(maxTicks = 100)
    public void pauseOnLostFocusRule(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        helper.assertTrue(HalisonCode.pauseOnLostFocus(server), "on by default");
        for (boolean value : new boolean[] {false, true}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule halisoncode:pause_on_lost_focus " + value);
            helper.assertTrue(HalisonCode.pauseOnLostFocus(server) == value, "set to " + value);
        }
        helper.succeed();
    }

    // Rule 3: the default robot and two named ones; a fourth is refused; names are checked and unique per player.
    @GameTest(maxTicks = 100, environment = MORE)
    public void raisedRuleAllowsNamedRobotsUpToTheLimit(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        run(player, "robot spawn");
        run(player, "robot spawn Miner");
        run(player, "robot spawn Digger");
        helper.assertTrue(RobotManager.robot(key(player, "Miner")) != null && RobotManager.robot(key(player, "Digger")) != null, "two named robots");
        helper.assertTrue(run(player, "robot spawn Third").toString().contains("halisoncode:max_robots"), "a fourth robot is refused");
        helper.assertTrue(RobotManager.robot(key(player, "Third")) == null, "no fourth robot");
        helper.assertTrue(run(player, "robot spawn miner").toString().contains("already have a robot called Miner"), "Miner and miner are one name");
        helper.assertTrue(run(player, "robot spawn all").toString().contains("choose another name"), "reserved word");
        // Spawning a robot the player has moves it, at the limit too.
        RobotEntity miner = RobotManager.robot(key(player, "Miner"));
        run(player, "robot spawn Miner");
        helper.assertTrue(RobotManager.robot(key(player, "Miner")) == miner, "the same robot, moved");
        // Each robot has its own inventory.
        miner.inventory().setItem(0, new ItemStack(Items.STONE, 3));
        helper.assertTrue(RobotManager.robot(player.getUUID()).inventory().isEmpty(), "the default robot's inventory is its own");
        List<String> list = run(player, "robot list");
        helper.assertTrue(list.toString().contains("default robot (agent)") && list.toString().contains("Miner") && list.toString().contains("Digger"), "list: " + list);
        miner.inventory().clearContent();
        RobotManager.robot(key(player, "Digger")).discard();
        leave(helper, player);
        helper.succeed();
    }

    // Lowering the rule only stops new robots: the ones a player has stay and keep working.
    @GameTest(maxTicks = 100)
    public void loweredRuleKeepsExistingRobots(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        RobotEntity miner = spawn(helper, player, "Miner", START); // as if the rule was higher before
        helper.assertTrue(run(player, "robot spawn Digger").toString().contains("halisoncode:max_robots"), "no new robot at rule 1");
        helper.assertTrue(RobotManager.robot(key(player, "Miner")) == miner && !miner.isRemoved(), "Miner stays");
        run(player, "robot move forward 2 Miner");
        helper.succeedWhen(() -> {
            helper.assertValueEqual(miner.blockPosition(), helper.absolutePos(START.south(2)), "Miner moved");
            leave(helper, player);
        });
    }

    // /robot remove: every item moves to the player; what does not fit drops at the player's feet; nothing is lost.
    @GameTest(maxTicks = 100, environment = MORE)
    public void removeGivesTheItemsToThePlayer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        RobotEntity miner = spawn(helper, player, "Miner", START);
        miner.inventory().setItem(0, new ItemStack(Items.STONE, 10));
        miner.inventory().setItem(5, new ItemStack(Items.DIRT, 64));
        RobotEntity digger = spawn(helper, player, "Digger", START.south(2));
        digger.inventory().setItem(0, new ItemStack(Items.COBBLESTONE, 7));
        List<String> out = run(player, "robot remove Miner");
        helper.assertTrue(miner.isRemoved() && RobotManager.robot(key(player, "Miner")) == null, "Miner is gone: " + out);
        helper.assertValueEqual(player.getInventory().countItem(Items.STONE), 10, "stone in the player's inventory");
        helper.assertValueEqual(player.getInventory().countItem(Items.DIRT), 64, "dirt in the player's inventory");
        helper.assertTrue(run(player, "robot remove Miner").toString().contains("no robot called Miner"), "removed for good");
        // A full inventory: the rest drops where the player stands.
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) player.getInventory().setItem(i, new ItemStack(Items.GLASS, 64));
        run(player, "robot remove Digger");
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(3),
                e -> e.isAlive() && e.getItem().is(Items.COBBLESTONE)).stream().mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertValueEqual(dropped, 7, "cobblestone dropped at the player");
        helper.assertTrue(digger.isRemoved(), "Digger is gone");
        leave(helper, player);
        helper.succeed();
    }

    // Issue #324: the default robot can be removed too. Its items go to the player, the program stops, and the name and
    // the look come back with the next /robot spawn.
    @GameTest(maxTicks = 100, environment = MORE)
    public void theDefaultRobotCanBeRemoved(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(run(player, "robot remove agent").toString().contains("You have no robot"), "nothing to remove");
        RobotEntity agent = spawn(helper, player, "", START);
        RobotEntity miner = spawn(helper, player, "Miner", START.south(2));
        miner.inventory().setItem(0, new ItemStack(Items.DIRT, 3));
        run(player, "robot rename agent Bob");
        run(player, "robot skin mint agent");
        run(player, "robot color 3366FF agent");
        agent.inventory().setItem(0, new ItemStack(Items.STONE, 10));
        List<JsonObject> inbox = new ArrayList<>();
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"control\",\"op\":\"start\"}", r -> inbox.add(JsonParser.parseString(r).getAsJsonObject()));
        String move = ",\"run\":" + inbox.getLast().get("run").getAsLong() + ",\"op\":\"agent.move\",\"args\":[0,5]";
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"cmd\",\"id\":1" + move + "}", r -> inbox.add(JsonParser.parseString(r).getAsJsonObject()));
        helper.assertValueEqual(run(player, "robot remove Bob"), List.of("Robot Bob is removed. Its items are in your inventory or at your feet"), "the answer");
        helper.assertTrue(agent.isRemoved() && RobotManager.robot(player.getUUID()) == null, "the default robot is gone");
        helper.assertValueEqual(player.getInventory().countItem(Items.STONE), 10, "stone in the player's inventory");
        helper.assertTrue(inbox.getLast().toString().contains("STOPPED"), "the waiting command was stopped: " + inbox);
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"cmd\",\"id\":2" + move + ",\"robot\":\"Miner\"}", r -> inbox.add(JsonParser.parseString(r).getAsJsonObject()));
        helper.assertTrue(inbox.getLast().toString().contains("STOPPED"), "the program is over: " + inbox);
        helper.assertTrue(RobotManager.robot(key(player, "Miner")) == miner && miner.inventory().getItem(0).getCount() == 3, "Miner stays");
        helper.assertTrue(suggested(player, "robot come ").equals(List.of("Miner")), "only Miner is suggested: " + suggested(player, "robot come "));
        helper.assertTrue(!run(player, "robot list").toString().contains("Bob"), "not in the list");
        for (String command : List.of("robot status", "robot remove agent")) { // issue #327: the player still has Miner
            helper.assertValueEqual(run(player, command), List.of("You have no default robot. Use /robot spawn to get one"), command);
        }
        // Back with its name and look, and empty.
        run(player, "robot spawn");
        RobotEntity again = RobotManager.robot(player.getUUID());
        helper.assertTrue(again != null && again != agent, "a new robot");
        helper.assertTrue(again.getCustomName() != null && again.getCustomName().getString().equals("Bob"), "its name");
        helper.assertValueEqual(again.skin() + " " + again.primaryColor(), "mint " + 0x3366FF, "its look");
        helper.assertTrue(again.inventory().isEmpty(), "no items twice");
        // A full inventory: the rest drops where the player stands.
        again.inventory().setItem(0, new ItemStack(Items.COBBLESTONE, 7));
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) player.getInventory().setItem(i, new ItemStack(Items.GLASS, 64));
        run(player, "robot remove agent");
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(3),
                e -> e.isAlive() && e.getItem().is(Items.COBBLESTONE)).stream().mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertValueEqual(dropped, 7, "cobblestone dropped at the player");
        // Another robot may take the name of a removed one; the default robot then comes back without it.
        helper.assertTrue(run(player, "robot rename Miner Bob").toString().contains("is now called Bob"), "the name is free");
        run(player, "robot spawn");
        RobotEntity third = RobotManager.robot(player.getUUID());
        helper.assertTrue(third.getCustomName() == null && RobotManager.robot(key(player, "Bob")) == miner, "one Bob");
        // Not loaded: refused, it holds the only copy of its items.
        third.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        helper.assertTrue(run(player, "robot remove agent").toString().contains("is not loaded"), "refused while not loaded");
        leave(helper, player);
        helper.succeed();
    }

    // Issue #141: two robots of one player, each with its own queue, move at the same time in one run.
    @GameTest(maxTicks = 100)
    public void twoRobotsRunInParallel(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.snapTo(Vec3.atCenterOf(helper.absolutePos(START).above(100)));
        RobotEntity agent = spawn(helper, player, "", START);
        RobotEntity miner = spawn(helper, player, "Miner", START.east(3));
        List<JsonObject> inbox = new ArrayList<>();
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"control\",\"op\":\"start\"}", r -> inbox.add(JsonParser.parseString(r).getAsJsonObject()));
        long run = inbox.getLast().get("run").getAsLong();
        String move = ",\"run\":" + run + ",\"op\":\"agent.move\",\"args\":[0,5]";
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"cmd\",\"id\":1" + move + "}", r -> inbox.add(JsonParser.parseString(r).getAsJsonObject()));
        ServerRelay.handle(player, "{\"v\":2,\"t\":\"cmd\",\"id\":2" + move + ",\"robot\":\"Miner\"}", r -> inbox.add(JsonParser.parseString(r).getAsJsonObject()));
        // Five blocks take 20 ticks; one after the other would take 40.
        helper.runAfterDelay(5 * HalisonCode.limits().ticksPerBlock() + 6, () -> {
            helper.assertValueEqual(agent.blockPosition(), helper.absolutePos(START.south(5)), "the default robot moved");
            helper.assertValueEqual(miner.blockPosition(), helper.absolutePos(START.east(3).south(5)), "Miner moved at the same time");
            helper.assertValueEqual(inbox.stream().filter(m -> m.get("t").getAsString().equals("result")).count(), 2L, "both answered: " + inbox);
            leave(helper, player);
            helper.succeed();
        });
    }

    // Another player can name the robot but never reach it: names are looked up among the sender's own robots.
    @GameTest(maxTicks = 100)
    public void othersCannotUseSomeonesNamedRobot(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerPlayer other = player(helper);
        RobotEntity miner = spawn(helper, owner, "Miner", START);
        List<String> replies = new ArrayList<>();
        ServerRelay.handle(other, "{\"v\":2,\"t\":\"control\",\"op\":\"start\"}", replies::add);
        long run = JsonParser.parseString(replies.getLast()).getAsJsonObject().get("run").getAsLong();
        ServerRelay.handle(other, "{\"v\":2,\"t\":\"cmd\",\"id\":1,\"run\":" + run + ",\"op\":\"agent.move\",\"args\":[0,2],\"robot\":\"Miner\"}", replies::add);
        helper.assertTrue(replies.getLast().contains("UNLOADED"), "the stranger has no Miner: " + replies.getLast());
        run(other, "robot move forward 2 Miner");
        run(other, "robot remove Miner");
        run(other, "robot rename Miner Thief");
        // A name tag from a stranger does nothing either.
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Thief"));
        other.setItemInHand(InteractionHand.MAIN_HAND, tag);
        GameTestCompat.interact(miner, other);
        helper.runAfterDelay(20, () -> {
            helper.assertValueEqual(miner.blockPosition(), helper.absolutePos(START), "Miner did not move");
            helper.assertTrue(!miner.isRemoved() && RobotManager.robot(key(owner, "Miner")) == miner, "Miner is still the owner's");
            helper.assertValueEqual(miner.getCustomName().getString(), "Miner", "name unchanged");
            leave(helper, owner, other);
            helper.succeed();
        });
    }

    // Issue #151: /robot rename and a name tag rename a robot; the default robot only shows a name and stays agent.
    // Issue #435: Norwegian letters in names, typed in commands: spawn, a command with the name last, rename of a named
    // robot and of the default robot. A name with a space is answered with the name rules, not a parser error.
    @GameTest(maxTicks = 100, environment = MORE)
    public void namesWithNorwegianLetters(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        run(player, "robot spawn");
        List<String> spawned = run(player, "robot spawn Bærer");
        RobotEntity baerer = RobotManager.robot(key(player, "Bærer"));
        helper.assertTrue(baerer != null, "Bærer spawned: " + spawned);
        helper.assertValueEqual(baerer.getCustomName().getString(), "Bærer", "name tag");
        helper.assertTrue(run(player, "robot status Bærer").toString().contains("Bærer"), "a command with the name last");
        run(player, "robot rename Bærer Åse");
        helper.assertTrue(RobotManager.robot(key(player, "Åse")) == baerer && RobotManager.robot(key(player, "Bærer")) == null, "renamed to Åse");
        run(player, "robot rename Øyvind");
        helper.assertValueEqual(RobotManager.robot(player.getUUID()).getCustomName().getString(), "Øyvind", "the default robot renamed");
        List<String> spaced = run(player, "robot spawn Bær er");
        helper.assertTrue(spaced.toString().contains("starts with a letter"), "the name rules answer: " + spaced);
        helper.assertTrue(RobotManager.robot(key(player, "Bær er")) == null, "no robot with a space");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(maxTicks = 100, environment = MORE)
    public void renameByCommandAndNameTag(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        RobotEntity agent = spawn(helper, player, "", START);
        RobotEntity miner = spawn(helper, player, "Miner", START.south(2));
        spawn(helper, player, "Digger", START.south(4));
        helper.assertTrue(miner.isCustomNameVisible() && miner.getCustomName().getString().equals("Miner"), "a named robot shows its name");
        run(player, "robot rename Miner Builder");
        helper.assertTrue(RobotManager.robot(key(player, "Builder")) == miner && RobotManager.robot(key(player, "Miner")) == null, "renamed");
        helper.assertValueEqual(miner.getCustomName().getString(), "Builder", "tag after rename");
        helper.assertTrue(run(player, "robot rename Builder digger").toString().contains("already have a robot called Digger"), "names stay unique");
        run(player, "robot rename Bob");
        helper.assertTrue(RobotManager.robot(player.getUUID()) == agent, "the default robot stays the default");
        helper.assertValueEqual(agent.getCustomName().getString(), "Bob", "the default robot's tag");
        helper.assertTrue(run(player, "robot rename Builder").toString().contains("/robot rename Builder <name>"), "the new name is missing");
        // A name tag item, used by the owner.
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Tagged"));
        player.setItemInHand(InteractionHand.MAIN_HAND, tag);
        GameTestCompat.interact(miner, player);
        helper.assertTrue(RobotManager.robot(key(player, "Tagged")) == miner, "renamed by the name tag");
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "a name tag does not open the inventory");
        // Name tags off and on again.
        run(player, "robot nametag off");
        helper.assertTrue(!miner.isCustomNameVisible() && !agent.isCustomNameVisible(), "tags hidden");
        run(player, "robot nametag on Tagged");
        helper.assertTrue(miner.isCustomNameVisible() && !agent.isCustomNameVisible(), "one tag shown");
        RobotManager.robot(key(player, "Digger")).discard();
        leave(helper, player);
        helper.succeed();
    }

    // Issue #293: agent, and the name the default robot was given, mean the default robot in every command.
    @GameTest(maxTicks = 100, environment = MORE)
    public void theDefaultRobotHasANameInCommands(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(suggested(player, "robot skin mint ").isEmpty(), "no robot, nothing to suggest");
        RobotEntity agent = spawn(helper, player, "", START);
        RobotEntity miner = spawn(helper, player, "Miner", START.south(2));
        helper.assertTrue(run(player, "robot list").toString().contains("default robot (agent)"), "the list without a name");
        run(player, "robot skin mint agent");
        helper.assertValueEqual(agent.skin(), "mint", "skin by agent");
        run(player, "robot rename agent Bob");
        helper.assertValueEqual(agent.getCustomName().getString(), "Bob", "renamed by agent");
        helper.assertTrue(run(player, "robot list").toString().contains("Bob (agent)"), "the list with a name");
        helper.assertValueEqual(run(player, "robot skin industrial Bob"), List.of("Robot Bob now has the skin industrial"), "skin by its name");
        helper.assertValueEqual(agent.skin(), "industrial", "skin by its name");
        run(player, "robot color 3366FF Bob");
        helper.assertValueEqual(agent.primaryColor(), 0x3366FF, "colour by its name");
        helper.assertValueEqual(miner.skin(), "workshop", "Miner keeps its own skin");
        for (String text : List.of("robot rename ", "robot skin mint ", "robot color reset ", "robot come ", "robot status ", "robot stop ",
                "robot nametag on ", "robot move forward 1 ", "robot spawn ", "robot inventory ")) {
            helper.assertTrue(suggested(player, text).containsAll(List.of("agent", "Bob", "Miner")), text + "suggests " + suggested(player, text));
        }
        // Names are unique, the default robot's name too.
        helper.assertTrue(run(player, "robot rename Miner bob").toString().contains("already have a robot called Bob"), "rename to the default's name");
        helper.assertTrue(run(player, "robot spawn bob").toString().contains("already have a robot called Bob"), "spawn with the default's name");
        helper.assertTrue(run(player, "robot rename Miner Agent").toString().contains("is a word the /robot commands use"), "agent is taken");
        helper.assertTrue(run(player, "robot rename agent Miner").toString().contains("already have a robot called Miner"), "rename to Miner's name");
        for (String word : List.of("Miner", "agent", "Bob")) {
            helper.assertValueEqual(run(player, "robot rename " + word), List.of("Write the new name too: /robot rename " + word + " <name>"), "no new name");
        }
        helper.assertTrue(run(player, "robot rename Bob bob").toString().contains("is now called bob"), "another case of its own name");
        run(player, "robot rename bob Bob");
        // Spawn moves it.
        run(player, "robot spawn Bob");
        helper.assertTrue(RobotManager.robot(player.getUUID()) == agent && RobotManager.robot(key(player, "Bob")) == null, "spawn Bob moves the default robot");
        // The tag and the queue of the default robot alone.
        run(player, "robot nametag off agent");
        helper.assertTrue(!agent.isCustomNameVisible() && miner.isCustomNameVisible(), "only the default robot's tag is hidden");
        run(player, "robot nametag on Bob");
        helper.assertTrue(agent.isCustomNameVisible(), "shown again");
        run(player, "robot move forward 6 Bob");
        run(player, "robot move forward 6 Miner");
        run(player, "robot stop agent");
        helper.assertValueEqual(RobotManager.queue(key(player, "")).size(), 0, "the default robot's queue");
        helper.assertTrue(RobotManager.queue(key(player, "Miner")).size() > 0, "Miner goes on");
        // The name is kept when the robot is spawned again while it is not loaded, and a rename works then too.
        agent.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        helper.assertTrue(run(player, "robot status Bob").toString().contains("Robot Bob is not loaded"), "not loaded");
        run(player, "robot rename Bob Rob");
        run(player, "robot spawn");
        RobotEntity again = RobotManager.robot(player.getUUID());
        helper.assertTrue(again != agent && again.getCustomName() != null && again.getCustomName().getString().equals("Rob"), "the name after a spawn");
        leave(helper, player);
        helper.succeed();
    }

    // Issue #293: a world saved before the name was in robots.dat has it only as the robot's tag.
    @GameTest(maxTicks = 100, environment = MORE)
    public void aTagFromAnOlderWorldBecomesTheDefaultRobotsName(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        RobotEntity agent = spawn(helper, player, "", START);
        spawn(helper, player, "Miner", START.south(2));
        for (String tag : List.of("Miner", "My robot", "Bob")) {
            agent.setCustomName(Component.literal(tag));
            CompoundTag saved = GameTestCompat.save(agent, helper.getLevel().registryAccess());
            agent.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            agent = new RobotEntity(HalisonCode.ROBOT.get(), helper.getLevel());
            GameTestCompat.load(agent, saved, helper.getLevel().registryAccess());
            helper.getLevel().addFreshEntity(agent);
            helper.assertValueEqual(agent.getCustomName().getString(), tag, "the tag stays");
            helper.assertTrue(suggested(player, "robot come ").contains("Bob") == tag.equals("Bob"), "only Bob becomes its name, not " + tag);
        }
        helper.assertTrue(run(player, "robot status Miner").toString().contains("Robot Miner is at"), "a named robot goes first");
        helper.assertTrue(run(player, "robot status Bob").toString().contains("Robot Bob is at"), "the default robot by its old tag");
        leave(helper, player);
        helper.succeed();
    }

    /** What Tab offers after {@code text}. */
    private static List<String> suggested(ServerPlayer player, String text) {
        var commands = player.level().getServer().getCommands().getDispatcher();
        return commands.getCompletionSuggestions(commands.parse(text, player.createCommandSourceStack())).join().getList().stream()
                .map(s -> s.getText()).toList();
    }

    // /robot stop with a name stops that robot only; without a name all of the player's robots.
    @GameTest(maxTicks = 100, environment = MORE)
    public void stopOneOrAll(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.snapTo(Vec3.atCenterOf(helper.absolutePos(START).above(100)));
        RobotEntity agent = spawn(helper, player, "", START);
        RobotEntity miner = spawn(helper, player, "Miner", START.east(3));
        run(player, "robot move forward 6");
        run(player, "robot move forward 6 Miner");
        helper.runAfterDelay(2, () -> run(player, "robot stop Miner"));
        helper.runAfterDelay(40, () -> {
            helper.assertValueEqual(agent.blockPosition(), helper.absolutePos(START.south(6)), "the default robot went on");
            helper.assertTrue(miner.blockPosition().getZ() - helper.absolutePos(START).getZ() < 3, "Miner stopped early");
            run(player, "robot move forward 6");
            run(player, "robot move forward 6 Miner");
            run(player, "robot stop");
            helper.assertValueEqual(RobotManager.queue(key(player, "")).size() + RobotManager.queue(key(player, "Miner")).size(), 0, "both queues empty");
            leave(helper, player);
            helper.succeed();
        });
    }

    // A robot saved before named robots (no OwnerName, only the default link) loads as the player's default robot.
    @GameTest(maxTicks = 100)
    public void oldSaveLoadsAsTheDefaultRobot(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        RobotEntity first = RobotManager.spawn(helper.getLevel(), owner, Vec3.atBottomCenterOf(helper.absolutePos(START)), Direction.SOUTH);
        first.inventory().setItem(0, new ItemStack(Items.STONE, 4));
        CompoundTag saved = GameTestCompat.save(first, helper.getLevel().registryAccess());
        saved.remove("OwnerName");
        first.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        RobotEntity back = new RobotEntity(HalisonCode.ROBOT.get(), helper.getLevel());
        GameTestCompat.load(back, saved, helper.getLevel().registryAccess());
        helper.getLevel().addFreshEntity(back);
        helper.assertTrue(RobotManager.robot(owner) == back, "the old robot is the default robot");
        helper.assertValueEqual(back.name(), "", "no name");
        helper.assertValueEqual(back.inventory().getItem(0).getCount(), 4, "its items");
        helper.assertTrue(!back.hasCustomName(), "no name tag");
        try {
            RobotManager.submit(owner, RobotManager.queue(owner).run(), RobotCommand.parse("agent.move", List.of(0, 1)), r -> { });
        } catch (Exception e) {
            helper.fail("a command for the old robot: " + e);
        }
        helper.succeedWhen(() -> {
            helper.assertValueEqual(back.blockPosition(), helper.absolutePos(START.south()), "it still takes commands");
            back.inventory().clearContent();
            back.discard();
        });
    }

    // Issue #242: /robot skin and /robot color stay with a named robot, and its name with them, through a save and a
    // load and through /robot spawn while it is unloaded. Only that robot changes. A save from before styles is workshop.
    @GameTest(maxTicks = 100, environment = MORE)
    public void styleStaysWithTheRobotThroughSaveLoadAndRespawn(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        run(player, "robot spawn");
        run(player, "robot spawn Miner");
        helper.assertTrue(run(player, "robot skin industrial Miner").toString().contains("industrial"), "skin set");
        run(player, "robot color 3366FF Miner");
        helper.assertTrue(run(player, "robot skin chrome Miner").toString().contains("Incorrect argument"), "unknown skin refused as the game does");
        RobotEntity miner = RobotManager.robot(key(player, "Miner"));
        RobotEntity first = RobotManager.robot(player.getUUID());
        helper.assertValueEqual(miner.skin(), "industrial", "Miner's skin");
        helper.assertValueEqual(miner.primaryColor(), 0x3366FF, "Miner's colour");
        helper.assertValueEqual(first.skin(), "workshop", "the default robot keeps its own skin");

        CompoundTag saved = GameTestCompat.save(miner, helper.getLevel().registryAccess());
        RobotEntity back = new RobotEntity(HalisonCode.ROBOT.get(), helper.getLevel());
        GameTestCompat.load(back, saved, helper.getLevel().registryAccess());
        helper.assertValueEqual(back.skin(), "industrial", "skin after a load");
        helper.assertValueEqual(back.primaryColor(), 0x3366FF, "colour after a load");
        helper.assertValueEqual(back.getCustomName().getString(), "Miner", "name after a load");
        saved.remove("Skin");
        saved.remove("PrimaryColor");
        RobotEntity old = new RobotEntity(HalisonCode.ROBOT.get(), helper.getLevel());
        GameTestCompat.load(old, saved, helper.getLevel().registryAccess());
        helper.assertValueEqual(old.skin(), "workshop", "a save from before styles");
        helper.assertValueEqual(old.primaryColor(), -1, "no colour in a save from before styles");

        miner.inventory().clearContent();
        miner.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        run(player, "robot spawn Miner"); // a new robot in its place, from the links
        RobotEntity respawned = RobotManager.robot(key(player, "Miner"));
        helper.assertTrue(respawned != null && respawned != miner, "Miner spawned again");
        helper.assertValueEqual(respawned.skin(), "industrial", "skin after a spawn while unloaded");
        helper.assertValueEqual(respawned.primaryColor(), 0x3366FF, "colour after a spawn while unloaded");
        leave(helper, player);
        helper.succeed();
    }

    /** Queues agent.goToRobot(target) for the player's robot {@code robot} ("" for the default robot); the result goes in the list. */
    private static List<Result> goTo(GameTestHelper helper, ServerPlayer player, String robot, String target) {
        return goTo(new ArrayList<>(), helper, player, robot, target);
    }

    private static List<Result> goTo(List<Result> results, GameTestHelper helper, ServerPlayer player, String robot, String target) {
        RobotLinks.Key key = key(player, robot);
        try {
            RobotManager.submit(key, RobotManager.queue(key).run(), RobotCommand.parse("agent.goToRobot", List.of(target)), results::add);
        } catch (RobotException e) {
            helper.fail("goToRobot rejected: " + e.result());
        }
        return results;
    }

    // Issue #243: the default robot walks to the player's Miner, past another player's Miner that is nearer, and ends
    // level with it, next to it and facing it. Then Miner, already next to it, goes to agent: it only turns. Going to
    // itself answers true at once.
    @GameTest(maxTicks = 200)
    public void goToAnotherRobotEndsNextToItFacingIt(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ServerPlayer stranger = player(helper);
        RobotEntity agent = spawn(helper, player, "", START);
        RobotEntity miner = spawn(helper, player, "Miner", START.south(4).east(2));
        RobotEntity decoy = spawn(helper, stranger, "Miner", START.east(2));
        List<Result> there = goTo(helper, player, "", "Miner");
        List<Result> beside = new ArrayList<>();
        List<Result> self = new ArrayList<>();
        BlockPos minerAt = miner.blockPosition();
        helper.runAfterDelay(80, () -> {
            helper.assertValueEqual(there, List.of(Result.ok(true)), "arrived");
            BlockPos at = agent.blockPosition();
            helper.assertValueEqual(at.distManhattan(minerAt), 1, "next to the player's Miner");
            helper.assertValueEqual(at.getY(), minerAt.getY(), "level with it");
            helper.assertValueEqual(agent.getDirection(), RobotPath.direction(at, minerAt), "facing it");
            helper.assertTrue(at.distManhattan(decoy.blockPosition()) > 1, "not at the other player's Miner");
            goTo(beside, helper, player, "Miner", "");
            goTo(self, helper, player, "", "");
        });
        helper.runAfterDelay(100, () -> {
            helper.assertValueEqual(beside, List.of(Result.ok(true)), "Miner was already next to agent");
            helper.assertValueEqual(miner.blockPosition(), minerAt, "Miner did not move");
            helper.assertValueEqual(miner.getDirection(), RobotPath.direction(minerAt, agent.blockPosition()), "Miner faces agent");
            helper.assertValueEqual(self, List.of(Result.ok(true, "agent is this robot itself, so it is already there")), "to itself");
            decoy.discard();
            leave(helper, player, stranger);
            helper.succeed();
        });
    }

    // Issue #243: a target with no free block next to it answers false at once; an unknown name says which robots the
    // player has (never another player's); a robot that is not loaded is UNLOADED; another player's robot of the
    // same name is never found.
    @GameTest(maxTicks = 100)
    public void goToRobotRefusals(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ServerPlayer stranger = player(helper);
        RobotEntity agent = spawn(helper, player, "", START);
        BlockPos shut = START.south(4).above(2);
        for (Direction d : Direction.values()) helper.setBlock(shut.relative(d), Blocks.STONE);
        spawn(helper, player, "Miner", shut);
        spawn(helper, player, "Digger", START.east(4)).remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        RobotEntity secret = spawn(helper, stranger, "Secret", START.east(2));
        List<Result> results = goTo(helper, player, "", "Miner");
        List<Result> nobody = goTo(helper, player, "", "Nobody");
        List<Result> digger = goTo(helper, player, "", "Digger");
        List<Result> strangers = goTo(helper, stranger, "Secret", "Miner");
        helper.succeedWhen(() -> {
            helper.assertValueEqual(results, List.of(Result.ok(false, Messages.robot("robot.halisoncode.no_free_block", "robot Miner"))), "boxed in");
            helper.assertValueEqual(results.get(0).message(), "there is no free block next to robot Miner", "English");
            helper.assertValueEqual(nobody, List.of(Result.error(Result.Code.BAD_ARGS,
                    "you have no robot called Nobody; your robots are agent (the default robot), Digger, Miner")), "unknown name: the player's robots");
            helper.assertValueEqual(digger, List.of(Result.error(Result.Code.UNLOADED, "robot Digger is not loaded (see /robot list)")), "not loaded");
            helper.assertValueEqual(strangers, List.of(Result.error(Result.Code.BAD_ARGS,
                    "you have no robot called Miner; your robots are Secret")), "the stranger has no Miner, and sees only their own names");
            helper.assertValueEqual(agent.blockPosition(), helper.absolutePos(START), "agent did not move");
            helper.assertValueEqual(secret.blockPosition(), helper.absolutePos(START.east(2)), "Secret did not move");
            secret.discard();
            leave(helper, player, stranger);
        });
    }
}
