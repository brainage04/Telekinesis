package io.github.brainage04.telekinesis;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.drop.BlockDropCapture;
import io.github.brainage04.telekinesis.platform.TelekinesisPlatform;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Behaviour exercised through the production implementation on both loaders. */
public final class TelekinesisBehaviorGameTests {
    private static final BlockPos DROP_POS = new BlockPos(1, 1, 1);

    private TelekinesisBehaviorGameTests() {
    }

    public static void personalCommandsAndOperatorPermissions(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        boolean previousGlobal = TelekinesisConfigManager.config().enabled();
        boolean previousPersonal = TelekinesisPlayerSettings.isEnabled(player);
        CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput();
        try {
            TelekinesisConfigManager.setEnabled(true);
            run(helper, source, "telekinesis disable");
            check(!TelekinesisPlayerSettings.isEffective(player), "Disable must affect the player's drops");
            run(helper, source, "telekinesis toggle");
            check(TelekinesisPlayerSettings.isEffective(player), "Toggle must restore the personal setting");
            run(helper, source, "telekinesis enable");
            run(helper, source, "telekinesis status");
            run(helper, source, "telekinesis help");
            try {
                helper.getLevel().getServer().getCommands().getDispatcher().execute("telekinesis config disable", source);
                throw new AssertionError("A non-operator must not change the server setting");
            } catch (CommandSyntaxException expected) {
                check(TelekinesisConfigManager.config().enabled(), "Rejected config command must not mutate state");
            }
        } finally {
            TelekinesisPlayerSettings.setEnabled(player, previousPersonal);
            TelekinesisConfigManager.setEnabled(previousGlobal);
        }
        helper.succeed();
    }

    public static void operatorCommandsSaveAndReload(GameTestHelper helper) {
        boolean previous = TelekinesisConfigManager.config().enabled();
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
        try {
            run(helper, source, "telekinesis config disable");
            check(!TelekinesisConfigManager.config().enabled(), "Operator disable must change the global setting");
            run(helper, source, "telekinesis config reload");
            check(!TelekinesisConfigManager.config().enabled(), "Reload must preserve the saved disabled setting");
            run(helper, source, "telekinesis config toggle");
            check(TelekinesisConfigManager.config().enabled(), "Operator toggle must enable the mod");
            run(helper, source, "telekinesis config enable");
            run(helper, source, "telekinesis config status");
            try {
                helper.getLevel().getServer().getCommands().getDispatcher().execute("telekinesis disable", source);
                throw new AssertionError("The console must not have a personal preference");
            } catch (CommandSyntaxException expected) {
                check(TelekinesisConfigManager.config().enabled(), "Console personal command must not disable the server");
            }
        } finally {
            TelekinesisConfigManager.setEnabled(previous);
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
        helper.getBlockEntity(DROP_POS, ChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 6));
        destroy(helper, player);
        check(player.getInventory().getItem(1).getCount() == 64, "Available stack space must receive exactly two diamonds");
        int remaining = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(feet, feet).inflate(1))) {
            if (entity.getItem().is(Items.DIAMOND)) {
                remaining += entity.getItem().getCount();
                check(entity.position().distanceToSqr(feet) < 0.01, "Overflow must be at the player's feet");
                check(entity.getDeltaMovement().equals(Vec3.ZERO), "Overflow must not fly away");
            }
        }
        check(remaining == 4, "Exactly four overflow diamonds must remain in the world");
        helper.succeed();
    }

    public static void silkTouchPreservesLootAndSuppressesExperience(GameTestHelper helper) {
        ServerPlayer player = enabledPlayer(helper);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        helper.setBlock(DROP_POS, Blocks.DIAMOND_ORE);
        int before = player.totalExperience;
        destroy(helper, player);
        check(player.getInventory().countItem(Items.DIAMOND_ORE) == 1, "Silk Touch must collect the ore, not diamonds");
        check(player.getInventory().countItem(Items.DIAMOND) == 0, "Silk Touch must not add normal loot too");
        check(player.totalExperience == before, "Silk Touch must not award ore experience");
        helper.succeed();
    }

    public static void experienceRepairsMendingBeforeAwardingPoints(GameTestHelper helper) {
        ServerPlayer player = enabledPlayer(helper);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
        pickaxe.setDamageValue(100);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        helper.setBlock(DROP_POS, Blocks.DIAMOND_ORE);
        int before = player.totalExperience;
        destroy(helper, player);
        check(pickaxe.getDamageValue() < 100, "Ore experience must repair the equipped Mending tool");
        check(player.totalExperience == before, "Experience consumed by Mending must not be awarded twice");
        helper.succeed();
    }

    public static void unrelatedSpawnsRemainInWorldAfterBreak(GameTestHelper helper) {
        ServerPlayer player = enabledPlayer(helper);
        helper.setBlock(DROP_POS, Blocks.CHEST);
        destroy(helper, player);
        Vec3 position = Vec3.atCenterOf(helper.absolutePos(DROP_POS));
        ItemEntity item = new ItemEntity(helper.getLevel(), position.x, position.y, position.z, new ItemStack(Items.EMERALD));
        check(helper.getLevel().addFreshEntity(item), "Unrelated items must not be captured after a block break");
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), position, Vec3.ZERO, 5);
        check(helper.getLevel().addFreshEntity(orb), "Unrelated experience must not be captured after a block break");
        check(player.getInventory().countItem(Items.EMERALD) == 0, "Unrelated emerald must not enter the player's inventory");
        helper.succeed();
    }

    public static void nestedBreaksRestoreOuterPlayer(GameTestHelper helper) {
        ServerPlayer outer = enabledPlayer(helper);
        ServerPlayer inner = enabledPlayer(helper);
        Vec3 position = Vec3.atCenterOf(helper.absolutePos(DROP_POS));
        BlockDropCapture.begin(outer);
        try {
            BlockDropCapture.begin(inner);
            try {
                helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), position.x, position.y, position.z, new ItemStack(Items.EMERALD)));
            } finally {
                BlockDropCapture.end();
            }
            helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), position.x, position.y, position.z, new ItemStack(Items.DIAMOND)));
        } finally {
            BlockDropCapture.end();
        }
        check(inner.getInventory().countItem(Items.EMERALD) == 1, "Nested drop must belong to the inner breaker");
        check(inner.getInventory().countItem(Items.DIAMOND) == 0, "Nested breaker must not retain the outer scope");
        check(outer.getInventory().countItem(Items.DIAMOND) == 1, "Outer breaker must be restored after nested break");
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
            check(TelekinesisPlayerSettings.isEnabled(player), "Stopping must clear the old world's preferences");
            callbacks.started.accept(server);
            check(!TelekinesisPlayerSettings.isEnabled(player), "Restart must reload the disabled preference from disk");
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
                for (String document : new String[]{"{broken", "{\"schemaVersion\":2,\"players\":{}}"}) {
                    Files.writeString(file, document);
                    callbacks.started.accept(server);
                    check(!TelekinesisPlayerSettings.setEnabled(player, false), "Unreadable state must not be replaced by a preference command");
                    check(!TelekinesisPlayerSettings.isEnabled(player), "The unsaved preference must still apply this session");
                    callbacks.stopping.accept(server);
                    check(Files.readString(file).equals(document), "Shutdown must preserve unreadable or newer-schema state");
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

    private static ServerPlayer player(GameTestHelper helper) {
        return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    }

    private static ServerPlayer enabledPlayer(GameTestHelper helper) {
        TelekinesisConfigManager.setEnabled(true);
        ServerPlayer player = player(helper);
        TelekinesisPlayerSettings.setEnabled(player, true);
        player.getInventory().clearContent();
        return player;
    }

    private static void destroy(GameTestHelper helper, ServerPlayer player) {
        check(player.gameMode.destroyBlock(helper.absolutePos(DROP_POS)), "Expected the test block to break");
    }

    private static void run(GameTestHelper helper, CommandSourceStack source, String command) {
        try {
            check(helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source) > 0, "Command failed: " + command);
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

        @Override public Path configDirectory() { throw new UnsupportedOperationException(); }
        @Override public void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback) { throw new UnsupportedOperationException(); }
        @Override public void registerServerStarted(Consumer<MinecraftServer> callback) { started = callback; }
        @Override public void registerServerStopping(Consumer<MinecraftServer> callback) { stopping = callback; }
    }
}
