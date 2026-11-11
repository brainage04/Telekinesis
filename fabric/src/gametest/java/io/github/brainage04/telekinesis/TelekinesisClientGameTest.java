package io.github.brainage04.telekinesis;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.brainage04.minecraftmodconventions.ClientGameTestPlayerActions;
import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariants;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.DyeColor;
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
 * The film of Telekinesis' showcase: one captioned stage per feature.
 *
 * <p>Every stage plays like a person: the player starts the stage already looking at the block or
 * entity it acts on, walks with the movement keys, mines and attacks with the held attack key and
 * uses with the use key, within vanilla reach. {@link ClientGameTestPlayerActions} keeps it that
 * way and fails the stage when the crosshair is not on the target or the target is out of reach.
 *
 * <p>Server-side calls only build the scene, spawn the stage's mobs, set game rules and place the
 * player between stages, before that stage is filmed.
 */
@SuppressWarnings("UnstableApiUsage")
public final class TelekinesisClientGameTest implements FabricClientGameTest {
    private static final int FLOOR_Y = 63;
    private static final int STAND_Y = FLOOR_Y + 1;
    private static final int STAGE_MIN_X = -110;
    private static final int STAGE_MAX_X = 278;
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
    private static final int CREDITED_X = 96;
    private static final int GOLEM_X = 120;
    private static final int FRAME_X = 144;
    private static final int STAND_X = 168;
    private static final int PAINTING_X = 192;
    private static final int BOAT_X = 216;
    private static final int CART_X = 240;
    private static final int CUSHION_X = 264;

    private static final double NEAR_Z = 4.5D;

    /** Far enough back that the golem reads as a figure in the scene, not a wall of pixels. */
    private static final double FAR_Z = 8.5D;

    /** Steps back far enough that both the chest's and the mob's scattered drops fit one frame. */
    private static final int BOTH_DROPS_STEP_BACK_TICKS = 15;

    /** Vanilla gives a new item 10 ticks before a player may pick it up; wait past that. */
    private static final int PICKUP_DELAY_TICKS = 30;

    /** Two blocks from the oak's trunk: close enough to break it from where the stage starts. */
    private static final double TREE_START_Z = 2.5D;

    /** How close the player stands to the TNT to light it: inside reach, back from the blast. */
    private static final double TNT_LIGHT_DISTANCE = 3.0D;

    /** How far the player steps back from the lit TNT, outside a power-4 blast's damage. */
    private static final double TNT_SAFE_DISTANCE = 7.0D;

    /**
     * Where the player stands to use the anchor and the Nether bed: the far edge of vanilla reach.
     */
    private static final double BLAST_STAND_Z = -1.0D;

    /** The tick a lit TNT is given, so the player can step back before it blows. */
    private static final int TNT_FUSE_TICKS = 60;

    private static final int NETHER_FLOOR_Y = 200;
    private static final BlockPos NETHER_BED_FOOT = new BlockPos(0, NETHER_FLOOR_Y + 1, -6);

    /** The longest oak the tree stage can fell: its top log has to be inside vanilla reach. */
    private static final int MAX_TREE_LOGS = 7;

    /** The log after the bottom two: the player walks under the trunk as this one breaks. */
    private static final int LOG_BROKEN_WHILE_WALKING_IN = 2;

    /** Where the player stops walking in, under the trunk, in blocks along z. */
    private static final double TRUNK_STAND_Z = 0.8D;

    /** The most punches a breakable entity may take before the stage fails. */
    private static final int MAX_PUNCHES = 12;

    /** The shortest gap a painting or a frame needs: a one-block stone wall behind it. */
    private static final int WALL_Z = 1;

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

    private static final Map<String, String[]> CAPTIONS = loadCaptions();

    private static final int LOAD_TIMEOUT_TICKS = 2400;
    private static final int ACTION_TIMEOUT_TICKS = 200;

    /** Ticks a block may take to break before the stage fails. */
    private static final int BREAK_TIMEOUT_TICKS = 300;

    private static final long BEFORE_MILLIS = 400L;
    private static final long AFTER_MILLIS = 600L;

    /** The pause after the caption before the TNT stage's right click: about half a second in. */
    private static final long TNT_USE_DELAY_MILLIS = 400L;

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
                                creditedKill(context, server);
                                uncreditedKill(context, server);
                                itemFrame(context, server);
                                armourStand(context, server);
                                painting(context, server);
                                cushion(context, server);
                                chestBoat(context, server);
                                chestMinecart(context, server);
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
                Vec3.atCenterOf(ore),
                player -> {
                    player.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
                    player.getInventory()
                            .setItem(0, pickaxe(player.level(), Enchantments.FORTUNE, 3));
                });
        String id = "telekinesis.mining";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        mine(context, server, ore);
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
                Vec3.atCenterOf(chest),
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
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        mine(context, server, chest);
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
                Vec3.atCenterOf(logs.getFirst()),
                player -> player.getInventory().setItem(0, axe(player.level())));
        String id = "telekinesis.tree";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        fellTree(context, logs);
        // No walk-out: stand where the trunk was and look straight up at the leaves coming down.
        ClientGameTestPlayerActions.lookStraightUp(context);
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
                    BREAK_TIMEOUT_TICKS);
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

    /**
     * Holds the attack key through the whole felling, as a player does: the crosshair moves to each
     * log in turn and the third breaks while the player walks in under the trunk, so the swing
     * never stops between logs. The guard checks the log under the crosshair before each break; the
     * whole trunk is asserted by the caller once the stage's action is over.
     */
    private static void fellTree(ClientGameTestContext context, List<BlockPos> logs) {
        ClientGameTestPlayerActions.requireLookingAtBlock(context, logs.getFirst());
        context.getInput().holdKey(options -> options.keyAttack);
        try {
            for (int index = 0; index < logs.size(); index++) {
                BlockPos log = logs.get(index);
                if (index == LOG_BROKEN_WHILE_WALKING_IN) {
                    walkUnderTheTrunk(context, log);
                    continue;
                }
                ClientGameTestPlayerActions.lookAtBlock(context, log);
                ClientGameTestPlayerActions.requireLookingAtBlock(context, log);
                context.waitFor(
                        client -> client.level.getBlockState(log).isAir(), BREAK_TIMEOUT_TICKS);
            }
        } finally {
            context.getInput().releaseKey(options -> options.keyUp);
            context.getInput().releaseKey(options -> options.keyAttack);
        }
    }

    /**
     * Breaks the log that is overhead as the player steps into the trunk's gap: the forward key
     * goes down first, the crosshair follows the log up, and the key comes back up under the trunk.
     */
    private static void walkUnderTheTrunk(ClientGameTestContext context, BlockPos log) {
        context.getInput().holdKey(options -> options.keyUp);
        try {
            ClientGameTestPlayerActions.lookAtBlock(context, log);
            ClientGameTestPlayerActions.requireLookingAtBlock(context, log);
            context.waitFor(
                    client -> client.player.position().z() <= TRUNK_STAND_Z, ACTION_TIMEOUT_TICKS);
            context.waitFor(client -> client.level.getBlockState(log).isAir(), BREAK_TIMEOUT_TICKS);
        } finally {
            context.getInput().releaseKey(options -> options.keyUp);
        }
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
                huskAt.add(0.0D, 1.1D, 0.0D),
                player -> {
                    Husk husk = spawnHusk(player.level(), huskAt, Items.IRON_SWORD, 1.0F);
                    huskId.set(husk.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                });
        context.waitFor(
                client -> !client.level.getEntitiesOfClass(Husk.class, siteBox(MOB_X)).isEmpty(),
                LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.mob";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
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

    /**
     * The player's hit, then a death from lava: the game credits the player's hit for five seconds,
     * so the loot of a husk the lava finished still comes to the player who punched it.
     */
    private static void creditedKill(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos pool = new BlockPos(CREDITED_X, STAND_Y, 2);
        Vec3 huskAt = new Vec3(CREDITED_X + 0.5D, STAND_Y, 2.5D);
        AtomicInteger huskId = new AtomicInteger();
        prepare(
                context,
                server,
                CREDITED_X,
                NEAR_Z,
                NORMAL_FOV,
                huskAt.add(0.0D, 1.1D, 0.0D),
                player -> {
                    ServerLevel level = player.level();
                    // A one-block lava basin the husk stands in, walled in stone so the lava stays
                    // put. Its health carries it past the punch; the lava finishes it.
                    level.setBlockAndUpdate(pool, Blocks.LAVA.defaultBlockState());
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        level.setBlockAndUpdate(
                                pool.relative(side), Blocks.STONE.defaultBlockState());
                    }
                    Husk husk = spawnHusk(level, huskAt, Items.DIAMOND_SWORD, 44.0F);
                    huskId.set(husk.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                });
        context.waitFor(
                client ->
                        !client.level.getEntitiesOfClass(Husk.class, siteBox(CREDITED_X)).isEmpty(),
                LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.credited_kill";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.attackEntity(context, huskId.get());
        context.waitFor(
                client ->
                        client.level.getEntitiesOfClass(Husk.class, siteBox(CREDITED_X)).stream()
                                .allMatch(Husk::isDeadOrDying),
                BREAK_TIMEOUT_TICKS);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.DIAMOND_SWORD, 2);
                    check(
                            player.totalExperience > 0,
                            "A kill the game credits after the player's hit pays its experience");
                    checkNoItemsAt(player.level(), CREDITED_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    /**
     * The same death, but the game credits the iron golem that did it: the husk never had the
     * player's hit, so the loot stays where the game spawned it.
     */
    private static void uncreditedKill(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        // The husk stands in the foreground and the golem comes in from behind it, so the kill, the
        // dying husk and the sword it leaves on the ground all stay in view.
        Vec3 huskAt = new Vec3(GOLEM_X - 1.0D, STAND_Y, 2.0D);
        Vec3 golemAt = new Vec3(GOLEM_X + 2.0D, STAND_Y, 1.5D);
        prepare(
                context,
                server,
                GOLEM_X,
                FAR_Z,
                NORMAL_FOV,
                huskAt.add(0.0D, 1.0D, 0.0D),
                player -> {
                    ServerLevel level = player.level();
                    spawnHusk(level, huskAt, Items.DIAMOND_SWORD, 4.0F);
                    IronGolem golem =
                            EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
                    check(golem != null, "The uncredited kill stage needs its golem");
                    // A player-made golem never turns on the player, only on the husk it sees.
                    golem.setPlayerCreated(true);
                    golem.setPersistenceRequired();
                    golem.snapTo(golemAt.x, golemAt.y, golemAt.z, 0.0F, 0.0F);
                    level.addFreshEntity(golem);
                });
        context.waitFor(
                client ->
                        !client.level.getEntitiesOfClass(Husk.class, siteBox(GOLEM_X)).isEmpty()
                                && !client.level
                                        .getEntitiesOfClass(IronGolem.class, siteBox(GOLEM_X))
                                        .isEmpty(),
                LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.uncredited_kill";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        context.waitFor(
                client ->
                        client.level.getEntitiesOfClass(Husk.class, siteBox(GOLEM_X)).stream()
                                .allMatch(Husk::isDeadOrDying),
                ACTION_TIMEOUT_TICKS);
        context.waitTicks(PICKUP_DELAY_TICKS);
        waitForServer(
                context,
                server,
                minecraftServer -> {
                    List<ItemEntity> items =
                            minecraftServer
                                    .overworld()
                                    .getEntitiesOfClass(ItemEntity.class, siteBox(GOLEM_X));
                    return items.stream().anyMatch(item -> item.getItem().is(Items.DIAMOND_SWORD));
                });
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.DIAMOND_SWORD, 0);
                    check(
                            player.totalExperience == 0,
                            "A kill the game credits to a mob pays the player nothing");
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void itemFrame(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos wall = new BlockPos(FRAME_X, STAND_Y + 1, WALL_Z);
        BlockPos frameAt = wall.south();
        Vec3 frameLook = new Vec3(FRAME_X + 0.5D, STAND_Y + 1.5D, frameAt.getZ() + 0.03D);
        AtomicInteger frameId = new AtomicInteger();
        prepare(
                context,
                server,
                FRAME_X,
                NEAR_Z,
                NORMAL_FOV,
                frameLook,
                player -> {
                    ServerLevel level = player.level();
                    for (int y = -1; y <= 1; y++) {
                        level.setBlockAndUpdate(
                                wall.offset(0, y, 0), Blocks.STONE.defaultBlockState());
                    }
                    ItemFrame frame = new ItemFrame(level, frameAt, Direction.SOUTH);
                    frame.setItem(new ItemStack(Items.DIAMOND));
                    check(level.addFreshEntity(frame), "The frame stage needs its item frame");
                    frameId.set(frame.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                });
        context.waitFor(
                client -> client.level.getEntity(frameId.get()) != null, LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.item_frame";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, frameLook, frameId.get());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.DIAMOND, 1);
                    checkCount(player, Items.ITEM_FRAME, 1);
                    checkNoItemsAt(player.level(), FRAME_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void armourStand(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        Vec3 standAt = new Vec3(STAND_X + 0.5D, STAND_Y, 2.5D);
        Vec3 standLook = standAt.add(0.0D, 1.0D, 0.0D);
        AtomicInteger standId = new AtomicInteger();
        prepare(
                context,
                server,
                STAND_X,
                NEAR_Z,
                NORMAL_FOV,
                standLook,
                player -> {
                    ServerLevel level = player.level();
                    ArmorStand stand = new ArmorStand(level, standAt.x, standAt.y, standAt.z);
                    stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                    check(level.addFreshEntity(stand), "The stand stage needs its armour stand");
                    standId.set(stand.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                });
        context.waitFor(
                client -> client.level.getEntity(standId.get()) != null, LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.armour_stand";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        // An armour stand breaks on the second punch, which has to land within five ticks of the
        // first (ArmorStand.java:330), so a player double-clicks it.
        ClientGameTestPlayerActions.requireLookingAtEntity(context, standId.get());
        context.getInput().pressKey(options -> options.keyAttack);
        context.waitTicks(2);
        ClientGameTestPlayerActions.requireLookingAtEntity(context, standId.get());
        context.getInput().pressKey(options -> options.keyAttack);
        context.waitFor(
                client -> client.level.getEntity(standId.get()) == null, ACTION_TIMEOUT_TICKS);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.ARMOR_STAND, 1);
                    checkCount(player, Items.IRON_HELMET, 1);
                    checkCount(player, Items.DIAMOND_SWORD, 1);
                    checkNoItemsAt(player.level(), STAND_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void painting(ClientGameTestContext context, TestDedicatedServerContext server) {
        BlockPos wall = new BlockPos(PAINTING_X, STAND_Y + 1, WALL_Z);
        BlockPos paintingAt = wall.south();
        Vec3 paintingLook = new Vec3(PAINTING_X + 0.5D, STAND_Y + 1.5D, paintingAt.getZ() + 0.03D);
        AtomicInteger paintingId = new AtomicInteger();
        prepare(
                context,
                server,
                PAINTING_X,
                NEAR_Z,
                NORMAL_FOV,
                paintingLook,
                player -> {
                    ServerLevel level = player.level();
                    for (int y = -1; y <= 1; y++) {
                        level.setBlockAndUpdate(
                                wall.offset(0, y, 0), Blocks.STONE.defaultBlockState());
                    }
                    Painting picture =
                            new Painting(
                                    level,
                                    paintingAt,
                                    Direction.SOUTH,
                                    level.registryAccess()
                                            .lookupOrThrow(Registries.PAINTING_VARIANT)
                                            .getOrThrow(PaintingVariants.KEBAB));
                    check(level.addFreshEntity(picture), "The painting stage needs its painting");
                    paintingId.set(picture.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                });
        context.waitFor(
                client -> client.level.getEntity(paintingId.get()) != null, LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.painting";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, paintingLook, paintingId.get());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.PAINTING, 1);
                    checkNoItemsAt(player.level(), PAINTING_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void cushion(ClientGameTestContext context, TestDedicatedServerContext server) {
        Vec3 cushionAt = new Vec3(CUSHION_X + 0.5D, STAND_Y, 2.5D);
        Vec3 cushionLook = cushionAt.add(0.0D, 0.25D, 0.0D);
        AtomicInteger cushionId = new AtomicInteger();
        prepare(
                context,
                server,
                CUSHION_X,
                NEAR_Z,
                NORMAL_FOV,
                cushionLook,
                player -> {
                    ServerLevel level = player.level();
                    Cushion cushion = EntityTypes.CUSHION.create(level, EntitySpawnReason.COMMAND);
                    check(cushion != null, "The cushion stage needs its cushion");
                    cushion.setColor(DyeColor.LIGHT_BLUE);
                    cushion.snapTo(cushionAt.x, cushionAt.y, cushionAt.z, 0.0F, 0.0F);
                    check(level.addFreshEntity(cushion), "The cushion must spawn");
                    cushionId.set(cushion.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                });
        context.waitFor(
                client -> client.level.getEntity(cushionId.get()) != null, LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.cushion";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, cushionLook, cushionId.get());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.CUSHION.pick(DyeColor.LIGHT_BLUE), 1);
                    checkNoItemsAt(player.level(), CUSHION_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void chestBoat(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        Vec3 boatAt = new Vec3(BOAT_X + 0.5D, STAND_Y, 2.5D);
        Vec3 boatLook = boatAt.add(0.0D, 0.3D, 0.0D);
        AtomicInteger boatId = new AtomicInteger();
        prepare(
                context,
                server,
                BOAT_X,
                NEAR_Z,
                NORMAL_FOV,
                boatLook,
                player -> {
                    ServerLevel level = player.level();
                    ChestBoat boat =
                            EntityTypes.OAK_CHEST_BOAT.create(level, EntitySpawnReason.COMMAND);
                    check(boat != null, "The boat stage needs its chest boat");
                    boat.snapTo(boatAt.x, boatAt.y, boatAt.z, 0.0F, 0.0F);
                    boat.setItem(0, new ItemStack(Items.EMERALD, 3));
                    check(level.addFreshEntity(boat), "The chest boat must spawn");
                    boatId.set(boat.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                });
        context.waitFor(client -> client.level.getEntity(boatId.get()) != null, LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.chest_boat";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, boatLook, boatId.get());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.OAK_CHEST_BOAT, 1);
                    checkCount(player, Items.EMERALD, 3);
                    checkNoItemsAt(player.level(), BOAT_X);
                });
        holdFor(context, AFTER_MILLIS);
        stageEnd(context, id);
    }

    private static void chestMinecart(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        Vec3 cartAt = new Vec3(CART_X + 0.5D, STAND_Y, 2.5D);
        Vec3 cartLook = cartAt.add(0.0D, 0.35D, 0.0D);
        AtomicInteger cartId = new AtomicInteger();
        prepare(
                context,
                server,
                CART_X,
                NEAR_Z,
                NORMAL_FOV,
                cartLook,
                player -> {
                    ServerLevel level = player.level();
                    MinecartChest cart =
                            EntityTypes.CHEST_MINECART.create(level, EntitySpawnReason.COMMAND);
                    check(cart != null, "The cart stage needs its chest minecart");
                    cart.snapTo(cartAt.x, cartAt.y, cartAt.z, 0.0F, 0.0F);
                    cart.setItem(0, new ItemStack(Items.EMERALD, 3));
                    check(level.addFreshEntity(cart), "The chest minecart must spawn");
                    cartId.set(cart.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                });
        context.waitFor(client -> client.level.getEntity(cartId.get()) != null, LOAD_TIMEOUT_TICKS);
        String id = "telekinesis.chest_cart";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, cartLook, cartId.get());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.CHEST_MINECART, 1);
                    checkCount(player, Items.EMERALD, 3);
                    checkNoItemsAt(player.level(), CART_X);
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
                tnt.getZ() + TNT_LIGHT_DISTANCE,
                BLAST_FOV,
                tntAt,
                player -> {
                    ServerLevel level = player.level();
                    level.setBlockAndUpdate(tnt, Blocks.TNT.defaultBlockState());
                    emeraldChest(level, chest);
                    player.getInventory().setItem(0, new ItemStack(Items.FLINT_AND_STEEL));
                });
        String id = "telekinesis.tnt";
        caption(context, id);
        stageStart(id);
        holdFor(context, TNT_USE_DELAY_MILLIS);
        ClientGameTestPlayerActions.useOnBlock(context, tnt);
        waitForServer(
                context,
                server,
                minecraftServer -> {
                    List<PrimedTnt> primed =
                            minecraftServer
                                    .overworld()
                                    .getEntitiesOfClass(PrimedTnt.class, siteBox(TNT_X));
                    primed.forEach(
                            entity -> entity.setFuse(Math.min(entity.getFuse(), TNT_FUSE_TICKS)));
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
                Vec3.atCenterOf(anchor),
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
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
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
                Vec3.atCenterOf(ore),
                player -> {
                    player.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
                    player.getInventory().setItem(0, pickaxe(player.level(), null, 0));
                    for (int slot = 1; slot < 36; slot++) {
                        player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                    }
                });
        String id = "telekinesis.full_inventory";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        mine(context, server, ore);
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

    /**
     * The negative case for the two most valuable positives: with the personal setting off, a
     * broken chest and a killed mob both leave everything in the world, on camera.
     */
    private static void turnedOff(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        // The chest breaks from the edge of reach, so its scattered contents land well outside the
        // player's pickup range; a break closer than that lets vanilla collect the emeralds.
        BlockPos chest = new BlockPos(OFF_X + 3, STAND_Y, 2);
        Vec3 huskAt = new Vec3(OFF_X - 1.5D, STAND_Y, 2.5D);
        Vec3 bothDrops = new Vec3(OFF_X + 1.0D, STAND_Y + 0.5D, 2.5D);
        AtomicInteger huskId = new AtomicInteger();
        prepare(
                context,
                server,
                OFF_X,
                NEAR_Z,
                NORMAL_FOV,
                huskAt.add(0.0D, 1.1D, 0.0D),
                player -> {
                    ServerLevel level = player.level();
                    emeraldChest(level, chest);
                    Husk husk = spawnHusk(level, huskAt, Items.IRON_SWORD, 1.0F);
                    huskId.set(husk.getId());
                    player.getInventory().setItem(0, axe(level));
                });
        String id = "telekinesis.off";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
        context.runOnClient(client -> client.player.connection.sendCommand("telekinesis disable"));
        waitForServer(
                context,
                server,
                minecraftServer -> !TelekinesisPlayerSettings.isEffective(player(minecraftServer)));
        holdFor(context, BEFORE_MILLIS);
        // The kill comes first and the chest break last, so the closing shot can hold both piles of
        // drops in one frame while the hotbar stays empty.
        ClientGameTestPlayerActions.attackEntity(context, huskId.get());
        context.waitFor(
                client ->
                        client.level.getEntitiesOfClass(Husk.class, siteBox(OFF_X)).stream()
                                .allMatch(Husk::isDeadOrDying),
                ACTION_TIMEOUT_TICKS);
        mine(context, server, chest);
        ClientGameTestPlayerActions.lookLevel(context, new Vec3(OFF_X + 0.5D, STAND_Y, -10.0D));
        context.getInput().holdKeyFor(options -> options.keyDown, BOTH_DROPS_STEP_BACK_TICKS);
        ClientGameTestPlayerActions.lookAt(context, bothDrops);
        context.waitTicks(PICKUP_DELAY_TICKS);
        waitForServer(
                context,
                server,
                minecraftServer -> {
                    List<ItemEntity> items =
                            minecraftServer
                                    .overworld()
                                    .getEntitiesOfClass(ItemEntity.class, siteBox(OFF_X));
                    return items.stream().anyMatch(item -> item.getItem().is(Items.CHEST))
                            && items.stream().anyMatch(item -> item.getItem().is(Items.EMERALD))
                            && items.stream().anyMatch(item -> item.getItem().is(Items.IRON_SWORD));
                });
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkCount(player, Items.CHEST, 0);
                    checkCount(player, Items.EMERALD, 0);
                    checkCount(player, Items.IRON_SWORD, 0);
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
                            nether, stand.x, stand.y, stand.z, Set.of(), NORTH, 0.0F, true);
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
        ClientGameTestPlayerActions.lookAtBlock(context, NETHER_BED_FOOT);
        String id = "telekinesis.nether_bed";
        caption(context, id);
        stageStart(id);
        holdFor(context, BEFORE_MILLIS);
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

    /**
     * Places the player at the stage and leaves it looking at the stage's first target, so every
     * stage opens on the block or entity it acts on instead of a level view.
     */
    private static void prepare(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            int siteX,
            double standZ,
            int fov,
            Vec3 firstLook,
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
        ClientGameTestPlayerActions.lookAt(context, firstLook);
    }

    /** Punches the entity under the crosshair until it is gone, one press per attack cooldown. */
    private static void punchUntilGone(ClientGameTestContext context, Vec3 look, int entityId) {
        for (int punch = 0; punch < MAX_PUNCHES; punch++) {
            context.waitFor(
                    client -> client.player.getAttackStrengthScale(0.0F) >= 1.0F,
                    ACTION_TIMEOUT_TICKS);
            ClientGameTestPlayerActions.lookAt(context, look);
            ClientGameTestPlayerActions.requireLookingAtEntity(context, entityId);
            context.getInput().pressKey(options -> options.keyAttack);
            context.waitTicks(3);
            if (context.computeOnClient(client -> client.level.getEntity(entityId) == null)) {
                return;
            }
        }
        throw new AssertionError("Entity " + entityId + " survived " + MAX_PUNCHES + " punches");
    }

    /** Spawns the stage's husk where the camera can see it, with its own sword as its loot. */
    private static Husk spawnHusk(ServerLevel level, Vec3 at, Item inHand, float health) {
        Husk husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
        check(husk != null, "The stage needs its husk");
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        husk.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        husk.setYHeadRot(0.0F);
        husk.setYBodyRot(0.0F);
        husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(inHand));
        husk.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        AttributeInstance maxHealth = husk.getAttribute(Attributes.MAX_HEALTH);
        check(maxHealth != null, "The husk needs its max-health attribute");
        maxHealth.setBaseValue(Math.max(health, 20.0F));
        husk.setHealth(health);
        check(level.addFreshEntity(husk), "The husk must spawn");
        return husk;
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

    /**
     * Shows the stage's caption from the recorded text, the same file the sidecar reads, so the
     * film and its metadata cannot describe a stage differently.
     */
    private static void caption(ClientGameTestContext context, String id) {
        String[] text = CAPTIONS.get(id);
        check(text != null, "No caption is recorded for stage " + id);
        ClientGameTestRecorder.showStep(context, id, text[0], text[1]);
        context.waitTicks(CAPTION_LEAD_TICKS);
    }

    private static Map<String, String[]> loadCaptions() {
        String resource = "/telekinesis-captions.json";
        try (InputStream stream = TelekinesisClientGameTest.class.getResourceAsStream(resource)) {
            check(stream != null, "The recording captions are missing: " + resource);
            JsonObject root =
                    JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                            .getAsJsonObject();
            Map<String, String[]> captions = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                JsonObject text = entry.getValue().getAsJsonObject();
                captions.put(
                        entry.getKey(),
                        new String[] {
                            text.get("title").getAsString(), text.get("subtitle").getAsString()
                        });
            }
            return Map.copyOf(captions);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /** Waits by the clock, as the film runs, not by ticks, which run slower under load. */
    private static void holdFor(ClientGameTestContext context, long millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            context.waitTick();
        }
    }

    /**
     * Mines a block with the player's own attack key and waits for the server to have broken it:
     * the client removes the block before the server confirms, and the drops are only handed out on
     * the server.
     */
    private static void mine(
            ClientGameTestContext context, TestDedicatedServerContext server, BlockPos pos) {
        ClientGameTestPlayerActions.lookAtBlock(context, pos);
        ClientGameTestPlayerActions.mine(context, pos);
        waitForServer(
                context,
                server,
                minecraftServer -> player(minecraftServer).level().getBlockState(pos).isAir());
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
