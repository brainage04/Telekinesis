package io.github.brainage04.telekinesis;

import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.check;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.checkCount;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.enabledPlayer;

import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/// The two sources the game gives no player to name: a leaf of a tree the player cut, which decays later with no
/// entity at all, and a respawn anchor the player used where it explodes. Beds take the anchor's path; BrainageLib's
/// own suite exercises both.
public final class TelekinesisLeafAndCreditGameTests {
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos LOG_POS = new BlockPos(3, 1, 3);

    /// Rolls of a decaying oak leaf's loot table; with no drop at all in about one leaf of thirteen, a count that has
    /// to be above zero is in practice.
    private static final int LEAF_ROLLS = 300;

    private TelekinesisLeafAndCreditGameTests() {
    }

    /// A player cuts a tree's trunk; the leaf it held up decays later with no entity to name, and its drops reach the
    /// player who cut the tree instead of the ground.
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

            int paid = player.getInventory().countItem(Items.OAK_SAPLING)
                    + player.getInventory().countItem(Items.STICK)
                    + player.getInventory().countItem(Items.APPLE);
            check(paid > 0, "The decayed leaf of the felled tree must pay the player who cut it");
            check(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(leafPos).inflate(3.0D)).isEmpty(),
                    "No leaf drop may stay on the ground of a Telekinesis player");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// A charged respawn anchor used outside the Nether explodes naming nobody; the chest its blast breaks pays the
    /// player who used it, and the blast still hurts that player.
    public static void respawnAnchorBlastPaysThePlayerWhoUsedIt(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        check(end != null, "The test server must have an End");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos anchor = new BlockPos(origin.getX() + 20_000, 64, origin.getZ() + 64);
        BlockPos chest = anchor.west(2);
        GameTestPlayer fixture = enabledPlayer(helper, "TkAnchor", PLAYER_POS);
        ServerPlayer player = fixture.player();
        try {
            for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-6, -1, -6), anchor.offset(6, 6, 6))) {
                boolean shell = pos.getY() == anchor.getY() - 1 || pos.getY() == anchor.getY() + 6
                        || Math.abs(pos.getX() - anchor.getX()) == 6 || Math.abs(pos.getZ() - anchor.getZ()) == 6;
                end.setBlock(pos, (shell ? Blocks.OBSIDIAN : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            end.setBlock(anchor, Blocks.RESPAWN_ANCHOR.defaultBlockState().setValue(RespawnAnchorBlock.CHARGE, 4),
                    Block.UPDATE_CLIENTS);
            end.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) end.getBlockEntity(chest)).setItem(0, new ItemStack(Items.EMERALD, 64));
            player.getInventory().clearContent();
            player.teleportTo(end, anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 3.5D, Set.of(), 0.0F, 0.0F,
                    true);
            // What the client's teleport confirmation would do: a player still changing dimension takes no damage.
            player.hasChangedDimension();
            fixture.markLoaded();
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 3));
            float health = player.getHealth();

            player.gameMode.useItemOn(player, end, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(anchor), Direction.UP, anchor, false));

            check(!end.getBlockState(anchor).is(Blocks.RESPAWN_ANCHOR), "The anchor must explode");
            checkCount(player, Items.EMERALD, 64, "the chest the anchor's blast broke");
            check(player.getHealth() < health, "The player must still take their own blast's damage");
        } finally {
            end.getEntitiesOfClass(ItemEntity.class, new AABB(anchor).inflate(8.0D)).forEach(ItemEntity::discard);
            fixture.close();
        }
        helper.succeed();
    }
}
