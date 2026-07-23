package io.github.brainage04.telekinesis;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public final class TelekinesisClientGameTest implements FabricClientGameTest {
    private static final int STAGE_Y = 63;
    private static final BlockPos ORE_POS = new BlockPos(0, STAGE_Y + 1, 0);
    private static final BlockPos CHEST_POS = new BlockPos(2, STAGE_Y + 1, 0);
    private static final BlockPos DISABLED_ORE_POS = new BlockPos(-2, STAGE_Y + 1, 0);
    private static final AABB STAGE_BOX = new AABB(-6, STAGE_Y, -4, 6, STAGE_Y + 4, 6);

    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();

        try (TestDedicatedServerContext server = context.worldBuilder().createServer(serverProperties)) {
            ClientGameTestServers.connectToDedicatedServer(context, server, "Telekinesis recording GameTest");
            try {
                boolean previousTelekinesis = server.computeOnServer(minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    TelekinesisConfigManager.setEnabled(true);
                    TelekinesisPlayerSettings.setEnabled(player, true);
                    prepareStage(player.level(), player);
                    return TelekinesisConfigManager.config().enabled();
                });

                context.runOnClient(client -> {
                    if (client.player == null) {
                        throw new AssertionError("Expected a connected client player for the recording.");
                    }
                    client.player.setYRot(180.0F);
                    client.player.setXRot(12.0F);
                });

                try {
                    context.waitTicks(30);
                    ClientGameTestRecorder.startRecording(context);
                    ClientGameTestRecorder.showStep(
                            context,
                            "telekinesis.stage",
                            "Telekinesis",
                            "Survival player on the elevated ore stage"
                    );
                    context.waitTicks(30);

                    ClientGameTestRecorder.showStep(
                            context,
                            "telekinesis.ore",
                            "Diamond ore",
                            "Breaking ore sends its diamond directly to inventory"
                    );
                    server.computeOnServer(minecraftServer -> {
                        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                        ServerLevel level = player.level();
                        destroy(player, ORE_POS);
                        assertInventoryCount(player, Items.DIAMOND, 1);
                        assertNoItemEntity(level, Items.DIAMOND);
                        return null;
                    });
                    context.waitTicks(35);

                    ClientGameTestRecorder.showStep(
                            context,
                            "telekinesis.chest",
                            "Chest contents",
                            "The chest and each unique contents stack go directly to inventory"
                    );
                    server.computeOnServer(minecraftServer -> {
                        ServerLevel level = minecraftServer.getPlayerList().getPlayers().getFirst().level();
                        level.setBlock(CHEST_POS, Blocks.CHEST.defaultBlockState(), 3);
                        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(CHEST_POS);
                        if (chest == null) {
                            throw new AssertionError("Expected the demonstration chest block entity.");
                        }
                        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
                        chest.setItem(1, new ItemStack(Items.EMERALD, 2));
                        return null;
                    });
                    context.waitTicks(25);
                    ClientGameTestRecorder.showStep(
                            context,
                            "telekinesis.chest.break",
                            "Chest contents",
                            "Breaking the chest transfers every drop directly to inventory"
                    );
                    server.computeOnServer(minecraftServer -> {
                        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                        ServerLevel level = player.level();
                        destroy(player, CHEST_POS);
                        assertInventoryCount(player, Items.CHEST, 1);
                        assertInventoryCount(player, Items.DIAMOND, 4);
                        assertInventoryCount(player, Items.EMERALD, 2);
                        assertNoItemEntities(level);
                        return null;
                    });
                    context.waitTicks(35);

                    ClientGameTestRecorder.showStep(
                            context,
                            "telekinesis.disabled",
                            "Telekinesis disabled",
                            "With the game rule off, the diamond remains in the world"
                    );
                    server.computeOnServer(minecraftServer -> {
                        ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                        ServerLevel level = player.level();
                        TelekinesisConfigManager.setEnabled(false);
                        destroy(player, DISABLED_ORE_POS);
                        assertInventoryCount(player, Items.DIAMOND, 4);
                        assertItemEntityPresent(level, Items.DIAMOND);
                        return null;
                    });
                    context.waitTicks(40);
                } finally {
                    server.computeOnServer(minecraftServer -> {
                        TelekinesisConfigManager.setEnabled(previousTelekinesis);
                        return null;
                    });
                }
            } finally {
                ClientGameTestServers.disconnectFromDedicatedServer(context);
            }
        }
    }


    private static void prepareStage(ServerLevel level, ServerPlayer player) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -3; z <= 5; z++) {
                level.setBlock(new BlockPos(x, STAGE_Y, z), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, STAGE_Y + 1, z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, STAGE_Y + 2, z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.setBlock(ORE_POS, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
        level.setBlock(DISABLED_ORE_POS, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
        player.getInventory().clearContent();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        player.setYRot(180.0F);
        player.setXRot(12.0F);
        player.teleportTo(0.5D, STAGE_Y + 1D, 4.5D);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void destroy(ServerPlayer player, BlockPos position) {
        if (!player.gameMode.destroyBlock(position)) {
            throw new AssertionError("Expected the survival player to destroy " + position + ".");
        }
    }

    private static void assertInventoryCount(ServerPlayer player, Item item, int expected) {
        int actual = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                actual += stack.getCount();
            }
        }
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + " " + item + " in inventory, found " + actual + ".");
        }
    }

    private static void assertNoItemEntity(ServerLevel level, Item item) {
        if (level.getEntitiesOfClass(ItemEntity.class, STAGE_BOX).stream()
                .anyMatch(itemEntity -> itemEntity.getItem().is(item))) {
            throw new AssertionError("Expected no " + item + " item entity on the stage.");
        }
    }

    private static void assertNoItemEntities(ServerLevel level) {
        if (!level.getEntitiesOfClass(ItemEntity.class, STAGE_BOX).isEmpty()) {
            throw new AssertionError("Expected every chest drop to enter inventory without an item entity.");
        }
    }

    private static void assertItemEntityPresent(ServerLevel level, Item item) {
        if (level.getEntitiesOfClass(ItemEntity.class, STAGE_BOX).stream()
                .noneMatch(itemEntity -> itemEntity.getItem().is(item))) {
            throw new AssertionError("Expected a " + item + " item entity to remain on the stage.");
        }
    }
}
