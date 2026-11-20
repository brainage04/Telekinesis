package io.github.brainage04.telekinesis;

import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.check;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.checkCount;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.enabledPlayer;

import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/// The blocks that break a tick after their support goes, through Telekinesis' own policy: a player
/// who breaks the bottom of a sugar cane column gets the whole column, and a column whose support
/// the game removes with no player behind it stays on the ground.
///
/// <p>The player is closed inside the sequence's last step, not when the test method returns: the
/// sequence runs on later ticks, and a player removed before them captures nothing.
public final class TelekinesisSupportBreakGameTests {
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos COLUMN = new BlockPos(5, 1, 5);

    private TelekinesisSupportBreakGameTests() {}

    public static void sugarCaneColumnGoesToTheBreaker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sand = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(sand, Blocks.SAND.defaultBlockState());
        level.setBlockAndUpdate(sand.west(), Blocks.WATER.defaultBlockState());
        for (int height = 1; height <= 3; height++) {
            level.setBlockAndUpdate(sand.above(height), Blocks.SUGAR_CANE.defaultBlockState());
        }

        GameTestPlayer fixture = enabledPlayer(helper, "TkCane", PLAYER_POS);
        ServerPlayer player = fixture.player();
        player.getInventory().clearContent();
        helper.startSequence()
                .thenExecute(
                        () ->
                                check(
                                        player.gameMode.destroyBlock(sand.above()),
                                        "The bottom sugar cane must break"))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        level.getBlockState(sand.above(3)).isAir(),
                                        "The column above must break"))
                .thenExecute(
                        () -> {
                            try {
                                checkCount(
                                        player, Items.SUGAR_CANE, 3, "a broken sugar cane column");
                                check(
                                        groundItems(helper, Items.SUGAR_CANE) == 0,
                                        "No sugar cane may stay on the ground");
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    /// The support removed by the game rather than by a player: no break scope is open, so the
    /// column's drops stay in the world even with Telekinesis on for the player standing by.
    public static void supportBrokenWithoutAPlayerStaysInTheWorld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sand = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(sand, Blocks.SAND.defaultBlockState());
        level.setBlockAndUpdate(sand.west(), Blocks.WATER.defaultBlockState());
        for (int height = 1; height <= 3; height++) {
            level.setBlockAndUpdate(sand.above(height), Blocks.SUGAR_CANE.defaultBlockState());
        }

        GameTestPlayer fixture = enabledPlayer(helper, "TkCaneNoPlayer", PLAYER_POS);
        ServerPlayer player = fixture.player();
        player.getInventory().clearContent();
        helper.startSequence()
                .thenExecute(() -> check(level.destroyBlock(sand, true), "The support must break"))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        level.getBlockState(sand.above(3)).isAir(),
                                        "The column above must break"))
                .thenExecute(
                        () -> {
                            try {
                                checkCount(
                                        player, Items.SUGAR_CANE, 0, "a support break by nobody");
                                check(
                                        groundItems(helper, Items.SUGAR_CANE) == 3,
                                        "The whole column must drop in the world");
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    /// Telekinesis switched off for the player leaves the whole column in the world, as it leaves
    /// every other drop.
    public static void disabledPlayerKeepsTheColumnInTheWorld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sand = helper.absolutePos(COLUMN);
        level.setBlockAndUpdate(sand, Blocks.SAND.defaultBlockState());
        level.setBlockAndUpdate(sand.west(), Blocks.WATER.defaultBlockState());
        for (int height = 1; height <= 3; height++) {
            level.setBlockAndUpdate(sand.above(height), Blocks.SUGAR_CANE.defaultBlockState());
        }

        GameTestPlayer fixture = enabledPlayer(helper, "TkCaneOff", PLAYER_POS);
        ServerPlayer player = fixture.player();
        player.getInventory().clearContent();
        TelekinesisPlayerSettings.setEnabled(player, false);
        helper.startSequence()
                .thenExecute(
                        () ->
                                check(
                                        player.gameMode.destroyBlock(sand.above()),
                                        "The bottom sugar cane must break"))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        level.getBlockState(sand.above(3)).isAir(),
                                        "The column above must break"))
                .thenExecute(
                        () -> {
                            try {
                                checkCount(
                                        player,
                                        Items.SUGAR_CANE,
                                        0,
                                        "a player with Telekinesis off");
                                check(
                                        groundItems(helper, Items.SUGAR_CANE) == 3,
                                        "The whole column must drop in the world");
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    /// Items inside the test's own bounds only: the server lays tests out 8 to a row, five blocks
    /// apart (`GameTestServer.java:325`, `StructureGridSpawner.java:17-18`), so a wider box could
    /// count a neighbour's drops.
    private static int groundItems(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).stream()
                .filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }
}
