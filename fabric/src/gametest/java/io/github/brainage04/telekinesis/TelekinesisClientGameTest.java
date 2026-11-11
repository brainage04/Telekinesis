package io.github.brainage04.telekinesis;

import io.github.brainage04.minecraftmodconventions.ClientGameTestPlayerActions;
import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The nine captioned stages of Telekinesis' showcase.
 *
 * <p>Every stage plays like a person: the player walks with the movement keys, turns to the block or
 * entity it acts on, and then mines or uses it with the attack or use key, within vanilla reach. {@link
 * ClientGameTestPlayerActions} keeps it that way and fails the stage when the crosshair is not on the
 * target or the target is out of reach.
 *
 * <p>Server-side calls only build the scene, spawn the stage's mob, set game rules and place the player
 * between stages, before that stage is filmed.
 */
@SuppressWarnings("UnstableApiUsage")
public final class TelekinesisClientGameTest implements FabricClientGameTest {
    private static final int FLOOR_Y = 63;
    private static final int STAND_Y = FLOOR_Y + 1;
    private static final int STAGE_MIN_X = -110;
    private static final int STAGE_MAX_X = 86;
    private static final int STAGE_MIN_Z = -16;
    private static final int STAGE_MAX_Z = 8;

    private static final int MINING_X = -96;
    private static final int CHEST_X = -72;
    private static final int TREE_X = -48;
    private static final int MOB_X = -24;
    private static final int TNT_X = 0;
    private static final int ANCHOR_X = 24;
    private static final int FULL_X = 48;
    private static final int OFF_X = 72;

    private static final double NEAR_Z = 4.5D;
    /** Where the player starts the tree stage, before walking up to the oak. */
    private static final double TREE_START_Z = 6.5D;
    /** Where the player starts the TNT stage, before walking up to the TNT. */
    private static final double TNT_START_Z = 0.5D;
    /** How close the player walks to the TNT before lighting it: inside reach, back from the blast. */
    private static final double TNT_LIGHT_DISTANCE = 3.0D;
    /** How far the player steps back from the lit TNT, outside a power-4 blast's damage. */
    private static final double TNT_SAFE_DISTANCE = 7.0D;
    /** Where the player stands to use the anchor and the Nether bed: the far edge of vanilla reach. */
    private static final double BLAST_STAND_Z = -1.0D;
    /** The tick a lit TNT is given, so the player can step back before it blows. */
    private static final int TNT_FUSE_TICKS = 60;

    private static final int NETHER_FLOOR_Y = 200;
    private static final BlockPos NETHER_BED_FOOT = new BlockPos(0, NETHER_FLOOR_Y + 1, -6);
    /** The longest oak the tree stage can fell: its top log has to be inside vanilla reach. */
    private static final int MAX_TREE_LOGS = 7;

    private static final float NORTH = 180.0F;
    private static final int RENDER_DISTANCE = 12;
    private static final int NORMAL_FOV = 70;
    private static final int BLAST_FOV = 45;
    /** Ticks the blast stages' resistance lasts, which a real player's armour would have bought. */
    private static final int BLAST_RESISTANCE_TICKS = 20 * 60;
    private static final int BLAST_RESISTANCE_AMPLIFIER = 3;

    private static final int LEAF_DECAY_TICK_SPEED = 300;
    private static final int SETTLE_TICKS = 15;
    private static final int CAPTION_LEAD_TICKS = 6;
    private static final int HOLD_AFTER_END_TICKS = 6;
    private static final int STEP_BACK_TICKS = 7;

    private static final int LOAD_TIMEOUT_TICKS = 2400;
    private static final int ACTION_TIMEOUT_TICKS = 200;
    private static final long BEFORE_MILLIS = 700L;
    private static final long AFTER_MILLIS = 1500L;

    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();
        serverProperties.setProperty("view-distance", Integer.toString(RENDER_DISTANCE));

        ClientGameTestServers.withDedicatedServer(
                context,
                serverProperties,
                "Telekinesis recording GameTest",
                logged(server -> server.runOnServer(TelekinesisClientGameTest::buildWorld)),
                logged(
                        server -> {
                            boolean previousTelekinesis =
                                    server.computeOnServer(
                                            minecraftServer -> {
                                                boolean enabled =
                                                        TelekinesisConfigManager.config().enabled();
                                                TelekinesisConfigManager.setEnabled(true);
                                                TelekinesisPlayerSettings.setEnabled(
                                                        player(minecraftServer), true);
                                                return enabled;
                                            });
                            ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
                            context.runOnClient(
                                    client -> {
                                        client.options.renderDistance().set(RENDER_DISTANCE);
                                        // The server only sends chunks out to the render distance
                                        // the client last told it.
                                        client.options.broadcastOptions();
                                        client.options.particles().set(ParticleStatus.DECREASED);
                                        // A section that has just been built would fade in over a
                                        // second; the film never shows that.
                                        client.options.chunkSectionFadeInTime().set(0.0D);
                                        // Without occlusion culling every section in range is
                                        // drawn, so none blinks out when the player moves.
                                        client.smartCull = false;
                                        // A new item in the hand shows its name for a while;
                                        // the shortest setting has it gone before each stage.
                                        client.options.notificationDisplayTime().set(0.5D);
                                        client.options.setCameraType(CameraType.FIRST_PERSON);
                                    });
                            try {
                                ClientGameTestRecorder.startRecording(context);
                                mining(context, server);
                                chest(context, server);
                                tree(context, server);
                                mobKill(context, server);
                                tnt(context, server);
                                respawnAnchor(context, server);
                                fullInventory(context, server);
                                turnedOff(context, server);
                                netherBed(context, server);
                            } finally {
                                server.runOnServer(
                                        minecraftServer ->
                                                TelekinesisConfigManager.setEnabled(
                                                        previousTelekinesis));
                                context.runOnClient(
                                        client -> {
                                            client.options.setCameraType(CameraType.FIRST_PERSON);
                                            client.options.fov().set(NORMAL_FOV);
                                        });
                            }
                        }));
    }

    /**
     * Prints a failure before it leaves {@code step}: the harness stops the recording in a {@code
     * finally}, and when that fails too, its exception replaces this one.
     */
    private static Consumer<TestDedicatedServerContext> logged(
            Consumer<TestDedicatedServerContext> step) {
        return server -> {
            try {
                step.accept(server);
            } catch (RuntimeException | Error failure) {
                Telekinesis.LOGGER.error("Telekinesis recording step failed", failure);
                throw failure;
            }
        };
    }

    private static void mining(ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos ore = new BlockPos(MINING_X, STAND_Y, 2);
        prepare(
                context,
                server,
                MINING_X,
                NEAR_Z,
                NORMAL_FOV,
                player -> {
                    player.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
                    player.getInventory()
                            .setItem(0, pickaxe(player.level(), Enchantments.FORTUNE, 3));
                });
        String id = "telekinesis.mining";
        caption(
                context,
                id,
                "Mining a block",
                "The ore's diamonds (Fortune III) and its experience go straight to you");
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.lookAtBlock(context, ore);
        ClientGameTestPlayerActions.mine(context, ore);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkAtLeast(player, Items.DIAMOND, 1);
                    check(player.totalExperience > 0, "The ore's experience must go to the miner");
                    checkNoItemsAt(player.level(), MINING_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void chest(ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos chest = new BlockPos(CHEST_X, STAND_Y, 2);
        List<ItemStack> contents =
                List.of(
                        new ItemStack(Items.DIAMOND, 3),
                        new ItemStack(Items.EMERALD, 12),
                        new ItemStack(Items.IRON_INGOT, 16),
                        new ItemStack(Items.GOLD_INGOT, 5),
                        new ItemStack(Items.GOLDEN_APPLE, 1));
        prepare(
                context,
                server,
                CHEST_X,
                NEAR_Z,
                NORMAL_FOV,
                player -> {
                    ServerLevel level = player.level();
                    level.setBlockAndUpdate(
                            chest,
                            Blocks.CHEST
                                    .defaultBlockState()
                                    .setValue(ChestBlock.FACING, Direction.SOUTH));
                    ChestBlockEntity entity = (ChestBlockEntity) level.getBlockEntity(chest);
                    check(entity != null, "The chest stage needs its chest");
                    for (int slot = 0; slot < contents.size(); slot++) {
                        entity.setItem(slot * 3, contents.get(slot).copy());
                    }
                    player.getInventory().setItem(0, axe(level));
                });
        String id = "telekinesis.chest";
        caption(
                context,
                id,
                "Breaking a chest",
                "The chest and every stack inside it go straight to your inventory");
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.lookAtBlock(context, chest);
        ClientGameTestPlayerActions.mine(context, chest);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.CHEST, 1);
                    for (ItemStack stack : contents) {
                        checkCount(player, stack.getItem(), stack.getCount());
                    }
                    checkNoItemsAt(player.level(), CHEST_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void tree(ClientGameTestContext context, TestDedicatedServerContext server) {
        List<BlockPos> logs =
                server.computeOnServer(
                        minecraftServer -> {
                            ServerLevel level = minecraftServer.overworld();
                            List<BlockPos> found = new ArrayList<>();
                            BlockPos.MutableBlockPos cursor =
                                    new BlockPos.MutableBlockPos(TREE_X, STAND_Y, 0);
                            while (level.getBlockState(cursor).is(Blocks.OAK_LOG)) {
                                found.add(cursor.immutable());
                                cursor.move(Direction.UP);
                            }
                            return List.copyOf(found);
                        });
        check(logs.size() >= 4, "The tree stage needs an oak of at least four logs, got " + logs);
        check(
                logs.size() <= MAX_TREE_LOGS,
                "The tree stage's oak must be at most "
                        + MAX_TREE_LOGS
                        + " logs tall so its top log is inside vanilla reach, got "
                        + logs.size()
                        + ": "
                        + logs);
        prepare(
                context,
                server,
                TREE_X,
                TREE_START_Z,
                NORMAL_FOV,
                player -> player.getInventory().setItem(0, axe(player.level())));
        Vec3 trunkFoot = new Vec3(TREE_X + 0.5D, STAND_Y, 0.5D);
        String id = "telekinesis.tree";
        caption(
                context,
                id,
                "Felling a tree",
                "Logs, then the drops of the decaying leaves, go to the feller");
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.walkWithinReach(context, trunkFoot);
        ClientGameTestPlayerActions.lookAtBlock(context, logs.get(0));
        ClientGameTestPlayerActions.mine(context, logs.get(0));
        ClientGameTestPlayerActions.lookAtBlock(context, logs.get(1));
        ClientGameTestPlayerActions.mine(context, logs.get(1));
        // Step into the two-log gap under the rest of the trunk and look straight up, the way a
        // player fells a tree.
        ClientGameTestPlayerActions.walkTo(context, trunkFoot, 0.3D);
        ClientGameTestPlayerActions.lookStraightUp(context);
        for (BlockPos log : logs.subList(2, logs.size())) {
            context.waitTicks(2);
            ClientGameTestPlayerActions.mine(context, log);
        }
        server.runOnServer(
                minecraftServer ->
                        command(
                                minecraftServer,
                                "gamerule random_tick_speed " + LEAF_DECAY_TICK_SPEED));
        AABB canopy = new AABB(TREE_X - 4, STAND_Y, -4, TREE_X + 5, STAND_Y + 10, 5);
        try {
            context.waitFor(
                    client ->
                            client.level
                                    .getBlockStates(canopy)
                                    .noneMatch(state -> state.is(Blocks.OAK_LEAVES)),
                    ACTION_TIMEOUT_TICKS);
        } finally {
            server.runOnServer(
                    minecraftServer -> command(minecraftServer, "gamerule random_tick_speed 3"));
        }
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.OAK_LOG, logs.size());
                    check(
                            count(player, Items.OAK_SAPLING)
                                            + count(player, Items.STICK)
                                            + count(player, Items.APPLE)
                                    > 0,
                            "The decayed leaves must pay the feller at least one sapling, stick or"
                                    + " apple");
                    checkNoItemsAt(player.level(), TREE_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void mobKill(ClientGameTestContext context, TestDedicatedServerContext server) {
        Vec3 huskAt = new Vec3(MOB_X + 0.5D, STAND_Y, 2.5D);
        AtomicInteger huskId = new AtomicInteger();
        prepare(
                context,
                server,
                MOB_X,
                NEAR_Z,
                NORMAL_FOV,
                player -> {
                    ServerLevel level = player.level();
                    Husk husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
                    check(husk != null, "The mob stage needs its husk");
                    husk.setNoAi(true);
                    husk.setPersistenceRequired();
                    husk.snapTo(huskAt.x, huskAt.y, huskAt.z, 0.0F, 0.0F);
                    husk.setYHeadRot(0.0F);
                    husk.setYBodyRot(0.0F);
                    husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                    husk.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
                    husk.setHealth(1.0F);
                    level.addFreshEntity(husk);
                    huskId.set(husk.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                });
        context.waitFor(
                client -> !client.level.getEntitiesOfClass(Husk.class, siteBox(MOB_X)).isEmpty(),
                LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.mob";
        caption(
                context,
                id,
                "Killing a mob",
                "The husk's loot, sword and experience go straight to its killer");
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.lookAt(context, huskAt.add(0.0D, 1.1D, 0.0D));
        ClientGameTestPlayerActions.attackEntity(context, huskId.get());
        context.waitFor(
                client ->
                        client.level.getEntitiesOfClass(Husk.class, siteBox(MOB_X)).stream()
                                .allMatch(husk -> husk.isDeadOrDying()),
                ACTION_TIMEOUT_TICKS);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.IRON_SWORD, 1);
                    check(
                            player.totalExperience > 0,
                            "The husk's experience must go to its killer");
                    checkNoItemsAt(player.level(), MOB_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void tnt(ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos tnt = new BlockPos(TNT_X, STAND_Y, -6);
        BlockPos chest = tnt.east();
        Vec3 tntAt = Vec3.atCenterOf(tnt);
        prepare(
                context,
                server,
                TNT_X,
                TNT_START_Z,
                BLAST_FOV,
                player -> {
                    ServerLevel level = player.level();
                    level.setBlockAndUpdate(tnt, Blocks.TNT.defaultBlockState());
                    emeraldChest(level, chest);
                    player.getInventory().setItem(0, new ItemStack(Items.FLINT_AND_STEEL));
                });
        String id = "telekinesis.tnt";
        caption(
                context,
                id,
                "TNT the player lit",
                "Blast drops and the chest's 64 emeralds go to the player who lit it");
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.walkTo(context, tntAt, TNT_LIGHT_DISTANCE);
        ClientGameTestPlayerActions.lookAtBlock(context, tnt);
        ClientGameTestPlayerActions.useOnBlock(context, tnt);
        waitForServer(
                context,
                server,
                minecraftServer -> {
                    List<PrimedTnt> primed =
                            minecraftServer
                                    .overworld()
                                    .getEntitiesOfClass(PrimedTnt.class, siteBox(TNT_X));
                    primed.forEach(entity -> entity.setFuse(Math.min(entity.getFuse(), TNT_FUSE_TICKS)));
                    return !primed.isEmpty();
                });
        // Back away from the blast while the fuse burns down.
        ClientGameTestPlayerActions.walkAwayFrom(context, tntAt, TNT_SAFE_DISTANCE);
        waitForServer(
                context,
                server,
                minecraftServer ->
                        minecraftServer.overworld().getBlockState(chest).isAir()
                                && minecraftServer
                                        .overworld()
                                        .getEntitiesOfClass(PrimedTnt.class, siteBox(TNT_X))
                                        .isEmpty());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.EMERALD, 64);
                    checkNoItemsAt(player.level(), TNT_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void respawnAnchor(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos anchor = new BlockPos(ANCHOR_X, STAND_Y, -6);
        BlockPos chest = anchor.east(2);
        prepare(
                context,
                server,
                ANCHOR_X,
                BLAST_STAND_Z,
                BLAST_FOV,
                player -> {
                    ServerLevel level = player.level();
                    level.setBlockAndUpdate(
                            anchor,
                            Blocks.RESPAWN_ANCHOR
                                    .defaultBlockState()
                                    .setValue(RespawnAnchorBlock.CHARGE, 4));
                    emeraldChest(level, chest);
                    // The anchor's blast at arm's length would kill an unarmoured player; a player
                    // wearing armour would live, and this keeps the stage on camera without plating
                    // the player.
                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.RESISTANCE,
                                    BLAST_RESISTANCE_TICKS,
                                    BLAST_RESISTANCE_AMPLIFIER,
                                    false,
                                    false,
                                    false));
                });
        emptyHand(context);
        String id = "telekinesis.respawn_anchor";
        caption(
                context,
                id,
                "Respawn anchor outside the Nether",
                "The game names no cause; Telekinesis credits the player who used it");
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.lookAtBlock(context, anchor);
        ClientGameTestPlayerActions.useOnBlock(context, anchor);
        waitForServer(
                context,
                server,
                minecraftServer -> minecraftServer.overworld().getBlockState(chest).isAir());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.EMERALD, 64);
                    checkNoItemsAt(player.level(), ANCHOR_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void fullInventory(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos ore = new BlockPos(FULL_X, STAND_Y, 2);
        prepare(
                context,
                server,
                FULL_X,
                NEAR_Z,
                NORMAL_FOV,
                player -> {
                    player.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
                    player.getInventory().setItem(0, pickaxe(player.level(), null, 0));
                    for (int slot = 1; slot < 36; slot++) {
                        player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                    }
                });
        String id = "telekinesis.full_inventory";
        caption(
                context,
                id,
                "Full inventory",
                "What does not fit is put at your feet, not left at the block");
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.lookAtBlock(context, ore);
        ClientGameTestPlayerActions.mine(context, ore);
        waitForServer(
                context,
                server,
                minecraftServer ->
                        !minecraftServer
                                .overworld()
                                .getEntitiesOfClass(ItemEntity.class, siteBox(FULL_X))
                                .isEmpty());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    List<ItemEntity> items =
                            player.level().getEntitiesOfClass(ItemEntity.class, siteBox(FULL_X));
                    check(
                            items.size() == 1
                                    && items.getFirst().getItem().is(Items.DIAMOND)
                                    && items.getFirst().position().distanceTo(player.position())
                                            < 1.0D,
                            "The diamond that does not fit must lie at the player's feet: "
                                    + items);
                });
        holdFor(context, BEFORE_MILLIS);
        context.getInput().holdKeyFor(options -> options.keyDown, STEP_BACK_TICKS);
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void turnedOff(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos ore = new BlockPos(OFF_X, STAND_Y, 1);
        prepare(
                context,
                server,
                OFF_X,
                NEAR_Z,
                NORMAL_FOV,
                player -> {
                    player.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
                    player.getInventory().setItem(0, pickaxe(player.level(), null, 0));
                });
        String id = "telekinesis.off";
        caption(
                context,
                id,
                "Telekinesis turned off",
                "After /telekinesis disable, drops stay in the world as usual");
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        context.runOnClient(client -> client.player.connection.sendCommand("telekinesis disable"));
        waitForServer(
                context,
                server,
                minecraftServer -> !TelekinesisPlayerSettings.isEffective(player(minecraftServer)));
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.lookAtBlock(context, ore);
        ClientGameTestPlayerActions.mine(context, ore);
        waitForServer(
                context,
                server,
                minecraftServer ->
                        !minecraftServer
                                .overworld()
                                .getEntitiesOfClass(ItemEntity.class, siteBox(OFF_X))
                                .isEmpty());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.DIAMOND, 0);
                    List<ItemEntity> items =
                            player.level().getEntitiesOfClass(ItemEntity.class, siteBox(OFF_X));
                    check(
                            items.size() == 1
                                    && items.getFirst().getItem().is(Items.DIAMOND)
                                    && items.getFirst().position().distanceTo(Vec3.atCenterOf(ore))
                                            < 1.5D,
                            "With Telekinesis off the diamond must stay at the ore: " + items);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
        context.runOnClient(client -> client.player.connection.sendCommand("telekinesis enable"));
        waitForServer(
                context,
                server,
                minecraftServer -> TelekinesisPlayerSettings.isEffective(player(minecraftServer)));
    }

    private static void netherBed(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos chest = NETHER_BED_FOOT.north().east(2);
        Vec3 stand = new Vec3(0.5D, NETHER_FLOOR_Y + 1.0D, BLAST_STAND_Z);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    ServerLevel nether = minecraftServer.getLevel(Level.NETHER);
                    check(nether != null, "The test server must have a Nether");
                    resetPlayer(player);
                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.RESISTANCE,
                                    BLAST_RESISTANCE_TICKS,
                                    BLAST_RESISTANCE_AMPLIFIER,
                                    false,
                                    false,
                                    false));
                    player.teleportTo(
                            nether,
                            stand.x,
                            stand.y,
                            stand.z,
                            Set.of(),
                            NORTH,
                            0.0F,
                            true);
                });
        context.waitFor(
                client ->
                        client.level != null
                                && client.level.dimension() == Level.NETHER
                                && client.gui.screen() == null
                                && client.player.position().distanceTo(stand) < 0.01D,
                LOAD_TIMEOUT_TICKS);
        context.runOnClient(
                client -> {
                    client.options.fov().set(BLAST_FOV);
                    client.gui.hud.getChat().clearMessages(false);
                });
        waitForChunkWindow(context);
        waitForMeshesIdle(context);
        emptyHand(context);
        String id = "telekinesis.nether_bed";
        caption(
                context,
                id,
                "Bed in the Nether",
                "Vanilla names no cause; the drops go to the player who used the bed");
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.lookAtBlock(context, NETHER_BED_FOOT);
        ClientGameTestPlayerActions.useOnBlock(context, NETHER_BED_FOOT);
        waitForServer(
                context,
                server,
                minecraftServer ->
                        minecraftServer.getLevel(Level.NETHER).getBlockState(chest).isAir());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.EMERALD, 64);
                    check(
                            player.level()
                                    .getEntitiesOfClass(
                                            ItemEntity.class,
                                            new AABB(NETHER_BED_FOOT).inflate(16.0D))
                                    .isEmpty(),
                            "No drop of the bed's blast may stay in the world");
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void prepare(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            int siteX,
            double standZ,
            int fov,
            Consumer<ServerPlayer> setUp) {
        Vec3 stand = new Vec3(siteX + 0.5D, STAND_Y, standZ);
        ItemStack[] hand = new ItemStack[1];
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    resetPlayer(player);
                    player.level()
                            .getEntitiesOfClass(ItemEntity.class, siteBox(siteX))
                            .forEach(ItemEntity::discard);
                    player.teleportTo(
                            minecraftServer.overworld(),
                            stand.x,
                            stand.y,
                            stand.z,
                            Set.of(),
                            NORTH,
                            0.0F,
                            true);
                    clearFire(player.level());
                    setUp.accept(player);
                    hand[0] = player.getInventory().getItem(0).copy();
                });
        context.waitFor(
                client ->
                        client.level.dimension() == Level.OVERWORLD
                                && client.player.position().distanceTo(stand) < 0.01D
                                && ItemStack.isSameItem(
                                        client.player.getInventory().getItem(0), hand[0])
                                && client.player.totalExperience == 0,
                LOAD_TIMEOUT_TICKS);
        context.runOnClient(
                client -> {
                    client.options.fov().set(fov);
                    client.gui.hud.getChat().clearMessages(false);
                    // The server does not send the selected slot; the stage's tool is in slot 0.
                    client.player.getInventory().setSelectedSlot(0);
                });
        waitForChunkWindow(context);
        waitForMeshesIdle(context);
    }

    private static void emptyHand(ClientGameTestContext context) {
        context.runOnClient(client -> client.player.getInventory().setSelectedSlot(8));
        context.waitTicks(2);
    }

    private static void clearFire(ServerLevel level) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = STAGE_MIN_X; x <= STAGE_MAX_X; x++) {
            for (int z = STAGE_MIN_Z; z <= STAGE_MAX_Z; z++) {
                for (int y = FLOOR_Y - 4; y <= STAND_Y + 3; y++) {
                    if (level.getBlockState(cursor.set(x, y, z)).is(Blocks.FIRE)) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static void resetPlayer(ServerPlayer player) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
        player.setExperienceLevels(0);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.clearFire();
        player.removeAllEffects();
    }

    private static void caption(
            ClientGameTestContext context, String id, String title, String subtitle) {
        ClientGameTestRecorder.showStep(context, id, title, subtitle);
        context.waitTicks(CAPTION_LEAD_TICKS);
    }

    /** Waits by the clock, as the film runs, not by ticks, which run slower under load. */
    private static void holdFor(ClientGameTestContext context, long millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            context.waitTick();
        }
    }

    private static void waitForServer(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            Predicate<MinecraftServer> condition) {
        for (int tick = 0; tick < ACTION_TIMEOUT_TICKS; tick++) {
            if (server.computeOnServer(condition::test)) {
                return;
            }
            context.waitTick();
        }
        throw new AssertionError("The server never reached the stage's expected state");
    }

    private static void stageStart(String id) {
        System.out.println("STAGE_START " + System.currentTimeMillis() + " " + id);
    }

    /**
     * Marks the end of a filmed stage and keeps the scene as it is for a moment: the cut keeps a
     * little picture after the end, which must still show this stage.
     */
    private static void stageEnd(ClientGameTestContext context, String id) {
        System.out.println("STAGE_END " + System.currentTimeMillis() + " " + id);
        context.waitTicks(HOLD_AFTER_END_TICKS);
    }

    private static void waitForChunkWindow(ClientGameTestContext context) {
        context.waitFor(
                client -> {
                    int centreX = SectionPos.blockToSectionCoord(client.player.getX());
                    int centreZ = SectionPos.blockToSectionCoord(client.player.getZ());
                    int radius = RENDER_DISTANCE - 2;
                    for (int chunkX = centreX - radius; chunkX <= centreX + radius; chunkX++) {
                        for (int chunkZ = centreZ - radius; chunkZ <= centreZ + radius; chunkZ++) {
                            int dx = chunkX - centreX;
                            int dz = chunkZ - centreZ;
                            if (dx * dx + dz * dz > radius * radius) {
                                continue;
                            }
                            // ClientLevel.hasChunk is true for every position, so ask the chunk
                            // cache.
                            if (client.level
                                            .getChunkSource()
                                            .getChunk(chunkX, chunkZ, ChunkStatus.FULL, false)
                                    == null) {
                                return false;
                            }
                        }
                    }
                    return true;
                },
                LOAD_TIMEOUT_TICKS);
    }

    private static void waitForMeshesIdle(ClientGameTestContext context) {
        int idleTicks = 0;
        for (int tick = 0; tick < LOAD_TIMEOUT_TICKS; tick++) {
            boolean idle =
                    context.computeOnClient(
                            client -> client.levelRenderer.hasRenderedAllSections());
            idleTicks = idle ? idleTicks + 1 : 0;
            if (idleTicks >= SETTLE_TICKS) {
                return;
            }
            context.waitTick();
        }
        throw new AssertionError("The client did not finish building the chunks around the player");
    }

    private static void buildWorld(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = STAGE_MIN_X; x <= STAGE_MAX_X; x++) {
            for (int z = STAGE_MIN_Z; z <= STAGE_MAX_Z; z++) {
                for (int y = FLOOR_Y - 3; y <= FLOOR_Y; y++) {
                    overworld.setBlock(
                            cursor.set(x, y, z),
                            Blocks.STONE.defaultBlockState(),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
        BlockPos sapling = new BlockPos(TREE_X, STAND_Y, 0);
        overworld.setBlock(
                sapling.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        boolean grown =
                overworld
                        .registryAccess()
                        .lookupOrThrow(Registries.FEATURE)
                        .getOrThrow(TreeFeatures.OAK)
                        .value()
                        .place(
                                overworld,
                                overworld.getChunkSource().getGenerator(),
                                RandomSource.create(4L),
                                sapling);
        check(grown, "The tree stage's oak must grow");

        ServerLevel nether = server.getLevel(Level.NETHER);
        check(nether != null, "The test server must have a Nether");
        for (int x = -10; x <= 10; x++) {
            for (int z = -14; z <= 9; z++) {
                for (int y = NETHER_FLOOR_Y - 2; y <= NETHER_FLOOR_Y; y++) {
                    nether.setBlock(
                            cursor.set(x, y, z),
                            Blocks.STONE.defaultBlockState(),
                            Block.UPDATE_CLIENTS);
                }
                if (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) {
                    nether.setBlock(
                            cursor.set(x, NETHER_FLOOR_Y + 6, z),
                            Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15),
                            Block.UPDATE_ALL);
                }
            }
        }
        BlockState bed =
                Blocks.BED
                        .red()
                        .defaultBlockState()
                        .setValue(BedBlock.FACING, Direction.NORTH)
                        .setValue(BedBlock.PART, BedPart.FOOT);
        nether.setBlock(NETHER_BED_FOOT, bed, Block.UPDATE_CLIENTS);
        nether.setBlock(
                NETHER_BED_FOOT.north(),
                bed.setValue(BedBlock.PART, BedPart.HEAD),
                Block.UPDATE_CLIENTS);
        emeraldChest(nether, NETHER_BED_FOOT.north().east(2));

        command(server, "gamerule advance_time false");
        command(server, "gamerule advance_weather false");
        command(server, "gamerule spawn_mobs false");
        command(server, "weather clear");
        command(server, "time set noon");
    }

    private static void command(MinecraftServer server, String command) {
        CommandSourceStack source =
                server.createCommandSourceStack()
                        .withPermission(PermissionSet.ALL_PERMISSIONS)
                        .withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    private static void emeraldChest(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity entity = (ChestBlockEntity) level.getBlockEntity(pos);
        check(entity != null, "Expected a chest at " + pos);
        entity.setItem(0, new ItemStack(Items.EMERALD, 64));
    }

    private static ItemStack pickaxe(
            ServerLevel level, ResourceKey<Enchantment> extra, int extraLevel) {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.enchant(enchantment(level, Enchantments.EFFICIENCY), 5);
        if (extra != null) {
            stack.enchant(enchantment(level, extra), extraLevel);
        }
        return stack;
    }

    private static ItemStack axe(ServerLevel level) {
        ItemStack stack = new ItemStack(Items.DIAMOND_AXE);
        stack.enchant(enchantment(level, Enchantments.EFFICIENCY), 5);
        return stack;
    }

    private static Holder<Enchantment> enchantment(
            ServerLevel level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static AABB siteBox(int siteX) {
        return new AABB(
                siteX - 12,
                FLOOR_Y - 8,
                STAGE_MIN_Z - 4,
                siteX + 12,
                FLOOR_Y + 16,
                STAGE_MAX_Z + 4);
    }

    private static void checkNoItemsAt(ServerLevel level, int siteX) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, siteBox(siteX));
        check(items.isEmpty(), "No drop may stay in the world at site " + siteX + ": " + items);
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void checkCount(ServerPlayer player, Item item, int expected) {
        int actual = count(player, item);
        check(
                actual == expected,
                "Expected " + expected + " " + item + " in the inventory, found " + actual);
    }

    private static void checkAtLeast(ServerPlayer player, Item item, int minimum) {
        int actual = count(player, item);
        check(
                actual >= minimum,
                "Expected at least " + minimum + " " + item + " in the inventory, found " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
