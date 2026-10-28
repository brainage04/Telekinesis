package io.github.brainage04.telekinesis.neoforge;

import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.github.brainage04.telekinesis.Telekinesis;
import io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/// A drop another mod adds in `LivingDropsEvent` goes to the killer once, like the loot-table drop.
@EventBusSubscriber(modid = Telekinesis.MOD_ID)
public final class NeoForgeLivingDropsGameTests {
    private static final String EXTRA_DROP_TAG = "tk_extra_drop";
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos MOB_POS = new BlockPos(3, 1, 1);

    private NeoForgeLivingDropsGameTests() {}

    /// Stands in for another mod: adds one diamond to the drops of mobs tagged by this test only.
    @SubscribeEvent
    public static void addExtraDrop(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.entityTags().contains(EXTRA_DROP_TAG)) {
            event.getDrops()
                    .add(
                            new ItemEntity(
                                    entity.level(),
                                    entity.getX(),
                                    entity.getY(),
                                    entity.getZ(),
                                    new ItemStack(Items.DIAMOND)));
        }
    }

    public static void livingDropsEventItemGoesToKillerOnce(GameTestHelper helper) {
        GameTestPlayer fixture =
                TelekinesisKillAndExplosionGameTests.enabledPlayer(
                        helper, "TkNeoDrops", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            Husk husk =
                    TelekinesisKillAndExplosionGameTests.probeMob(helper, EXTRA_DROP_TAG, MOB_POS);

            player.attack(husk);

            TelekinesisKillAndExplosionGameTests.check(
                    husk.isDeadOrDying(), "The melee hit must kill the husk");
            TelekinesisKillAndExplosionGameTests.checkCount(
                    player, Items.DIAMOND, 1, "the LivingDropsEvent drop");
            TelekinesisKillAndExplosionGameTests.checkCount(
                    player, Items.EMERALD, 1, "the loot-table drop");
            TelekinesisKillAndExplosionGameTests.check(
                    helper.getLevel()
                            .getEntitiesOfClass(
                                    ItemEntity.class,
                                    new AABB(helper.absolutePos(MOB_POS)).inflate(6.0D))
                            .isEmpty(),
                    "No item may stay on the ground");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }
}
