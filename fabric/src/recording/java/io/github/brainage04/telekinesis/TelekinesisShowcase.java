package io.github.brainage04.telekinesis;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.brainage04.minecraftmodconventions.ClientGameTestPlayerActions;
import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import io.github.brainage04.minecraftmodconventions.ClientGameTestStages;
import io.github.brainage04.minecraftmodconventions.ClientGameTestWaits;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariants;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Blaze;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The film of Telekinesis' showcase: every scenario twice, first with Telekinesis off (or, for the
 * kill-credit scenarios, without the game's kill credit), then with it on.
 *
 * <p>Every take plays like a person: the player arrives in the take's bay already looking at the
 * block or entity it acts on, walks with the movement keys, mines and attacks with the held attack
 * key and uses with the use key, within vanilla reach. {@link ClientGameTestPlayerActions} keeps it
 * that way and fails the take when the crosshair is not on the target or the target is out of
 * reach.
 *
 * <p>Each scenario has two identical bays side by side, so the "off" and the "on" take start from
 * the same view. The blocks of every bay are built before the client joins, and so before the
 * recording starts. The take's mobs and decorations are spawned while the player is teleported into
 * its bay, before its caption, which is where the film is cut. Each take checks on the server that
 * the drops went where its caption says, so a take cannot show something its caption does not
 * claim.
 */
@SuppressWarnings("UnstableApiUsage")
public final class TelekinesisShowcase {
    private static final int FLOOR_Y = 63;
    private static final int STAND_Y = FLOOR_Y + 1;
    private static final int STAGE_MIN_Z = -16;
    private static final int STAGE_MAX_Z = 8;

    /** The distance between two bays, wider than a take's drops and blasts reach. */
    private static final int BAY_SPACING = 24;

    private static final int FIRST_BAY_X = -408;

    private static final int MINING = 0;
    private static final int CHEST = 1;
    private static final int TREE = 2;
    private static final int MOB = 3;
    private static final int LAVA = 4;
    private static final int GOLEM = 5;
    private static final int SNOW_GOLEM = 6;
    private static final int WOLF = 7;
    private static final int ITEM_FRAME = 8;
    private static final int ARMOUR_STAND = 9;
    private static final int PAINTING = 10;
    private static final int CUSHION = 11;
    private static final int CHEST_BOAT = 12;
    private static final int CHEST_CART = 13;
    private static final int TNT = 14;
    private static final int ANCHOR = 15;
    private static final int FULL_INVENTORY = 16;
    private static final int OVERWORLD_SCENARIOS = 17;

    private static final int STAGE_MIN_X = bayX(0, false) - 14;
    private static final int STAGE_MAX_X = bayX(OVERWORLD_SCENARIOS - 1, true) + 14;

    private static final double NEAR_Z = 4.5D;

    /**
     * Where a broken chest stands, and where the player stands to break it: far enough apart that
     * the stacks a chest spills stay outside the player's pickup range.
     */
    private static final int CHEST_Z = 1;

    private static final double CHEST_STAND_Z = 5.0D;

    /** Where a mob that walks up to the player starts: one and a half blocks in front of it. */
    private static final double ATTACKER_Z = 3.0D;

    /** How far the player backs off from the mob that hit it, out of reach of its drops. */
    private static final double BACK_OFF_DISTANCE = 4.0D;

    /** Vanilla gives a new item 10 ticks before a player may pick it up; wait past that. */
    private static final int PICKUP_DELAY_TICKS = 30;

    /** Two blocks from the oak's trunk: close enough to break it from where the take starts. */
    private static final double TREE_START_Z = 2.5D;

    /** How close the player stands to the TNT to light it: inside reach, back from the blast. */
    private static final double TNT_LIGHT_DISTANCE = 3.0D;

    /** How far the player steps back from the lit TNT, outside a power-4 blast's damage. */
    private static final double TNT_SAFE_DISTANCE = 7.0D;

    /**
     * Where the player stands to use the anchor and the Nether bed: the far edge of vanilla reach.
     */
    private static final double BLAST_STAND_Z = -1.0D;

    private static final int BLAST_BLOCK_Z = -6;

    /** The tick a lit TNT is given, so the player can step back before it blows. */
    private static final int TNT_FUSE_TICKS = 60;

    private static final int NETHER_FLOOR_Y = 200;
    private static final int NETHER_OFF_X = -16;
    private static final int NETHER_ON_X = 16;

    /** The longest oak the tree take can fell: its top log has to be inside vanilla reach. */
    private static final int MAX_TREE_LOGS = 7;

    /** The log after the bottom two: the player walks under the trunk as this one breaks. */
    private static final int LOG_BROKEN_WHILE_WALKING_IN = 2;

    /** Where the player stops walking in, under the trunk, in blocks along z. */
    private static final double TRUNK_STAND_Z = 0.8D;

    /** The most punches a breakable entity may take before the take fails. */
    private static final int MAX_PUNCHES = 12;

    /** The shortest gap a painting or a frame needs: a one-block stone wall behind it. */
    private static final int WALL_Z = 1;

    private static final int RENDER_DISTANCE = 12;
    private static final int NORMAL_FOV = 70;
    private static final int BLAST_FOV = 45;

    /** Ticks the resistance lasts that a real player's armour would have bought. */
    private static final int RESISTANCE_TICKS = 20 * 60;

    private static final int RESISTANCE_AMPLIFIER = 3;

    /**
     * Health that makes each companion take end in one blow: an iron golem's (at least 7.5,
     * IronGolem.java:189-195), a wolf's bite (4) and a snowball on a blaze (3,
     * Snowball.java:58-59). Not vanilla values.
     */
    private static final float GOLEM_VICTIM_HEALTH = 7.0F;

    private static final float GOLEM_VICTIM_AFTER_HIT_HEALTH = 10.0F;
    private static final float WOLF_VICTIM_HEALTH = 4.0F;
    private static final float BLAZE_HEALTH = 3.0F;

    /**
     * A lava victim's health: the "off" husk burns in about a second and a half, the "on" husk
     * outlives the lava until the player's hit and dies soon after it.
     */
    private static final float LAVA_UNHIT_HEALTH = 12.0F;

    private static final float LAVA_HIT_HEALTH = 24.0F;

    /** The loot seeds tried for a blaze that drops its rod. */
    private static final int ROD_SEED_ATTEMPTS = 64;

    private static final int LEAF_DECAY_TICK_SPEED = 300;
    private static final int SETTLE_TICKS = 15;
    private static final int CAPTION_LEAD_TICKS = 6;
    private static final int HOLD_AFTER_END_TICKS = 6;
    private static final int STEP_BACK_TICKS = 7;

    // Built on the server thread, not in a static field: an ItemStack reads its item's components
    // (ItemStack.java:248-250), which are bound when the server loads its registries
    // (DataComponentInitializers.java:103), and a built-in item holder throws "Components not bound
    // yet" until then (Holder.java:268).
    private static List<ItemStack> chestContents() {
        return List.of(
                new ItemStack(Items.DIAMOND, 3),
                new ItemStack(Items.EMERALD, 12),
                new ItemStack(Items.IRON_INGOT, 16),
                new ItemStack(Items.GOLD_INGOT, 5),
                new ItemStack(Items.GOLDEN_APPLE, 1));
    }

    private static final Map<String, String[]> CAPTIONS = loadCaptions();

    private static final int LOAD_TIMEOUT_TICKS = 2400;
    private static final int ACTION_TIMEOUT_TICKS = 200;

    /** Ticks a block may take to break before the take fails. */
    private static final int BREAK_TIMEOUT_TICKS = 300;

    private static final long BEFORE_MILLIS = 400L;
    private static final long AFTER_MILLIS = 600L;

    /** The pause after the caption before the TNT take's right click: about half a second in. */
    private static final long TNT_USE_DELAY_MILLIS = 400L;

    private TelekinesisShowcase() {}

    public static Properties serverProperties() {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();
        serverProperties.setProperty("view-distance", Integer.toString(RENDER_DISTANCE));
        return serverProperties;
    }

    /** Builds every bay before the client joins, so none of it is filmed. */
    public static void buildWorld(TestDedicatedServerContext server) {
        logged(connected -> connected.runOnServer(TelekinesisShowcase::buildStages)).accept(server);
    }

    public static void film(ClientGameTestContext context, TestDedicatedServerContext server) {
        logged(connected -> filmScenarios(context, connected)).accept(server);
    }

    private static void filmScenarios(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        boolean previousTelekinesis =
                server.computeOnServer(
                        minecraftServer -> {
                            boolean enabled = TelekinesisConfigManager.config().enabled();
                            TelekinesisConfigManager.setEnabled(true);
                            TelekinesisPlayerSettings.setEnabled(player(minecraftServer), true);
                            return enabled;
                        });
        context.runOnClient(
                client -> {
                    client.options.renderDistance().set(RENDER_DISTANCE);
                    // The server only sends chunks out to the render distance the client last told
                    // it.
                    client.options.broadcastOptions();
                    client.options.particles().set(ParticleStatus.DECREASED);
                    // A section that has just been built would fade in over a second; the film
                    // never shows that.
                    client.options.chunkSectionFadeInTime().set(0.0D);
                    // Without occlusion culling every section in range is drawn, so none blinks out
                    // when the player moves.
                    client.smartCull = false;
                    // A new item in the hand shows its name for a while; the shortest setting has
                    // it gone before each take.
                    client.options.notificationDisplayTime().set(0.5D);
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                });
        try {
            for (boolean on : new boolean[] {false, true}) {
                mining(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                chest(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                tree(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                mobKill(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                lavaKill(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                golemKill(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                snowGolemKill(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                wolfKill(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                itemFrame(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                armourStand(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                painting(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                cushion(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                chestBoat(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                chestMinecart(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                tnt(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                respawnAnchor(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                fullInventory(context, server, on);
            }
            for (boolean on : new boolean[] {false, true}) {
                netherBed(context, server, on);
            }
        } finally {
            server.runOnServer(
                    minecraftServer -> {
                        TelekinesisConfigManager.setEnabled(previousTelekinesis);
                        TelekinesisPlayerSettings.setEnabled(player(minecraftServer), true);
                    });
            context.runOnClient(
                    client -> {
                        client.options.setCameraType(CameraType.FIRST_PERSON);
                        client.options.fov().set(NORMAL_FOV);
                    });
        }
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

    /**
     * The first "off" take shows the player turning Telekinesis off with its command. Every later
     * take sets the preference while the player is teleported into its bay, and its caption says
     * whether it is on.
     */
    private static void mining(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(MINING, on);
        BlockPos ore = new BlockPos(siteX, STAND_Y, 2);
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                Vec3.atCenterOf(ore),
                true,
                player -> player.getInventory().setItem(0, pickaxe(player.level(), true)));
        ClientGameTestPlayerActions.requireLookingAtBlock(context, ore);
        String id = takeId("mining", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        if (!on) {
            context.runOnClient(
                    client -> client.player.connection.sendCommand("telekinesis disable"));
            ClientGameTestWaits.waitForServer(
                    context,
                    server,
                    minecraftServer ->
                            !TelekinesisPlayerSettings.isEffective(player(minecraftServer)),
                    ACTION_TIMEOUT_TICKS,
                    "/telekinesis disable turns the player's setting off");
            ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        }
        mine(context, server, ore);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    if (on) {
                        checkAtLeast(player, Items.DIAMOND, 1);
                        check(
                                player.totalExperience > 0,
                                "The ore's experience must go to the miner");
                        checkNoItemsAt(player.level(), siteX);
                    } else {
                        checkCount(player, Items.DIAMOND, 0);
                        check(
                                onGround(player.level(), siteX, Items.DIAMOND) > 0,
                                "The diamonds must drop at the ore");
                        checkOrbsAt(player.level(), siteX);
                    }
                });
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void chest(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(CHEST, on);
        BlockPos chest = new BlockPos(siteX, STAND_Y, CHEST_Z);
        prepare(
                context,
                server,
                siteX,
                CHEST_STAND_Z,
                NORMAL_FOV,
                Vec3.atCenterOf(chest),
                on,
                player -> player.getInventory().setItem(0, axe(player.level())));
        ClientGameTestPlayerActions.requireLookingAtBlock(context, chest);
        String id = takeId("chest", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        mine(context, server, chest);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    List<ItemStack> expected = new ArrayList<>(chestContents());
                    expected.add(new ItemStack(Items.CHEST));
                    checkDelivered(player, siteX, on, expected);
                });
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void tree(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(TREE, on);
        List<BlockPos> logs =
                server.computeOnServer(
                        minecraftServer -> {
                            ServerLevel level = minecraftServer.overworld();
                            List<BlockPos> found = new ArrayList<>();
                            BlockPos.MutableBlockPos cursor =
                                    new BlockPos.MutableBlockPos(siteX, STAND_Y, 0);
                            while (level.getBlockState(cursor).is(Blocks.OAK_LOG)) {
                                found.add(cursor.immutable());
                                cursor.move(Direction.UP);
                            }
                            return List.copyOf(found);
                        });
        check(logs.size() >= 4, "The tree take needs an oak of at least four logs, got " + logs);
        check(
                logs.size() <= MAX_TREE_LOGS,
                "The tree take's oak must be at most "
                        + MAX_TREE_LOGS
                        + " logs tall so its top log is inside vanilla reach, got "
                        + logs.size()
                        + ": "
                        + logs);
        prepare(
                context,
                server,
                siteX,
                TREE_START_Z,
                NORMAL_FOV,
                Vec3.atCenterOf(logs.getFirst()),
                on,
                player -> player.getInventory().setItem(0, axe(player.level())));
        ClientGameTestPlayerActions.requireLookingAtBlock(context, logs.getFirst());
        String id = takeId("tree", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        if (!on) {
            breakBottomLogs(context, logs);
            ClientGameTestWaits.waitForServer(
                    context,
                    server,
                    minecraftServer ->
                            onGround(minecraftServer.overworld(), siteX, Items.OAK_LOG) == 2,
                    ACTION_TIMEOUT_TICKS,
                    "the two bottom logs lie at the trunk");
            server.runOnServer(
                    minecraftServer -> checkCount(player(minecraftServer), Items.OAK_LOG, 0));
            ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
            ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
            return;
        }
        fellTree(context, logs);
        server.runOnServer(
                minecraftServer ->
                        command(
                                minecraftServer,
                                "gamerule random_tick_speed " + LEAF_DECAY_TICK_SPEED));
        AABB canopy = new AABB(siteX - 4, STAND_Y, -4, siteX + 5, STAND_Y + 10, 5);
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
                    checkNoItemsAt(player.level(), siteX);
                });
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    /** Breaks the two logs a player reaches from where the take starts, holding the attack key. */
    private static void breakBottomLogs(ClientGameTestContext context, List<BlockPos> logs) {
        context.getInput().holdKey(options -> options.keyAttack);
        try {
            for (int index = 0; index < LOG_BROKEN_WHILE_WALKING_IN; index++) {
                BlockPos log = logs.get(index);
                if (index > 0) {
                    ClientGameTestPlayerActions.lookAtBlock(context, log);
                }
                ClientGameTestPlayerActions.requireLookingAtBlock(context, log);
                context.waitFor(
                        client -> client.level.getBlockState(log).isAir(), BREAK_TIMEOUT_TICKS);
            }
        } finally {
            context.getInput().releaseKey(options -> options.keyAttack);
        }
    }

    /**
     * Holds the attack key through the whole felling, as a player does. The bottom two logs break
     * from where the take starts, the third while the player walks in under the trunk. The player
     * then looks straight up once: standing inside the trunk's column, its crosshair stays on the
     * lowest log left, so the camera holds still while the rest of the trunk comes down.
     */
    private static void fellTree(ClientGameTestContext context, List<BlockPos> logs) {
        context.getInput().holdKey(options -> options.keyAttack);
        try {
            for (int index = 0; index < logs.size(); index++) {
                BlockPos log = logs.get(index);
                if (index == LOG_BROKEN_WHILE_WALKING_IN) {
                    walkUnderTheTrunk(context, log);
                    ClientGameTestPlayerActions.lookStraightUp(context);
                    continue;
                }
                if (index > 0 && index < LOG_BROKEN_WHILE_WALKING_IN) {
                    ClientGameTestPlayerActions.lookAtBlock(context, log);
                }
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

    private static void mobKill(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(MOB, on);
        Vec3 huskAt = new Vec3(siteX + 0.5D, STAND_Y, 2.0D);
        AtomicInteger huskId = new AtomicInteger();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                huskAt.add(0.0D, 1.1D, 0.0D),
                on,
                player -> {
                    huskId.set(spawnHusk(player.level(), huskAt, 1.0F).getId());
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                });
        waitForClientEntity(context, huskId.get());
        ClientGameTestPlayerActions.requireLookingAtEntity(context, huskId.get());
        String id = takeId("mob", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.attackEntity(context, huskId.get());
        waitForDeath(context, server, huskId.get());
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    checkDelivered(player, siteX, on, List.of(new ItemStack(Items.IRON_SWORD)));
                    checkExperience(player, siteX, on);
                });
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    /**
     * Lava kills a husk in its basin. Without the player's hit the game credits nobody, so the loot
     * stays where it fell, and the game drops no experience (LivingEntity.java:1529-1536). With the
     * player's hit inside the game's 100 ticks the game credits the player
     * (LivingEntity.java:1984-1987), so the loot is the player's. Telekinesis is on in both takes.
     * The husk holds a netherite sword because a fire-resistant item takes no damage in lava
     * (Items.java:1684, ItemEntity.java:296), so the "off" take's sword stays visible in the basin.
     */
    private static void lavaKill(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean hit) {
        int siteX = bayX(LAVA, hit);
        Vec3 huskAt = new Vec3(siteX + 0.5D, STAND_Y, 2.5D);
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                huskAt.add(0.0D, 1.1D, 0.0D),
                true,
                player -> player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD)));
        // The lava burns the husk from the spawn, and the off take's husk dies in about 1.5 s
        // of it, so the husk is fire-immune until the take starts: a slow host's client then
        // never shows the take a husk the lava has already killed.
        AtomicReference<Husk> husk = new AtomicReference<>();
        server.runOnServer(
                minecraftServer -> {
                    Husk spawned =
                            spawnHusk(
                                    minecraftServer.overworld(),
                                    huskAt,
                                    Items.NETHERITE_SWORD,
                                    hit ? LAVA_HIT_HEALTH : LAVA_UNHIT_HEALTH);
                    spawned.addEffect(
                            new MobEffectInstance(
                                    MobEffects.FIRE_RESISTANCE,
                                    LOAD_TIMEOUT_TICKS,
                                    0,
                                    false,
                                    false));
                    husk.set(spawned);
                });
        int huskId = server.computeOnServer(minecraftServer -> husk.get().getId());
        waitForClientEntity(context, huskId);
        ClientGameTestPlayerActions.requireLookingAtEntity(context, huskId);
        String id = takeId("lava", hit);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        // The burn begins with the take, so every host films the same one.
        server.runOnServer(minecraftServer -> husk.get().removeEffect(MobEffects.FIRE_RESISTANCE));
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        if (hit) {
            ClientGameTestPlayerActions.attackEntity(context, huskId);
            ClientGameTestWaits.waitForServer(
                    context,
                    server,
                    minecraftServer -> husk.get().getLastHurtByPlayer() != null,
                    ACTION_TIMEOUT_TICKS,
                    "the player's hit lands on the husk");
        }
        waitForDeath(context, server, huskId);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    if (hit) {
                        check(
                                husk.get().getKillCredit() == player,
                                "The game must credit the player's hit");
                        checkCount(player, Items.NETHERITE_SWORD, 1);
                        check(
                                player.totalExperience > 0,
                                "A kill the game credits after the player's hit pays its"
                                        + " experience");
                        checkNoItemsAt(player.level(), siteX);
                    } else {
                        check(
                                husk.get().getKillCredit() == null,
                                "The game must credit nobody with the lava's kill");
                        checkCount(player, Items.NETHERITE_SWORD, 0);
                        check(
                                onGround(player.level(), siteX, Items.NETHERITE_SWORD) == 1,
                                "The husk's sword must stay where it fell");
                        check(
                                orbsAt(player.level(), siteX) == 0,
                                "A kill nobody is credited with drops no experience");
                    }
                });
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    /**
     * An iron golem's blow kills the husk. Without the player's hit the game credits the golem, as
     * when the husk hit the player first (a mob's hit on a player gives the player no credit,
     * Mob.java:1395), so the loot stays and no experience drops. With the player's hit first the
     * game credits the player, and the loot and experience are the player's.
     */
    private static void golemKill(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean hit) {
        int siteX = bayX(GOLEM, hit);
        Vec3 huskAt = new Vec3(siteX + 0.5D, STAND_Y, hit ? 2.5D : ATTACKER_Z);
        Vec3 golemAt =
                hit
                        ? new Vec3(siteX + 2.5D, STAND_Y, 1.5D)
                        : new Vec3(siteX + 4.5D, STAND_Y, ATTACKER_Z);
        AtomicReference<Husk> husk = new AtomicReference<>();
        AtomicReference<IronGolem> golem = new AtomicReference<>();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                huskAt.add(0.0D, 1.1D, 0.0D),
                true,
                player -> {
                    ServerLevel level = player.level();
                    husk.set(
                            spawnHusk(
                                    level,
                                    huskAt,
                                    hit ? GOLEM_VICTIM_AFTER_HIT_HEALTH : GOLEM_VICTIM_HEALTH));
                    golem.set(spawnGolem(level, golemAt));
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                    giveResistance(player);
                });
        int huskId = server.computeOnServer(minecraftServer -> husk.get().getId());
        waitForClientEntity(context, huskId);
        waitForClientEntity(
                context, server.computeOnServer(minecraftServer -> golem.get().getId()));
        ClientGameTestPlayerActions.requireLookingAtEntity(context, huskId);
        String id = takeId("golem", hit);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        if (hit) {
            ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
            ClientGameTestPlayerActions.attackEntity(context, huskId);
            wake(server, golem.get());
        } else {
            wake(server, husk.get(), golem.get());
            awaitHitAndBackOff(context, server, husk);
        }
        waitForDeath(context, server, huskId);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    if (hit) {
                        check(
                                husk.get().getKillCredit() == player,
                                "The game must credit the player's hit, not the golem's blow");
                        checkCount(player, Items.IRON_SWORD, 1);
                        check(
                                player.totalExperience > 0,
                                "The golem's kill after the player's hit pays its experience");
                        checkNoItemsAt(player.level(), siteX);
                    } else {
                        check(
                                husk.get().getKillCredit() == golem.get(),
                                "The game must credit the golem");
                        checkCount(player, Items.IRON_SWORD, 0);
                        check(
                                onGround(player.level(), siteX, Items.IRON_SWORD) == 1,
                                "The husk's sword must stay where it fell");
                        check(
                                orbsAt(player.level(), siteX) == 0,
                                "A kill the game credits to the golem drops no experience");
                    }
                });
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    /**
     * A snow golem's snowball finishes a blaze. Without the player's punch the game credits the
     * snow golem, and the blaze's rod needs a player kill (VanillaEntityLoot.java:58-68), so
     * nothing drops. With the punch first the game credits the player.
     */
    private static void snowGolemKill(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean hit) {
        int siteX = bayX(SNOW_GOLEM, hit);
        Vec3 blazeAt = new Vec3(siteX + 0.5D, STAND_Y, 2.5D);
        Vec3 snowGolemAt = new Vec3(siteX + 4.5D, STAND_Y, 2.5D);
        AtomicReference<Blaze> blaze = new AtomicReference<>();
        AtomicReference<SnowGolem> snowGolem = new AtomicReference<>();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                blazeAt.add(0.0D, 0.9D, 0.0D),
                true,
                player -> {
                    ServerLevel level = player.level();
                    SnowGolem thrower =
                            EntityTypes.SNOW_GOLEM.create(level, EntitySpawnReason.COMMAND);
                    check(thrower != null, "The snow golem take needs its snow golem");
                    thrower.setNoAi(true);
                    thrower.setPersistenceRequired();
                    thrower.snapTo(snowGolemAt.x, snowGolemAt.y, snowGolemAt.z, 90.0F, 0.0F);
                    check(level.addFreshEntity(thrower), "The snow golem must spawn");
                    snowGolem.set(thrower);
                    blaze.set(spawnBlaze(level, player, thrower, blazeAt, hit));
                });
        int blazeId = server.computeOnServer(minecraftServer -> blaze.get().getId());
        waitForClientEntity(context, blazeId);
        emptyHand(context);
        ClientGameTestPlayerActions.requireLookingAtEntity(context, blazeId);
        String id = takeId("snow_golem", hit);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        if (hit) {
            ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
            ClientGameTestPlayerActions.attackEntity(context, blazeId);
        }
        wake(server, snowGolem.get());
        waitForDeath(context, server, blazeId);
        context.waitTicks(PICKUP_DELAY_TICKS);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    if (hit) {
                        check(
                                blaze.get().getKillCredit() == player,
                                "The game must credit the player's punch");
                        checkCount(player, Items.BLAZE_ROD, 1);
                        check(
                                player.totalExperience > 0,
                                "The blaze's experience must go to the player");
                        checkNoItemsAt(player.level(), siteX);
                    } else {
                        check(
                                blaze.get().getKillCredit() == snowGolem.get(),
                                "The game must credit the snow golem");
                        checkNoItemsAt(player.level(), siteX);
                        check(
                                orbsAt(player.level(), siteX) == 0,
                                "A kill the game credits to the snow golem drops no experience");
                        check(player.totalExperience == 0, "The player gets no experience");
                    }
                });
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    /**
     * The husk hits the player, and the player's tamed wolf, which takes on whatever last hurt its
     * owner (OwnerHurtByTargetGoal.java:23-35), kills it. The game credits a tamed wolf's kill to
     * its owner (LivingEntity.java:1369-1371), so Telekinesis decides where the loot goes.
     */
    private static void wolfKill(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(WOLF, on);
        Vec3 huskAt = new Vec3(siteX + 0.5D, STAND_Y, ATTACKER_Z);
        Vec3 wolfAt = new Vec3(siteX + 1.5D, STAND_Y, NEAR_Z);
        AtomicReference<Husk> husk = new AtomicReference<>();
        AtomicReference<Wolf> wolf = new AtomicReference<>();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                huskAt.add(0.0D, 1.1D, 0.0D),
                on,
                player -> {
                    ServerLevel level = player.level();
                    husk.set(spawnHusk(level, huskAt, WOLF_VICTIM_HEALTH));
                    Wolf pet = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
                    check(pet != null, "The wolf take needs its wolf");
                    pet.tame(player);
                    // OwnerHurtByTargetGoal.java:24: a sitting wolf does not defend its owner.
                    pet.setOrderedToSit(false);
                    pet.setInSittingPose(false);
                    pet.setNoAi(true);
                    pet.setPersistenceRequired();
                    pet.snapTo(wolfAt.x, wolfAt.y, wolfAt.z, 180.0F, 0.0F);
                    check(level.addFreshEntity(pet), "The wolf must spawn");
                    wolf.set(pet);
                    giveResistance(player);
                });
        int huskId = server.computeOnServer(minecraftServer -> husk.get().getId());
        waitForClientEntity(context, huskId);
        waitForClientEntity(context, server.computeOnServer(minecraftServer -> wolf.get().getId()));
        ClientGameTestPlayerActions.requireLookingAtEntity(context, huskId);
        String id = takeId("wolf", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        wake(server, husk.get(), wolf.get());
        awaitHitAndBackOff(context, server, husk);
        waitForDeath(context, server, huskId);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    check(
                            husk.get().getKillCredit() == player,
                            "The game must credit the wolf's kill to its owner");
                    checkDelivered(player, siteX, on, List.of(new ItemStack(Items.IRON_SWORD)));
                    checkExperience(player, siteX, on);
                });
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    /**
     * Waits for the husk's hit to land on the player, then backs the player off, as a player would,
     * so the drops of the husk's death land out of the player's pickup range.
     */
    private static void awaitHitAndBackOff(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            AtomicReference<Husk> husk) {
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer -> {
                    check(
                            !husk.get().isDeadOrDying(),
                            "The husk died before its hit landed on the player");
                    return player(minecraftServer).getLastHurtByMob() == husk.get();
                },
                ACTION_TIMEOUT_TICKS,
                "the husk's hit lands on the player");
        Vec3 hitFrom = server.computeOnServer(minecraftServer -> husk.get().position());
        ClientGameTestPlayerActions.walkAwayFrom(context, hitFrom, BACK_OFF_DISTANCE);
    }

    private static void itemFrame(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(ITEM_FRAME, on);
        BlockPos frameAt = new BlockPos(siteX, STAND_Y + 1, WALL_Z).south();
        Vec3 frameLook = new Vec3(siteX + 0.5D, STAND_Y + 1.5D, frameAt.getZ() + 0.03D);
        AtomicInteger frameId = new AtomicInteger();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                frameLook,
                on,
                player -> {
                    ItemFrame frame = new ItemFrame(player.level(), frameAt, Direction.SOUTH);
                    frame.setItem(new ItemStack(Items.DIAMOND));
                    check(
                            player.level().addFreshEntity(frame),
                            "The frame take needs its item frame");
                    frameId.set(frame.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                });
        waitForClientEntity(context, frameId.get());
        ClientGameTestPlayerActions.requireLookingAtEntity(context, frameId.get());
        String id = takeId("item_frame", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, frameId.get());
        server.runOnServer(
                minecraftServer ->
                        checkDelivered(
                                player(minecraftServer),
                                siteX,
                                on,
                                List.of(
                                        new ItemStack(Items.DIAMOND),
                                        new ItemStack(Items.ITEM_FRAME))));
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void armourStand(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(ARMOUR_STAND, on);
        Vec3 standAt = new Vec3(siteX + 0.5D, STAND_Y, 2.5D);
        AtomicInteger standId = new AtomicInteger();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                standAt.add(0.0D, 1.0D, 0.0D),
                on,
                player -> {
                    ServerLevel level = player.level();
                    ArmorStand stand = new ArmorStand(level, standAt.x, standAt.y, standAt.z);
                    stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                    check(level.addFreshEntity(stand), "The stand take needs its armour stand");
                    standId.set(stand.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                });
        waitForClientEntity(context, standId.get());
        ClientGameTestPlayerActions.requireLookingAtEntity(context, standId.get());
        String id = takeId("armour_stand", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        // An armour stand breaks on the second punch, which has to land within five ticks of the
        // first (ArmorStand.java:330), so a player double-clicks it.
        context.getInput().pressKey(options -> options.keyAttack);
        context.waitTicks(2);
        ClientGameTestPlayerActions.requireLookingAtEntity(context, standId.get());
        context.getInput().pressKey(options -> options.keyAttack);
        context.waitFor(
                client -> client.level.getEntity(standId.get()) == null, ACTION_TIMEOUT_TICKS);
        server.runOnServer(
                minecraftServer ->
                        checkDelivered(
                                player(minecraftServer),
                                siteX,
                                on,
                                List.of(
                                        new ItemStack(Items.ARMOR_STAND),
                                        new ItemStack(Items.IRON_HELMET),
                                        new ItemStack(Items.DIAMOND_SWORD))));
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void painting(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(PAINTING, on);
        BlockPos paintingAt = new BlockPos(siteX, STAND_Y + 1, WALL_Z).south();
        Vec3 paintingLook = new Vec3(siteX + 0.5D, STAND_Y + 1.5D, paintingAt.getZ() + 0.03D);
        AtomicInteger paintingId = new AtomicInteger();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                paintingLook,
                on,
                player -> {
                    ServerLevel level = player.level();
                    Painting picture =
                            new Painting(
                                    level,
                                    paintingAt,
                                    Direction.SOUTH,
                                    level.registryAccess()
                                            .lookupOrThrow(Registries.PAINTING_VARIANT)
                                            .getOrThrow(PaintingVariants.KEBAB));
                    check(level.addFreshEntity(picture), "The painting take needs its painting");
                    paintingId.set(picture.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                });
        waitForClientEntity(context, paintingId.get());
        ClientGameTestPlayerActions.requireLookingAtEntity(context, paintingId.get());
        String id = takeId("painting", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, paintingId.get());
        server.runOnServer(
                minecraftServer ->
                        checkDelivered(
                                player(minecraftServer),
                                siteX,
                                on,
                                List.of(new ItemStack(Items.PAINTING))));
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void cushion(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(CUSHION, on);
        Vec3 cushionAt = new Vec3(siteX + 0.5D, STAND_Y, 2.5D);
        AtomicInteger cushionId = new AtomicInteger();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                cushionAt.add(0.0D, 0.25D, 0.0D),
                on,
                player -> {
                    ServerLevel level = player.level();
                    Cushion cushion = EntityTypes.CUSHION.create(level, EntitySpawnReason.COMMAND);
                    check(cushion != null, "The cushion take needs its cushion");
                    cushion.setColor(DyeColor.LIGHT_BLUE);
                    cushion.snapTo(cushionAt.x, cushionAt.y, cushionAt.z, 0.0F, 0.0F);
                    check(level.addFreshEntity(cushion), "The cushion must spawn");
                    cushionId.set(cushion.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                });
        waitForClientEntity(context, cushionId.get());
        ClientGameTestPlayerActions.requireLookingAtEntity(context, cushionId.get());
        String id = takeId("cushion", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, cushionId.get());
        server.runOnServer(
                minecraftServer ->
                        checkDelivered(
                                player(minecraftServer),
                                siteX,
                                on,
                                List.of(new ItemStack(Items.CUSHION.pick(DyeColor.LIGHT_BLUE)))));
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void chestBoat(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(CHEST_BOAT, on);
        Vec3 boatAt = new Vec3(siteX + 0.5D, STAND_Y, 2.5D);
        AtomicInteger boatId = new AtomicInteger();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                boatAt.add(0.0D, 0.3D, 0.0D),
                on,
                player -> {
                    ServerLevel level = player.level();
                    ChestBoat boat =
                            EntityTypes.OAK_CHEST_BOAT.create(level, EntitySpawnReason.COMMAND);
                    check(boat != null, "The boat take needs its chest boat");
                    boat.snapTo(boatAt.x, boatAt.y, boatAt.z, 0.0F, 0.0F);
                    boat.setItem(0, new ItemStack(Items.EMERALD, 3));
                    check(level.addFreshEntity(boat), "The chest boat must spawn");
                    boatId.set(boat.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                });
        waitForClientEntity(context, boatId.get());
        ClientGameTestPlayerActions.requireLookingAtEntity(context, boatId.get());
        String id = takeId("chest_boat", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, boatId.get());
        server.runOnServer(
                minecraftServer ->
                        checkDelivered(
                                player(minecraftServer),
                                siteX,
                                on,
                                List.of(
                                        new ItemStack(Items.OAK_CHEST_BOAT),
                                        new ItemStack(Items.EMERALD, 3))));
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void chestMinecart(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(CHEST_CART, on);
        Vec3 cartAt = new Vec3(siteX + 0.5D, STAND_Y, 2.5D);
        AtomicInteger cartId = new AtomicInteger();
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                cartAt.add(0.0D, 0.35D, 0.0D),
                on,
                player -> {
                    ServerLevel level = player.level();
                    MinecartChest cart =
                            EntityTypes.CHEST_MINECART.create(level, EntitySpawnReason.COMMAND);
                    check(cart != null, "The cart take needs its chest minecart");
                    cart.snapTo(cartAt.x, cartAt.y, cartAt.z, 0.0F, 0.0F);
                    cart.setItem(0, new ItemStack(Items.EMERALD, 3));
                    check(level.addFreshEntity(cart), "The chest minecart must spawn");
                    cartId.set(cart.getId());
                    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                });
        waitForClientEntity(context, cartId.get());
        ClientGameTestPlayerActions.requireLookingAtEntity(context, cartId.get());
        String id = takeId("chest_cart", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        punchUntilGone(context, cartId.get());
        server.runOnServer(
                minecraftServer ->
                        checkDelivered(
                                player(minecraftServer),
                                siteX,
                                on,
                                List.of(
                                        new ItemStack(Items.CHEST_MINECART),
                                        new ItemStack(Items.EMERALD, 3))));
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void tnt(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(TNT, on);
        BlockPos tnt = new BlockPos(siteX, STAND_Y, BLAST_BLOCK_Z);
        BlockPos chest = tnt.east();
        Vec3 tntAt = Vec3.atCenterOf(tnt);
        prepare(
                context,
                server,
                siteX,
                tnt.getZ() + TNT_LIGHT_DISTANCE,
                BLAST_FOV,
                tntAt,
                on,
                player -> player.getInventory().setItem(0, new ItemStack(Items.FLINT_AND_STEEL)));
        ClientGameTestPlayerActions.requireLookingAtBlock(context, tnt);
        String id = takeId("tnt", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, TNT_USE_DELAY_MILLIS);
        ClientGameTestPlayerActions.useOnBlock(context, tnt);
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer -> {
                    List<PrimedTnt> primed =
                            minecraftServer
                                    .overworld()
                                    .getEntitiesOfClass(PrimedTnt.class, siteBox(siteX));
                    primed.forEach(
                            entity -> entity.setFuse(Math.min(entity.getFuse(), TNT_FUSE_TICKS)));
                    return !primed.isEmpty();
                },
                ACTION_TIMEOUT_TICKS,
                "the player's flint and steel primes the TNT");
        // Back away from the blast while the fuse burns down.
        ClientGameTestPlayerActions.walkAwayFrom(context, tntAt, TNT_SAFE_DISTANCE);
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer ->
                        minecraftServer.overworld().getBlockState(chest).isAir()
                                && minecraftServer
                                        .overworld()
                                        .getEntitiesOfClass(PrimedTnt.class, siteBox(siteX))
                                        .isEmpty(),
                ACTION_TIMEOUT_TICKS,
                "the blast destroys the chest and no TNT is left");
        server.runOnServer(minecraftServer -> checkBlastDrops(player(minecraftServer), siteX, on));
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void respawnAnchor(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(ANCHOR, on);
        BlockPos anchor = new BlockPos(siteX, STAND_Y, BLAST_BLOCK_Z);
        BlockPos chest = anchor.east(2);
        prepare(
                context,
                server,
                siteX,
                BLAST_STAND_Z,
                BLAST_FOV,
                Vec3.atCenterOf(anchor),
                on,
                TelekinesisShowcase::giveResistance);
        emptyHand(context);
        ClientGameTestPlayerActions.requireLookingAtBlock(context, anchor);
        String id = takeId("respawn_anchor", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.useOnBlock(context, anchor);
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer -> minecraftServer.overworld().getBlockState(chest).isAir(),
                ACTION_TIMEOUT_TICKS,
                "the anchor's blast destroys the chest");
        server.runOnServer(minecraftServer -> checkBlastDrops(player(minecraftServer), siteX, on));
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void fullInventory(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = bayX(FULL_INVENTORY, on);
        BlockPos ore = new BlockPos(siteX, STAND_Y, 2);
        prepare(
                context,
                server,
                siteX,
                NEAR_Z,
                NORMAL_FOV,
                Vec3.atCenterOf(ore),
                on,
                player -> {
                    player.getInventory().setItem(0, pickaxe(player.level(), false));
                    for (int slot = 1; slot < 36; slot++) {
                        player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                    }
                });
        ClientGameTestPlayerActions.requireLookingAtBlock(context, ore);
        String id = takeId("full_inventory", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        mine(context, server, ore);
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer -> onGround(minecraftServer.overworld(), siteX, Items.DIAMOND) > 0,
                ACTION_TIMEOUT_TICKS,
                "the diamond that does not fit lands on the ground");
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    List<ItemEntity> items =
                            player.level().getEntitiesOfClass(ItemEntity.class, siteBox(siteX));
                    Vec3 dropsAt = on ? player.position() : Vec3.atCenterOf(ore);
                    check(
                            items.size() == 1
                                    && items.getFirst().getItem().is(Items.DIAMOND)
                                    && items.getFirst().position().distanceTo(dropsAt) < 1.0D,
                            (on
                                            ? "The diamond that does not fit must lie at the"
                                                    + " player's feet: "
                                            : "The diamond must drop at the ore: ")
                                    + items);
                });
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        if (on) {
            context.getInput().holdKeyFor(options -> options.keyDown, STEP_BACK_TICKS);
        }
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void netherBed(
            ClientGameTestContext context, TestDedicatedServerContext server, boolean on) {
        int siteX = on ? NETHER_ON_X : NETHER_OFF_X;
        BlockPos bedFoot = netherBedFoot(siteX);
        BlockPos chest = bedFoot.north().east(2);
        prepare(
                context,
                server,
                Level.NETHER,
                new Vec3(siteX + 0.5D, NETHER_FLOOR_Y + 1.0D, BLAST_STAND_Z),
                BLAST_FOV,
                Vec3.atCenterOf(bedFoot),
                on,
                TelekinesisShowcase::giveResistance);
        emptyHand(context);
        ClientGameTestPlayerActions.requireLookingAtBlock(context, bedFoot);
        String id = takeId("nether_bed", on);
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.useOnBlock(context, bedFoot);
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer ->
                        minecraftServer.getLevel(Level.NETHER).getBlockState(chest).isAir(),
                ACTION_TIMEOUT_TICKS,
                "the bed's blast destroys the Nether chest");
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    List<ItemEntity> items =
                            player.level().getEntitiesOfClass(ItemEntity.class, siteBox(siteX));
                    if (on) {
                        checkCount(player, Items.EMERALD, 64);
                        check(items.isEmpty(), "No drop of the bed's blast may stay in the world");
                    } else {
                        checkCount(player, Items.EMERALD, 0);
                        check(
                                items.stream().anyMatch(item -> item.getItem().is(Items.EMERALD)),
                                "The chest's emeralds must scatter in the world");
                    }
                });
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static void prepare(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            int siteX,
            double standZ,
            int fov,
            Vec3 firstLook,
            boolean telekinesis,
            Consumer<ServerPlayer> setUp) {
        prepare(
                context,
                server,
                Level.OVERWORLD,
                new Vec3(siteX + 0.5D, STAND_Y, standZ),
                fov,
                firstLook,
                telekinesis,
                setUp);
    }

    /**
     * Places the player in the take's bay already looking at the take's first target, so the take
     * opens on it and no camera turn follows the teleport. A player who already faces that way from
     * the previous take keeps its rotation. The preference is set and the take's entities are
     * spawned here, before the caption, which is where the film is cut.
     */
    private static void prepare(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            ResourceKey<Level> dimension,
            Vec3 stand,
            int fov,
            Vec3 firstLook,
            boolean telekinesis,
            Consumer<ServerPlayer> setUp) {
        ItemStack[] hand = new ItemStack[1];
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    ServerLevel level = minecraftServer.getLevel(dimension);
                    check(level != null, "The test server must have " + dimension);
                    resetPlayer(player);
                    AABB site = siteBox(Mth.floor(stand.x));
                    level.getEntitiesOfClass(ItemEntity.class, site).forEach(Entity::discard);
                    level.getEntitiesOfClass(ExperienceOrb.class, site).forEach(Entity::discard);
                    // A companion left from an earlier take would wander into this one.
                    level.getEntitiesOfClass(Mob.class, stripBox(level)).forEach(Entity::discard);
                    clearFire(level, Mth.floor(stand.x));
                    TelekinesisPlayerSettings.setEnabled(player, telekinesis);
                    Vec3 eye = stand.add(0.0D, player.getEyeHeight(), 0.0D);
                    player.teleportTo(
                            level,
                            stand.x,
                            stand.y,
                            stand.z,
                            Set.of(),
                            yawTowards(eye, firstLook),
                            pitchTowards(eye, firstLook),
                            true);
                    setUp.accept(player);
                    hand[0] = player.getInventory().getItem(0).copy();
                });
        context.waitFor(
                client ->
                        client.level != null
                                && client.level.dimension() == dimension
                                && client.gui.screen() == null
                                && client.player.position().distanceTo(stand) < 0.01D
                                && ItemStack.isSameItem(
                                        client.player.getInventory().getItem(0), hand[0])
                                && client.player.totalExperience == 0,
                LOAD_TIMEOUT_TICKS);
        context.runOnClient(
                client -> {
                    client.options.fov().set(fov);
                    client.gui.hud.getChat().clearMessages(false);
                    // The server does not send the selected slot; the take's tool is in slot 0.
                    client.player.getInventory().setSelectedSlot(0);
                });
        ClientGameTestWaits.waitForChunkWindow(context, RENDER_DISTANCE - 2, LOAD_TIMEOUT_TICKS);
        ClientGameTestWaits.waitForRenderSettled(context, SETTLE_TICKS, LOAD_TIMEOUT_TICKS);
    }

    /**
     * The yaw and pitch {@code Entity.lookAt} gives a player whose eyes are at {@code eye}
     * (Entity.java:3769-3776), worked out before the teleport so that the teleport itself carries
     * them: {@code ServerPlayer.lookAt} sends a second packet the client could draw a frame before.
     */
    private static float yawTowards(Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        return Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
    }

    private static float pitchTowards(Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        return Mth.wrapDegrees(
                (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG));
    }

    /**
     * Punches the entity under the crosshair until it is gone. The player is already aimed, so it
     * presses again as soon as its attack strength is full, which takes the held item's attack
     * delay (Player.java:1799-1801).
     */
    private static void punchUntilGone(ClientGameTestContext context, int entityId) {
        int attackDelayTicks =
                context.computeOnClient(
                        client -> Mth.ceil(client.player.getCurrentItemAttackStrengthDelay()));
        context.waitFor(
                client -> client.player.getAttackStrengthScale(0.0F) >= 1.0F, ACTION_TIMEOUT_TICKS);
        for (int punch = 0; punch < MAX_PUNCHES; punch++) {
            ClientGameTestPlayerActions.requireLookingAtEntity(context, entityId);
            context.getInput().pressKey(options -> options.keyAttack);
            context.waitTicks(attackDelayTicks);
            if (context.computeOnClient(client -> client.level.getEntity(entityId) == null)) {
                return;
            }
        }
        throw new AssertionError("Entity " + entityId + " survived " + MAX_PUNCHES + " punches");
    }

    private static Husk spawnHusk(ServerLevel level, Vec3 at, float health) {
        return spawnHusk(level, at, Items.IRON_SWORD, health);
    }

    /**
     * Spawns a take's husk where the camera can see it, holding the sword that is its loot. It has
     * no AI until {@link #wake} gives it its AI, so it stands still until the take starts.
     */
    private static Husk spawnHusk(ServerLevel level, Vec3 at, Item sword, float health) {
        Husk husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
        check(husk != null, "The take needs its husk");
        husk.setNoAi(true);
        husk.setPersistenceRequired();
        husk.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        husk.setYHeadRot(0.0F);
        husk.setYBodyRot(0.0F);
        husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(sword));
        husk.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        AttributeInstance maxHealth = husk.getAttribute(Attributes.MAX_HEALTH);
        check(maxHealth != null, "The husk needs its max-health attribute");
        maxHealth.setBaseValue(Math.max(health, 20.0F));
        husk.setHealth(health);
        check(level.addFreshEntity(husk), "The husk must spawn");
        return husk;
    }

    /** A player-built golem, which never turns on the player (IronGolem.java:137-139). */
    private static IronGolem spawnGolem(ServerLevel level, Vec3 at) {
        IronGolem golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND);
        check(golem != null, "The golem take needs its golem");
        golem.setPlayerCreated(true);
        golem.setNoAi(true);
        golem.setPersistenceRequired();
        golem.snapTo(at.x, at.y, at.z, 90.0F, 0.0F);
        check(level.addFreshEntity(golem), "The golem must spawn");
        return golem;
    }

    /**
     * A blaze without AI and with three health. For the take where the game credits the player, its
     * death loot gets a seed that rolls a rod: the rod's count is a 0 to 1 roll
     * (VanillaEntityLoot.java:58-68), and the film shows one.
     */
    private static Blaze spawnBlaze(
            ServerLevel level, ServerPlayer player, SnowGolem thrower, Vec3 at, boolean hit) {
        String seed = hit ? ",DeathLootTableSeed:" + rodSeed(level, player, thrower) + "L" : "";
        command(
                level.getServer(),
                "summon minecraft:blaze "
                        + at.x
                        + " "
                        + at.y
                        + " "
                        + at.z
                        + " {NoAI:1b,PersistenceRequired:1b,Health:"
                        + BLAZE_HEALTH
                        + "f"
                        + seed
                        + "}");
        List<Blaze> blazes =
                level.getEntitiesOfClass(Blaze.class, new AABB(BlockPos.containing(at)));
        check(blazes.size() == 1, "The snow golem take needs one blaze, found " + blazes);
        return blazes.getFirst();
    }

    /**
     * The first loot seed whose roll gives the blaze a rod, read from the blaze's own loot table
     * with the parameters {@code LivingEntity.dropFromLootTable} passes for a kill the game credits
     * to a player, with the snow golem as the attacker (LivingEntity.java:1572-1583).
     */
    private static long rodSeed(ServerLevel level, ServerPlayer player, SnowGolem thrower) {
        Blaze probe = EntityTypes.BLAZE.create(level, EntitySpawnReason.COMMAND);
        check(probe != null, "The rod seed needs a blaze to roll for");
        LootTable table =
                level.getServer()
                        .reloadableRegistries()
                        .getLootTable(probe.getLootTable().orElseThrow());
        LootParams params =
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.THIS_ENTITY, probe)
                        .withParameter(LootContextParams.ORIGIN, probe.position())
                        .withParameter(
                                LootContextParams.DAMAGE_SOURCE,
                                level.damageSources().mobAttack(thrower))
                        .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, thrower)
                        .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, thrower)
                        .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                        .withLuck(player.getLuck())
                        .create(LootContextParamSets.ENTITY);
        for (long seed = 1L; seed <= ROD_SEED_ATTEMPTS; seed++) {
            if (table.getRandomItems(params, seed).stream()
                    .anyMatch(stack -> stack.is(Items.BLAZE_ROD))) {
                return seed;
            }
        }
        throw new AssertionError("No loot seed up to " + ROD_SEED_ATTEMPTS + " rolls a blaze rod");
    }

    private static void wake(TestDedicatedServerContext server, Mob... mobs) {
        server.runOnServer(
                minecraftServer -> {
                    for (Mob mob : mobs) {
                        mob.setNoAi(false);
                    }
                });
    }

    /**
     * The resistance a real player's armour would have bought: the blasts at arm's length and an
     * armed husk's hit stay on camera without plating the player.
     */
    private static void giveResistance(ServerPlayer player) {
        player.addEffect(
                new MobEffectInstance(
                        MobEffects.RESISTANCE,
                        RESISTANCE_TICKS,
                        RESISTANCE_AMPLIFIER,
                        false,
                        false,
                        false));
    }

    private static void waitForClientEntity(ClientGameTestContext context, int entityId) {
        context.waitFor(client -> client.level.getEntity(entityId) != null, LOAD_TIMEOUT_TICKS);
    }

    private static void waitForDeath(
            ClientGameTestContext context, TestDedicatedServerContext server, int entityId) {
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer -> {
                    Entity entity = player(minecraftServer).level().getEntity(entityId);
                    return entity == null
                            || (entity instanceof LivingEntity living && living.isDeadOrDying());
                },
                BREAK_TIMEOUT_TICKS,
                "the take's mob dies");
    }

    private static void emptyHand(ClientGameTestContext context) {
        context.runOnClient(client -> client.player.getInventory().setSelectedSlot(8));
        context.waitTicks(2);
    }

    private static void clearFire(ServerLevel level, int siteX) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = siteX - 12; x <= siteX + 12; x++) {
            for (int z = STAGE_MIN_Z; z <= STAGE_MAX_Z; z++) {
                for (int y = FLOOR_Y - 4; y <= STAND_Y + 3; y++) {
                    if (level.getBlockState(cursor.set(x, y, z)).is(Blocks.FIRE)) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        for (int x = siteX - 12; x <= siteX + 12; x++) {
            for (int z = STAGE_MIN_Z; z <= STAGE_MAX_Z; z++) {
                for (int y = NETHER_FLOOR_Y - 2; y <= NETHER_FLOOR_Y + 4; y++) {
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

    private static String takeId(String scenario, boolean on) {
        return "telekinesis." + scenario + (on ? ".on" : ".off");
    }

    /**
     * Shows the take's caption from the recorded text, the same file the sidecar reads, so the film
     * and its metadata cannot describe a take differently.
     */
    private static void caption(ClientGameTestContext context, String id) {
        String[] text = CAPTIONS.get(id);
        check(text != null, "No caption is recorded for take " + id);
        ClientGameTestRecorder.showStep(context, id, text[0], text[1]);
        context.waitTicks(CAPTION_LEAD_TICKS);
    }

    private static Map<String, String[]> loadCaptions() {
        String resource = "/telekinesis-captions.json";
        try (InputStream stream = TelekinesisShowcase.class.getResourceAsStream(resource)) {
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

    /**
     * Mines a block with the player's own attack key and waits for the server to have broken it:
     * the client removes the block before the server confirms, and the drops are only handed out on
     * the server.
     */
    private static void mine(
            ClientGameTestContext context, TestDedicatedServerContext server, BlockPos pos) {
        ClientGameTestPlayerActions.mine(context, pos);
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer -> player(minecraftServer).level().getBlockState(pos).isAir(),
                ACTION_TIMEOUT_TICKS,
                "the server breaks the mined block");
    }

    private static void buildStages(MinecraftServer server) {
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
        for (boolean on : new boolean[] {false, true}) {
            buildBays(overworld, on);
        }

        ServerLevel nether = server.getLevel(Level.NETHER);
        check(nether != null, "The test server must have a Nether");
        for (int x = -30; x <= 30; x++) {
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
        for (int siteX : new int[] {NETHER_OFF_X, NETHER_ON_X}) {
            BlockPos bedFoot = netherBedFoot(siteX);
            BlockState bed =
                    Blocks.BED
                            .red()
                            .defaultBlockState()
                            .setValue(BedBlock.FACING, Direction.NORTH)
                            .setValue(BedBlock.PART, BedPart.FOOT);
            nether.setBlock(bedFoot, bed, Block.UPDATE_CLIENTS);
            nether.setBlock(
                    bedFoot.north(),
                    bed.setValue(BedBlock.PART, BedPart.HEAD),
                    Block.UPDATE_CLIENTS);
            emeraldChest(nether, bedFoot.north().east(2));
        }

        command(server, "gamerule advance_time false");
        command(server, "gamerule advance_weather false");
        command(server, "gamerule spawn_mobs false");
        command(server, "weather clear");
        command(server, "time set noon");
    }

    /** The blocks of one half of every overworld scenario: both halves are built the same. */
    private static void buildBays(ServerLevel level, boolean on) {
        level.setBlockAndUpdate(
                new BlockPos(bayX(MINING, on), STAND_Y, 2), Blocks.DIAMOND_ORE.defaultBlockState());
        level.setBlockAndUpdate(
                new BlockPos(bayX(FULL_INVENTORY, on), STAND_Y, 2),
                Blocks.DIAMOND_ORE.defaultBlockState());

        BlockPos chest = new BlockPos(bayX(CHEST, on), STAND_Y, CHEST_Z);
        level.setBlockAndUpdate(
                chest,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        ChestBlockEntity chestEntity = (ChestBlockEntity) level.getBlockEntity(chest);
        check(chestEntity != null, "The chest scenario needs its chest");
        List<ItemStack> contents = chestContents();
        for (int slot = 0; slot < contents.size(); slot++) {
            chestEntity.setItem(slot * 3, contents.get(slot).copy());
        }

        BlockPos sapling = new BlockPos(bayX(TREE, on), STAND_Y, 0);
        level.setBlock(
                sapling.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        boolean grown =
                level.registryAccess()
                        .lookupOrThrow(Registries.FEATURE)
                        .getOrThrow(TreeFeatures.OAK)
                        .value()
                        .place(
                                level,
                                level.getChunkSource().getGenerator(),
                                RandomSource.create(4L),
                                sapling);
        check(grown, "The tree scenario's oak must grow");

        // A one-block lava basin the husk stands in, walled in stone so the lava stays put.
        BlockPos pool = new BlockPos(bayX(LAVA, on), STAND_Y, 2);
        level.setBlockAndUpdate(pool, Blocks.LAVA.defaultBlockState());
        for (Direction side : Direction.Plane.HORIZONTAL) {
            level.setBlockAndUpdate(pool.relative(side), Blocks.STONE.defaultBlockState());
        }

        for (int scenario : new int[] {ITEM_FRAME, PAINTING}) {
            BlockPos wall = new BlockPos(bayX(scenario, on), STAND_Y + 1, WALL_Z);
            for (int y = -1; y <= 1; y++) {
                level.setBlockAndUpdate(wall.offset(0, y, 0), Blocks.STONE.defaultBlockState());
            }
        }

        BlockPos tnt = new BlockPos(bayX(TNT, on), STAND_Y, BLAST_BLOCK_Z);
        level.setBlockAndUpdate(tnt, Blocks.TNT.defaultBlockState());
        emeraldChest(level, tnt.east());

        BlockPos anchor = new BlockPos(bayX(ANCHOR, on), STAND_Y, BLAST_BLOCK_Z);
        level.setBlockAndUpdate(
                anchor,
                Blocks.RESPAWN_ANCHOR.defaultBlockState().setValue(RespawnAnchorBlock.CHARGE, 4));
        emeraldChest(level, anchor.east(2));
    }

    private static int bayX(int scenario, boolean on) {
        return FIRST_BAY_X + (2 * scenario + (on ? 1 : 0)) * BAY_SPACING;
    }

    private static BlockPos netherBedFoot(int siteX) {
        return new BlockPos(siteX, NETHER_FLOOR_Y + 1, BLAST_BLOCK_Z);
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

    private static ItemStack pickaxe(ServerLevel level, boolean fortune) {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.enchant(enchantment(level, Enchantments.EFFICIENCY), 5);
        if (fortune) {
            stack.enchant(enchantment(level, Enchantments.FORTUNE), 3);
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
                NETHER_FLOOR_Y + 16,
                STAGE_MAX_Z + 4);
    }

    private static AABB stripBox(ServerLevel level) {
        return new AABB(
                STAGE_MIN_X,
                level.getMinY(),
                STAGE_MIN_Z - 8,
                STAGE_MAX_X,
                level.getMaxY(),
                STAGE_MAX_Z + 8);
    }

    /**
     * With Telekinesis on, every expected stack is in the inventory and nothing is left at the
     * site. With it off, the inventory holds none of them and they lie in the world.
     */
    private static void checkDelivered(
            ServerPlayer player, int siteX, boolean on, List<ItemStack> expected) {
        for (ItemStack stack : expected) {
            if (on) {
                checkCount(player, stack.getItem(), stack.getCount());
            } else {
                checkCount(player, stack.getItem(), 0);
                check(
                        onGround(player.level(), siteX, stack.getItem()) == stack.getCount(),
                        "Expected "
                                + stack
                                + " in the world at site "
                                + siteX
                                + ", found "
                                + onGround(player.level(), siteX, stack.getItem()));
            }
        }
        if (on) {
            checkNoItemsAt(player.level(), siteX);
        }
    }

    private static void checkExperience(ServerPlayer player, int siteX, boolean on) {
        if (on) {
            check(player.totalExperience > 0, "The experience must go to the player");
        } else {
            checkOrbsAt(player.level(), siteX);
        }
    }

    /** A blast's emeralds: all 64 in the inventory, or scattered in the world. */
    private static void checkBlastDrops(ServerPlayer player, int siteX, boolean on) {
        if (on) {
            checkCount(player, Items.EMERALD, 64);
            checkNoItemsAt(player.level(), siteX);
        } else {
            checkCount(player, Items.EMERALD, 0);
            check(
                    onGround(player.level(), siteX, Items.EMERALD) > 0,
                    "The chest's emeralds must scatter in the world");
        }
    }

    private static void checkOrbsAt(ServerLevel level, int siteX) {
        check(orbsAt(level, siteX) > 0, "The experience must drop as orbs at site " + siteX);
    }

    private static int orbsAt(ServerLevel level, int siteX) {
        return level.getEntitiesOfClass(ExperienceOrb.class, siteBox(siteX)).size();
    }

    private static int onGround(ServerLevel level, int siteX, Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, siteBox(siteX)).stream()
                .filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
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
