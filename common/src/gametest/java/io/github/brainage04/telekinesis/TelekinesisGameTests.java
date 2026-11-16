package io.github.brainage04.telekinesis;

import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TelekinesisGameTests {
    private static final BlockPos DROP_POS = new BlockPos(1, 1, 1);

    private TelekinesisGameTests() {}

    public static void minedBlockDropGoesDirectlyToInventory(GameTestHelper context) {
        ServerPlayer player = makeEnabledSurvivalPlayer(context);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        context.setBlock(DROP_POS, Blocks.DIAMOND_ORE);

        assertDestroyed(player, context.absolutePos(DROP_POS));
        assertItemCount(player, Items.DIAMOND, 1);
        context.assertItemEntityNotPresent(Items.DIAMOND);
        context.succeed();
    }

    public static void blockExperienceGoesDirectlyToPlayer(GameTestHelper context) {
        ServerPlayer player = makeEnabledSurvivalPlayer(context);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        context.setBlock(DROP_POS, Blocks.DIAMOND_ORE);
        int experienceBefore = player.totalExperience;

        assertDestroyed(player, context.absolutePos(DROP_POS));
        if (player.totalExperience <= experienceBefore) {
            throw new AssertionError(
                    "Expected diamond ore experience to be awarded directly to the player.");
        }
        context.assertEntityNotPresent(EntityTypes.EXPERIENCE_ORB);
        context.succeed();
    }

    public static void randomLootResultGoesDirectlyToInventory(GameTestHelper context) {
        ServerPlayer player = makeEnabledSurvivalPlayer(context);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SHOVEL));
        context.setBlock(DROP_POS, Blocks.GRAVEL);

        assertDestroyed(player, context.absolutePos(DROP_POS));
        int resultCount = countItem(player, Items.GRAVEL) + countItem(player, Items.FLINT);
        if (resultCount != 1) {
            throw new AssertionError("Expected one gravel loot result, found " + resultCount + ".");
        }
        context.assertItemEntityNotPresent(Items.GRAVEL);
        context.assertItemEntityNotPresent(Items.FLINT);
        context.succeed();
    }

    public static void multipleUniqueContainerDropsGoDirectlyToInventory(GameTestHelper context) {
        ServerPlayer player = makeEnabledSurvivalPlayer(context);
        context.setBlock(DROP_POS, Blocks.CHEST);
        ChestBlockEntity chest = context.getBlockEntity(DROP_POS, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        chest.setItem(1, new ItemStack(Items.EMERALD, 2));

        assertDestroyed(player, context.absolutePos(DROP_POS));
        assertItemCount(player, Items.CHEST, 1);
        assertItemCount(player, Items.DIAMOND, 3);
        assertItemCount(player, Items.EMERALD, 2);
        context.assertEntityNotPresent(EntityTypes.ITEM);
        context.succeed();
    }

    public static void fullInventoryDropsRemainderAtPlayerFeet(GameTestHelper context) {
        ServerPlayer player = makeEnabledSurvivalPlayer(context);
        for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        Vec3 playerPosition = Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(4, 1, 1)));
        player.setPos(playerPosition.x(), playerPosition.y(), playerPosition.z());
        context.setBlock(DROP_POS, Blocks.CHEST);

        assertDestroyed(player, context.absolutePos(DROP_POS));
        assertItemCount(player, Items.CHEST, 0);
        ItemEntity remainder =
                context
                        .getLevel()
                        .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(playerPosition, playerPosition).inflate(1.0D))
                        .stream()
                        .filter(itemEntity -> itemEntity.getItem().is(Items.CHEST))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "Expected the chest remainder at the player's feet."));
        if (remainder.position().distanceToSqr(player.position()) > 0.01D) {
            throw new AssertionError(
                    "Expected overflow at the player's exact position, found "
                            + remainder.position()
                            + ".");
        }
        context.succeed();
    }

    /** The rule exists, and it is on without a server owner having to do anything. */
    public static void enabledRuleDefaultsOn(GameTestHelper context) {
        GameRule<?> rule = enabledRule(context);
        if (!Boolean.TRUE.equals(rule.defaultValue())) {
            throw new AssertionError("telekinesis:enabled defaults to " + rule.defaultValue());
        }
        if (!context.getLevel().getGameRules().get(Telekinesis.ENABLED)) {
            throw new AssertionError("telekinesis:enabled is not on in a new world");
        }
        context.succeed();
    }

    /**
     * The server-wide switch is the gamerule: turned off through the command a server owner uses,
     * it stops the drops of a player whose own preference is on, and turning it back on restores
     * them on the next break with no restart.
     */
    public static void serverGameruleOffLeavesDropInWorld(GameTestHelper context) {
        withEnabled(
                context,
                false,
                () -> {
                    ServerPlayer player = makeSurvivalPlayer(context);
                    TelekinesisPlayerSettings.setEnabled(player, true);
                    player.setItemInHand(
                            InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
                    context.setBlock(DROP_POS, Blocks.DIAMOND_ORE);

                    assertDestroyed(player, context.absolutePos(DROP_POS));
                    assertItemCount(player, Items.DIAMOND, 0);
                    assertItemEntityNearDrop(context, Items.DIAMOND);
                    clearDrops(context);
                });
        withEnabled(
                context,
                true,
                () -> {
                    ServerPlayer player = makeSurvivalPlayer(context);
                    TelekinesisPlayerSettings.setEnabled(player, true);
                    player.setItemInHand(
                            InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
                    context.setBlock(DROP_POS, Blocks.DIAMOND_ORE);

                    assertDestroyed(player, context.absolutePos(DROP_POS));
                    assertItemCount(player, Items.DIAMOND, 1);
                    context.assertItemEntityNotPresent(Items.DIAMOND);
                });
        context.succeed();
    }

    /** A player's own preference off keeps the drop in the world while the rule is on. */
    public static void disabledPlayerPreferenceLeavesDropInWorld(GameTestHelper context) {
        withEnabled(
                context,
                true,
                () -> {
                    ServerPlayer player = makeEnabledSurvivalPlayer(context);
                    TelekinesisPlayerSettings.setEnabled(player, false);
                    player.setItemInHand(
                            InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
                    context.setBlock(DROP_POS, Blocks.DIAMOND_ORE);

                    try {
                        assertDestroyed(player, context.absolutePos(DROP_POS));
                        assertItemCount(player, Items.DIAMOND, 0);
                        assertItemEntityNearDrop(context, Items.DIAMOND);
                    } finally {
                        TelekinesisPlayerSettings.setEnabled(player, true);
                    }
                });
        context.succeed();
    }

    /** Discards the items a previous half of a test left on the ground. */
    private static void clearDrops(GameTestHelper context) {
        BlockPos at = context.absolutePos(DROP_POS);
        context.getLevel()
                .getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(4.0D))
                .forEach(ItemEntity::discard);
    }

    /** The enabled rule, looked up by name the way the gamerules screen finds it. */
    private static GameRule<?> enabledRule(GameTestHelper context) {
        GameRule<?> rule =
                BuiltInRegistries.GAME_RULE.getValue(
                        Identifier.fromNamespaceAndPath(Telekinesis.MOD_ID, "enabled"));
        if (rule == null) {
            throw new AssertionError("telekinesis:enabled gamerule is not registered");
        }
        return rule;
    }

    /**
     * Runs {@code body} with {@code telekinesis:enabled} set to {@code on} through the command a
     * server owner uses, and puts the rule back afterwards so no other test sees it changed.
     */
    static void withEnabled(GameTestHelper context, boolean on, Runnable body) {
        boolean before = context.getLevel().getGameRules().get(Telekinesis.ENABLED);
        setEnabled(context, on);
        try {
            body.run();
        } finally {
            setEnabled(context, before);
        }
    }

    /** Sets {@code telekinesis:enabled} through the command a server owner uses. */
    static void setEnabled(GameTestHelper context, boolean on) {
        MinecraftServer server = context.getLevel().getServer();
        if (server.getGameRules().get(Telekinesis.ENABLED) == on) {
            return;
        }
        server.getCommands()
                .performPrefixedCommand(
                        server.createCommandSourceStack(), "gamerule telekinesis:enabled " + on);
        if (server.getGameRules().get(Telekinesis.ENABLED) != on) {
            throw new AssertionError(
                    "/gamerule telekinesis:enabled " + on + " did not set the rule");
        }
    }

    private static ServerPlayer makeEnabledSurvivalPlayer(GameTestHelper context) {
        ServerPlayer player = makeSurvivalPlayer(context);
        TelekinesisPlayerSettings.setEnabled(player, true);
        return player;
    }

    private static ServerPlayer makeSurvivalPlayer(GameTestHelper context) {
        return (ServerPlayer) context.makeMockServerPlayer(GameType.SURVIVAL);
    }

    private static void assertDestroyed(ServerPlayer player, BlockPos absolutePos) {
        if (!player.gameMode.destroyBlock(absolutePos)) {
            throw new AssertionError("Expected the player to destroy the test block.");
        }
    }

    private static void assertItemCount(ServerPlayer player, Item item, int expectedCount) {
        int actualCount = countItem(player, item);
        if (actualCount != expectedCount) {
            throw new AssertionError(
                    "Expected "
                            + expectedCount
                            + " "
                            + item
                            + " in inventory, found "
                            + actualCount
                            + ".");
        }
    }

    private static void assertItemEntityNearDrop(GameTestHelper context, Item item) {
        BlockPos dropPosition = context.absolutePos(DROP_POS);
        boolean present =
                context
                        .getLevel()
                        .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(dropPosition).inflate(4.0D),
                                itemEntity -> itemEntity.getItem().is(item))
                        .stream()
                        .findAny()
                        .isPresent();
        if (!present) {
            throw new AssertionError(
                    "Expected " + item + " to remain in the world near the mined block.");
        }
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
