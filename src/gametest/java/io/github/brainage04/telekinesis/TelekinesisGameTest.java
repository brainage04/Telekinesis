package io.github.brainage04.telekinesis;

import io.github.brainage04.telekinesis.command.TelekinesisCommand;
import io.github.brainage04.telekinesis.gamerule.core.ModGameRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;

public class TelekinesisGameTest {
    private static final BlockPos DROP_POS = new BlockPos(1, 1, 1);

    @GameTest
    public void commandIsRegistered(GameTestHelper context) {
        if (context.getLevel().getServer().getCommands().getDispatcher().getRoot()
                .getChild(TelekinesisCommand.COMMAND_NAME) == null) {
            throw new AssertionError("Expected the telekinesis command to be registered.");
        }

        context.succeed();
    }

    @GameTest
    public void minedBlockDropGoesDirectlyToInventory(GameTestHelper context) {
        ServerPlayer player = makeSurvivalPlayer(context);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        context.setBlock(DROP_POS, Blocks.DIAMOND_ORE);

        assertDestroyed(player, context.absolutePos(DROP_POS));
        assertItemCount(player, Items.DIAMOND, 1);
        context.assertItemEntityNotPresent(Items.DIAMOND);
        context.succeed();
    }

    @GameTest
    public void randomLootResultGoesDirectlyToInventory(GameTestHelper context) {
        ServerPlayer player = makeSurvivalPlayer(context);
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

    @GameTest
    public void multipleUniqueContainerDropsGoDirectlyToInventory(GameTestHelper context) {
        ServerPlayer player = makeSurvivalPlayer(context);
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

    @GameTest
    public void fullInventoryLeavesDropInWorld(GameTestHelper context) {
        ServerPlayer player = makeSurvivalPlayer(context);
        for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        context.setBlock(DROP_POS, Blocks.CHEST);

        assertDestroyed(player, context.absolutePos(DROP_POS));
        assertItemCount(player, Items.CHEST, 0);
        context.assertItemEntityPresent(Items.CHEST);
        context.succeed();
    }

    @GameTest
    public void disabledGameRuleLeavesDropInWorld(GameTestHelper context) {
        GameRules rules = context.getLevel().getGameRules();
        boolean previousValue = rules.get(ModGameRules.ENABLE_TELEKINESIS);
        rules.set(ModGameRules.ENABLE_TELEKINESIS, false, context.getLevel().getServer());

        try {
            ServerPlayer player = makeSurvivalPlayer(context);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            context.setBlock(DROP_POS, Blocks.DIAMOND_ORE);

            assertDestroyed(player, context.absolutePos(DROP_POS));
            assertItemCount(player, Items.DIAMOND, 0);
            context.assertItemEntityPresent(Items.DIAMOND);
        } finally {
            rules.set(ModGameRules.ENABLE_TELEKINESIS, previousValue, context.getLevel().getServer());
        }

        context.succeed();
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
                    "Expected " + expectedCount + " " + item + " in inventory, found " + actualCount + "."
            );
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
