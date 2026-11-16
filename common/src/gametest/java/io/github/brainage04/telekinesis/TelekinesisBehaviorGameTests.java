package io.github.brainage04.telekinesis;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.brainagelib.drops.DropCapture;
import io.github.brainage04.brainagelib.gametest.CapturedCommandOutput;
import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.github.brainage04.telekinesis.platform.TelekinesisPlatform;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TelekinesisBehaviorGameTests {
    private static final BlockPos DROP_POS = new BlockPos(1, 1, 1);

    private TelekinesisBehaviorGameTests() {}

    public static void personalCommandsAndOperatorPermissions(GameTestHelper helper) {
        GameTestPlayer fixture =
                GameTestPlayer.join(helper, "TkCommands").gameMode(GameType.SURVIVAL);
        ServerPlayer player = fixture.player();
        boolean previousPersonal = TelekinesisPlayerSettings.isEnabled(player);
        CommandSourceStack source = player.createCommandSourceStack();
        try {
            TelekinesisGameTests.withEnabled(
                    helper,
                    true,
                    () -> {
                        fixture.clearMessages();
                        run(helper, source, "telekinesis disable");
                        check(
                                !TelekinesisPlayerSettings.isEffective(player),
                                "Disable must affect the player's drops");
                        check(
                                fixture.received(
                                        "["
                                                + Telekinesis.MOD_NAME
                                                + "] Your Telekinesis preference is now disabled."),
                                "The player must be told about the change, got "
                                        + fixture.messageTexts());
                        run(helper, source, "telekinesis toggle");
                        check(
                                TelekinesisPlayerSettings.isEffective(player),
                                "Toggle must restore the personal setting");
                        run(helper, source, "telekinesis enable");
                        fixture.clearMessages();
                        run(helper, source, "telekinesis status");
                        check(
                                fixture.received(
                                        "["
                                                + Telekinesis.MOD_NAME
                                                + "] Server: enabled. Personal preference: enabled."
                                                + " Effective state: enabled."),
                                "Status must report the gamerule, got " + fixture.messageTexts());
                        run(helper, source, "telekinesis help");
                    });
            TelekinesisGameTests.withEnabled(
                    helper,
                    false,
                    () -> {
                        TelekinesisPlayerSettings.setEnabled(player, true);
                        fixture.clearMessages();
                        run(helper, source, "telekinesis status");
                        check(
                                fixture.received(
                                        "["
                                                + Telekinesis.MOD_NAME
                                                + "] Server: disabled. Personal preference: enabled."
                                                + " Effective state: disabled."),
                                "Status must report the gamerule's off state, got "
                                        + fixture.messageTexts());
                        TelekinesisPlayerSettings.setEnabled(player, false);
                        fixture.clearMessages();
                        run(helper, source, "telekinesis enable");
                        check(
                                fixture.received(
                                        "["
                                                + Telekinesis.MOD_NAME
                                                + "] Your Telekinesis preference is now enabled"
                                                + " (the server-wide setting is disabled)."),
                                "A player must be told the rule is off, got "
                                        + fixture.messageTexts());
                    });
            try {
                helper.getLevel()
                        .getServer()
                        .getCommands()
                        .getDispatcher()
                        .execute("telekinesis config disable", source);
                throw new AssertionError("The server-wide switch is no longer a command");
            } catch (CommandSyntaxException expected) {
                check(
                        helper.getLevel().getGameRules().get(Telekinesis.ENABLED),
                        "A rejected command must not change the rule");
            }
        } finally {
            TelekinesisPlayerSettings.setEnabled(player, previousPersonal);
            fixture.close();
        }
        helper.succeed();
    }

    /**
     * The server-wide switch is a gamerule: /gamerule needs operator permission
     * (GameRuleCommand.java:22, register), so a player without it gets a parse error and the rule
     * keeps its value.
     */
    public static void serverWideSwitchIsTheGamerule(GameTestHelper helper) {
        GameTestPlayer fixture = GameTestPlayer.join(helper, "TkRule").gameMode(GameType.SURVIVAL);
        ServerPlayer player = fixture.player();
        try {
            TelekinesisGameTests.withEnabled(
                    helper,
                    true,
                    () -> {
                        boolean before = helper.getLevel().getGameRules().get(Telekinesis.ENABLED);
                        CommandSourceStack source = player.createCommandSourceStack();
                        try {
                            helper.getLevel()
                                    .getServer()
                                    .getCommands()
                                    .getDispatcher()
                                    .execute("gamerule telekinesis:enabled false", source);
                            throw new AssertionError(
                                    "A player without permission must not change the gamerule");
                        } catch (CommandSyntaxException expected) {
                            check(
                                    helper.getLevel().getGameRules().get(Telekinesis.ENABLED)
                                            == before,
                                    "A rejected /gamerule must not change the rule");
                        }
                    });
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /** The console reports the gamerule's state, and a console channel has no preference. */
    public static void consoleStatusReportsTheGamerule(GameTestHelper helper) {
        for (boolean on : new boolean[] {true, false}) {
            TelekinesisGameTests.withEnabled(
                    helper,
                    on,
                    () -> {
                        CapturedCommandOutput output = new CapturedCommandOutput();
                        CommandSourceStack source =
                                output.source(helper.getLevel().getServer());
                        run(helper, source, "telekinesis status");
                        check(
                                output.text()
                                        .equals(
                                                "[Telekinesis] Server-wide Telekinesis is "
                                                        + (on ? "enabled" : "disabled")
                                                        + "."),
                                "The console must report the rule's state, got " + output.text());
                        try {
                            helper.getLevel()
                                    .getServer()
                                    .getCommands()
                                    .getDispatcher()
                                    .execute("telekinesis disable", source);
                            throw new AssertionError(
                                    "The console must not have a personal preference");
                        } catch (CommandSyntaxException expected) {
                            check(
                                    helper.getLevel().getGameRules().get(Telekinesis.ENABLED) == on,
                                    "A console personal command must not change the rule");
                        }
                    });
        }
        helper.succeed();
    }

    public static void partialInventoryConservesOverflow(GameTestHelper helper) {
        ServerPlayer player = enabledPlayer(helper);
        for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 62));
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 1)));
        player.setPos(feet);
        helper.setBlock(DROP_POS, Blocks.CHEST);
        helper.getBlockEntity(DROP_POS, ChestBlockEntity.class)
                .setItem(0, new ItemStack(Items.DIAMOND, 6));
        destroy(helper, player);
        check(
                player.getInventory().getItem(1).getCount() == 64,
                "Available stack space must receive exactly two diamonds");
        int remaining = 0;
        for (ItemEntity entity :
                helper.getLevel()
                        .getEntitiesOfClass(ItemEntity.class, new AABB(feet, feet).inflate(1))) {
            if (entity.getItem().is(Items.DIAMOND)) {
                remaining += entity.getItem().getCount();
                check(
                        entity.position().distanceToSqr(feet) < 0.01,
                        "Overflow must be at the player's feet");
                check(entity.getDeltaMovement().equals(Vec3.ZERO), "Overflow must not fly away");
            }
        }
        check(remaining == 4, "Exactly four overflow diamonds must remain in the world");
        helper.succeed();
    }

    public static void silkTouchPreservesLootAndSuppressesExperience(GameTestHelper helper) {
        ServerPlayer player = enabledPlayer(helper);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(
                helper.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH),
                1);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        helper.setBlock(DROP_POS, Blocks.DIAMOND_ORE);
        int before = player.totalExperience;
        destroy(helper, player);
        check(
                player.getInventory().countItem(Items.DIAMOND_ORE) == 1,
                "Silk Touch must collect the ore, not diamonds");
        check(
                player.getInventory().countItem(Items.DIAMOND) == 0,
                "Silk Touch must not add normal loot too");
        check(player.totalExperience == before, "Silk Touch must not award ore experience");
        helper.succeed();
    }

    public static void experienceRepairsMendingBeforeAwardingPoints(GameTestHelper helper) {
        ServerPlayer player = enabledPlayer(helper);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(
                helper.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.MENDING),
                1);
        pickaxe.setDamageValue(100);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        helper.setBlock(DROP_POS, Blocks.DIAMOND_ORE);
        int before = player.totalExperience;
        destroy(helper, player);
        check(
                pickaxe.getDamageValue() < 100,
                "Ore experience must repair the equipped Mending tool");
        check(
                player.totalExperience == before,
                "Experience consumed by Mending must not be awarded twice");
        helper.succeed();
    }

    public static void unrelatedSpawnsRemainInWorldAfterBreak(GameTestHelper helper) {
        ServerPlayer player = enabledPlayer(helper);
        helper.setBlock(DROP_POS, Blocks.CHEST);
        destroy(helper, player);
        Vec3 position = Vec3.atCenterOf(helper.absolutePos(DROP_POS));
        ItemEntity item =
                new ItemEntity(
                        helper.getLevel(),
                        position.x,
                        position.y,
                        position.z,
                        new ItemStack(Items.EMERALD));
        check(
                helper.getLevel().addFreshEntity(item),
                "Unrelated items must not be captured after a block break");
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), position, Vec3.ZERO, 5);
        check(
                helper.getLevel().addFreshEntity(orb),
                "Unrelated experience must not be captured after a block break");
        check(
                player.getInventory().countItem(Items.EMERALD) == 0,
                "Unrelated emerald must not enter the player's inventory");
        helper.succeed();
    }

    public static void nestedBreaksRestoreOuterPlayer(GameTestHelper helper) {
        ServerPlayer outer = enabledPlayer(helper);
        ServerPlayer inner = enabledPlayer(helper);
        Vec3 position = Vec3.atCenterOf(helper.absolutePos(DROP_POS));
        DropCapture.begin(outer);
        try {
            DropCapture.begin(inner);
            try {
                helper.getLevel()
                        .addFreshEntity(
                                new ItemEntity(
                                        helper.getLevel(),
                                        position.x,
                                        position.y,
                                        position.z,
                                        new ItemStack(Items.EMERALD)));
            } finally {
                DropCapture.end();
            }
            helper.getLevel()
                    .addFreshEntity(
                            new ItemEntity(
                                    helper.getLevel(),
                                    position.x,
                                    position.y,
                                    position.z,
                                    new ItemStack(Items.DIAMOND)));
        } finally {
            DropCapture.end();
        }
        check(
                inner.getInventory().countItem(Items.EMERALD) == 1,
                "Nested drop must belong to the inner breaker");
        check(
                inner.getInventory().countItem(Items.DIAMOND) == 0,
                "Nested breaker must not retain the outer scope");
        check(
                outer.getInventory().countItem(Items.DIAMOND) == 1,
                "Outer breaker must be restored after nested break");
        helper.succeed();
    }

    public static void preferencesSurviveServerLifecycle(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        boolean previous = TelekinesisPlayerSettings.isEnabled(player);
        LifecycleCallbacks callbacks = new LifecycleCallbacks();
        TelekinesisPlayerSettings.initialize(callbacks);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            check(TelekinesisPlayerSettings.setEnabled(player, false), "Preference must be saved");
            callbacks.stopping.accept(server);
            check(
                    TelekinesisPlayerSettings.isEnabled(player),
                    "Stopping must clear the old world's preferences");
            callbacks.started.accept(server);
            check(
                    !TelekinesisPlayerSettings.isEnabled(player),
                    "Restart must reload the disabled preference from disk");
        } finally {
            callbacks.started.accept(server);
            TelekinesisPlayerSettings.setEnabled(player, previous);
        }
        helper.succeed();
    }

    public static void unreadablePreferencesAreNotOverwritten(GameTestHelper helper) {
        LifecycleCallbacks callbacks = new LifecycleCallbacks();
        TelekinesisPlayerSettings.initialize(callbacks);
        MinecraftServer server = helper.getLevel().getServer();
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("telekinesis-players.json");
        ServerPlayer player = player(helper);
        try {
            byte[] original = Files.readAllBytes(file);
            try {
                for (String document :
                        new String[] {"{broken", "{\"schemaVersion\":2,\"players\":{}}"}) {
                    Files.writeString(file, document);
                    callbacks.started.accept(server);
                    check(
                            !TelekinesisPlayerSettings.setEnabled(player, false),
                            "Unreadable state must not be replaced by a preference command");
                    check(
                            !TelekinesisPlayerSettings.isEnabled(player),
                            "The unsaved preference must still apply this session");
                    callbacks.stopping.accept(server);
                    check(
                            Files.readString(file).equals(document),
                            "Shutdown must preserve unreadable or newer-schema state");
                }
            } finally {
                Files.write(file, original);
                callbacks.started.accept(server);
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not exercise preference file recovery", exception);
        }
        helper.succeed();
    }

    public static void preferencesFromPreviousVersionLoad(GameTestHelper helper) {
        LifecycleCallbacks callbacks = new LifecycleCallbacks();
        TelekinesisPlayerSettings.initialize(callbacks);
        MinecraftServer server = helper.getLevel().getServer();
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("telekinesis-players.json");
        ServerPlayer player = player(helper);
        try {
            byte[] original = Files.readAllBytes(file);
            try {
                String previous =
                        "{\n  \"schemaVersion\": 1,\n  \"players\": {\n    \""
                                + player.getUUID()
                                + "\": {\n      \"lastKnownName\": \"Legacy\",\n      \"enabled\": false\n    }\n  }\n}";
                Files.writeString(file, previous);
                callbacks.started.accept(server);
                check(
                        !TelekinesisPlayerSettings.isEnabled(player),
                        "The previous version's preference must load");
                callbacks.stopping.accept(server);
                check(
                        Files.readString(file).equals(previous),
                        "Saving unchanged preferences must keep the file's format");
            } finally {
                Files.write(file, original);
                callbacks.started.accept(server);
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not exercise the preference file", exception);
        }
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    }

    private static ServerPlayer enabledPlayer(GameTestHelper helper) {
        if (!helper.getLevel().getGameRules().get(Telekinesis.ENABLED)) {
            throw new AssertionError("The test needs telekinesis:enabled on");
        }
        ServerPlayer player = player(helper);
        TelekinesisPlayerSettings.setEnabled(player, true);
        player.getInventory().clearContent();
        return player;
    }

    private static void destroy(GameTestHelper helper, ServerPlayer player) {
        check(
                player.gameMode.destroyBlock(helper.absolutePos(DROP_POS)),
                "Expected the test block to break");
    }

    private static void run(GameTestHelper helper, CommandSourceStack source, String command) {
        try {
            check(
                    helper.getLevel()
                                    .getServer()
                                    .getCommands()
                                    .getDispatcher()
                                    .execute(command, source)
                            > 0,
                    "Command failed: " + command);
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("Command failed: " + command, exception);
        }
    }

    private static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static final class LifecycleCallbacks implements TelekinesisPlatform {
        private Consumer<MinecraftServer> started;
        private Consumer<MinecraftServer> stopping;

        @Override
        public void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerServerStarted(Consumer<MinecraftServer> callback) {
            started = callback;
        }

        @Override
        public void registerServerStopping(Consumer<MinecraftServer> callback) {
            stopping = callback;
        }
    }
}
