package io.github.brainage04.telekinesis;

import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.check;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.checkCount;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.enabledPlayer;

import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class TelekinesisLeafAndCreditGameTests {
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos LOG_POS = new BlockPos(3, 1, 3);

    /// Rolls of a decaying oak leaf's loot table. It pays out on only about 7% of leaves (a sapling
    /// 5%, sticks 2%, an apple 0.5%; `data/minecraft/loot_table/blocks/oak_leaves.json`), so 300
    /// rolls all empty has probability about 1e-10.
    private static final int LEAF_ROLLS = 300;

    /// How long the End room's chunks may make no generation progress before the wait gives up.
    /// The GameTest server runs its ticks as fast as it can, so a tick budget cannot bound a wait
    /// for chunk generation, and a total wall-clock budget cannot either: a host whose heavy slots
    /// are full takes longer for the same work, and a total budget would fail it while the chunks
    /// are still generating. What the wait can bound is a stall, so it fails only when the
    /// surrounding chunks' status and ticket levels have not moved for this long.
    private static final long ROOM_STALL_TIMEOUT_NANOS = 120_000_000_000L;

    private TelekinesisLeafAndCreditGameTests() {}

    private static boolean blastHit(GameTestPlayer fixture) {
        EmbeddedChannel channel = fixture.channel();
        channel.runPendingTasks();
        channel.flushOutbound();
        boolean hit = false;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            hit |=
                    outbound instanceof ClientboundExplodePacket explosion
                            && explosion.playerKnockback().isPresent();
            ReferenceCountUtil.release(outbound);
        }
        return hit;
    }

    /// Fails the test when the chunks around {@code centre} have made no generation progress for
    /// two minutes of wall time. Entity-ticking status needs the 5x5 chunks around the room at
    /// FULL (ChunkMap.java:360-363), so those are the chunks whose progress decides the wait. Each
    /// poll reads their generation status and loading ticket level; a value that differs from the
    /// last one restarts the clock, so a host that is merely slow keeps making progress and never
    /// trips the bound.
    ///
    /// The check runs on the tick path, not inside the wait's assertion: a sequence's assertion
    /// failure is swallowed while the test still has ticks left (GameTestSequence.java:78-82), so a
    /// bound raised there only reaches the test at tick exhaustion. A runnable registered with
    /// {@link GameTestHelper#runAfterDelay} fails the test at once (GameTestInfo.java:126-134).
    private static final class RoomProgress {
        private final ServerLevel level;
        private final ChunkPos centre;
        private String last;
        private long lastProgressNanos = System.nanoTime();

        private RoomProgress(ServerLevel level, ChunkPos centre) {
            this.level = level;
            this.centre = centre;
        }

        /// Starts the stall check, which reschedules itself every tick. The chain ends with the
        /// test: a done test's tick runnables are never run again (GameTestInfo.java:83-84).
        private void watch(GameTestHelper helper) {
            helper.runAfterDelay(
                    1,
                    () -> {
                        assertStillLoading(helper);
                        watch(helper);
                    });
        }

        private void assertStillLoading(GameTestHelper helper) {
            StringBuilder progress = new StringBuilder();
            for (int x = centre.x() - 2; x <= centre.x() + 2; x++) {
                for (int z = centre.z() - 2; z <= centre.z() + 2; z++) {
                    long key = ChunkPos.pack(x, z);
                    progress.append(level.getChunkSource().chunkMap.getLatestStatus(key))
                            .append('/')
                            .append(
                                    level.getChunkSource()
                                            .chunkMap
                                            .getDistanceManager()
                                            .getChunkLevel(key, false))
                            .append(' ');
                }
            }
            String now = progress.toString();
            long nanos = System.nanoTime();
            if (!now.equals(last)) {
                last = now;
                lastProgressNanos = nanos;
                return;
            }
            if (nanos - lastProgressNanos > ROOM_STALL_TIMEOUT_NANOS) {
                helper.fail(
                        "The End room's chunks made no generation progress for 2 min of wall time,"
                                + " still at "
                                + now);
            }
        }
    }

    public static void decayedLeafOfAFelledTreePaysTheFeller(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkLeaves", PLAYER_POS);
        ServerPlayer player = fixture.player();
        try {
            BlockPos logPos = helper.absolutePos(LOG_POS);
            BlockPos leafPos = logPos.above();
            helper.setBlock(LOG_POS, Blocks.OAK_LOG);
            helper.setBlock(LOG_POS.above(), Blocks.OAK_LEAVES);
            player.getInventory().clearContent();

            check(player.gameMode.destroyBlock(logPos), "The trunk must break");
            check(helper.getBlockState(LOG_POS).isAir(), "The trunk must be gone");

            BlockState leaf = helper.getBlockState(LOG_POS.above());
            check(leaf.is(Blocks.OAK_LEAVES), "The leaf must still stand");
            helper.setBlock(LOG_POS.above(), Blocks.AIR);
            for (int roll = 0; roll < LEAF_ROLLS; roll++) {
                Block.dropResources(leaf, helper.getLevel(), leafPos);
            }

            int paid =
                    player.getInventory().countItem(Items.OAK_SAPLING)
                            + player.getInventory().countItem(Items.STICK)
                            + player.getInventory().countItem(Items.APPLE);
            check(paid > 0, "The decayed leaf of the felled tree must pay the player who cut it");
            check(
                    helper.getLevel()
                            .getEntitiesOfClass(ItemEntity.class, new AABB(leafPos).inflate(3.0D))
                            .isEmpty(),
                    "No leaf drop may stay on the ground of a Telekinesis player");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void lastFellerWithTelekinesisOffKeepsTheirLeaves(GameTestHelper helper) {
        GameTestPlayer first = enabledPlayer(helper, "TkFirstCut", PLAYER_POS);
        GameTestPlayer last = enabledPlayer(helper, "TkLastCut", PLAYER_POS.east());
        try {
            TelekinesisPlayerSettings.setEnabled(last.player(), false);
            BlockPos lower = LOG_POS;
            BlockPos upper = LOG_POS.above();
            BlockPos top = upper.above();
            helper.setBlock(lower, Blocks.OAK_LOG);
            helper.setBlock(upper, Blocks.OAK_LOG);
            for (BlockPos leaf :
                    new BlockPos[] {lower.east(), upper.east(), upper.east().above(), top}) {
                helper.setBlock(leaf, Blocks.OAK_LEAVES);
            }
            first.player().getInventory().clearContent();
            last.player().getInventory().clearContent();

            check(
                    first.player().gameMode.destroyBlock(helper.absolutePos(lower)),
                    "The lower log must break");
            check(
                    last.player().gameMode.destroyBlock(helper.absolutePos(upper)),
                    "The upper log must break");

            BlockState leaf = helper.getBlockState(top);
            check(leaf.is(Blocks.OAK_LEAVES), "The top leaf must still stand");
            BlockPos leafPos = helper.absolutePos(top);
            helper.getLevel()
                    .getEntitiesOfClass(ItemEntity.class, new AABB(leafPos).inflate(3.0D))
                    .forEach(ItemEntity::discard);
            helper.setBlock(top, Blocks.AIR);
            for (int roll = 0; roll < LEAF_ROLLS; roll++) {
                Block.dropResources(leaf, helper.getLevel(), leafPos);
            }

            check(
                    leafDrops(first.player()) == 0,
                    "The top leaf of a tree another player felled must not pay the first cutter");
            check(
                    leafDrops(last.player()) == 0,
                    "A player with Telekinesis off gets no leaf drops");
            check(
                    !helper.getLevel()
                            .getEntitiesOfClass(ItemEntity.class, new AABB(leafPos).inflate(1.0D))
                            .isEmpty(),
                    "The top leaf's drops must stay in the world");
        } finally {
            // The test's own bounds only: the server lays tests out eight to a row, five to six
            // blocks apart (GameTestServer.java:325, StructureGridSpawner.java:17-18), so a radius
            // that reaches past them can delete a neighbouring test's drops.
            helper.getLevel()
                    .getEntitiesOfClass(ItemEntity.class, helper.getBounds())
                    .forEach(ItemEntity::discard);
            first.close();
            last.close();
        }
        helper.succeed();
    }

    private static int leafDrops(ServerPlayer player) {
        return player.getInventory().countItem(Items.OAK_SAPLING)
                + player.getInventory().countItem(Items.STICK)
                + player.getInventory().countItem(Items.APPLE);
    }

    /// The room is a force-loaded End chunk, clicked once it has loaded: a forced chunk loads to
    /// entity-ticking status (ChunkMap.java:128), the status the wait checks
    /// (ServerLevel.java:1929). An explosion reaches the entities of every loaded chunk, ticking or
    /// not (ServerExplosion.java:182, Visibility.java:22-30).
    ///
    /// The wait is bounded by a stall, not by ticks or by total wall time: entity-ticking status
    /// needs the 5x5 chunks around the room at FULL (ChunkMap.java:360-363), and generating those
    /// cold chunks takes as long as the host needs, while the GameTest server runs its ticks as
    /// fast as it can. A tick budget therefore expires while the chunks are still generating: on
    /// CI 43589136758 the wait ran out its 100 ticks and failed, and the same head passed on
    /// rerun. A total wall-clock budget fails the same way on a host whose heavy slots are full,
    /// so the wait fails only when those chunks' status and ticket levels stop moving.
    public static void respawnAnchorBlastPaysThePlayerWhoUsedIt(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        check(end != null, "The test server must have an End");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ChunkPos room = new ChunkPos((origin.getX() + 20_000) >> 4, (origin.getZ() + 64) >> 4);
        BlockPos anchor = new BlockPos(room.getMinBlockX() + 8, 64, room.getMinBlockZ() + 8);
        BlockPos chest = anchor.west(2);
        GameTestPlayer fixture = enabledPlayer(helper, "TkAnchor", PLAYER_POS);
        ServerPlayer player = fixture.player();
        Runnable cleanup =
                () -> {
                    end.getEntitiesOfClass(ItemEntity.class, new AABB(anchor).inflate(8.0D))
                            .forEach(ItemEntity::discard);
                    end.setChunkForced(room.x(), room.z(), false);
                    fixture.close();
                };
        try {
            end.setChunkForced(room.x(), room.z(), true);
            for (BlockPos pos :
                    BlockPos.betweenClosed(anchor.offset(-6, -1, -6), anchor.offset(6, 6, 6))) {
                boolean shell =
                        pos.getY() == anchor.getY() - 1
                                || pos.getY() == anchor.getY() + 6
                                || Math.abs(pos.getX() - anchor.getX()) == 6
                                || Math.abs(pos.getZ() - anchor.getZ()) == 6;
                end.setBlock(
                        pos,
                        (shell ? Blocks.OBSIDIAN : Blocks.AIR).defaultBlockState(),
                        Block.UPDATE_CLIENTS);
            }
            end.setBlock(
                    anchor,
                    Blocks.RESPAWN_ANCHOR
                            .defaultBlockState()
                            .setValue(RespawnAnchorBlock.CHARGE, 4),
                    Block.UPDATE_CLIENTS);
            end.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) end.getBlockEntity(chest))
                    .setItem(0, new ItemStack(Items.EMERALD, 64));
            player.getInventory().clearContent();
            player.teleportTo(
                    end,
                    anchor.getX() + 0.5D,
                    anchor.getY(),
                    anchor.getZ() + 3.5D,
                    Set.of(),
                    0.0F,
                    0.0F,
                    true);
            // What the client's teleport confirmation would do: a player still changing dimension
            // takes no damage.
            player.hasChangedDimension();
            fixture.markLoaded();
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 3));
        } catch (RuntimeException | Error failure) {
            cleanup.run();
            throw failure;
        }
        RoomProgress roomProgress = new RoomProgress(end, room);
        roomProgress.watch(helper);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        end.isPositionEntityTicking(anchor),
                                        "The End room must tick entities"))
                .thenExecute(
                        () -> {
                            try {
                                fixture.clearMessages();

                                player.gameMode.useItemOn(
                                        player,
                                        end,
                                        ItemStack.EMPTY,
                                        InteractionHand.MAIN_HAND,
                                        new BlockHitResult(
                                                Vec3.atCenterOf(anchor),
                                                Direction.UP,
                                                anchor,
                                                false));

                                check(
                                        !end.getBlockState(anchor).is(Blocks.RESPAWN_ANCHOR),
                                        "The anchor must explode");
                                checkCount(
                                        player,
                                        Items.EMERALD,
                                        64,
                                        "the chest the anchor's blast broke");
                                check(
                                        blastHit(fixture),
                                        "The blast must still reach the player who used the anchor");
                            } finally {
                                cleanup.run();
                            }
                        })
                .thenSucceed();
    }
}
