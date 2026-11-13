package io.github.brainage04.telekinesis;

import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.check;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.checkCount;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.checkNoDropsNear;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.enabledPlayer;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.probeMob;

import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/// Kills that an iron golem, a snow golem or a wolf finishes. Telekinesis follows the game's kill
/// credit (`LivingEntity.getKillCredit`, LivingEntity.java:1984-1990 in the 26.3 sources): a
/// player's hit inside the game's 100 ticks, or a tamed wolf's bite, makes the kill the player's; a
/// golem's kill of a mob nobody hit is the golem's, and its loot stays in the world.
public final class TelekinesisCompanionKillGameTests {
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos OTHER_PLAYER_POS = new BlockPos(1, 1, 5);
    private static final BlockPos MOB_POS = new BlockPos(3, 1, 1);
    private static final BlockPos COMPANION_POS = new BlockPos(5, 1, 3);

    /// Low enough that one golem blow kills: an iron golem hits for at least 7.5
    /// (IronGolem.java:189-195).
    private static final float AFTER_HIT_HEALTH = 5.0F;

    /// A fist does 1 damage, so the punched blaze keeps 2, and a snowball does 3 to a blaze
    /// (Snowball.java:58-59).
    private static final float BLAZE_HEALTH = 3.0F;

    private TelekinesisCompanionKillGameTests() {}

    public static void golemKillAfterThePlayersHitGoesToThePlayer(GameTestHelper helper) {
        golemKillAfterThePlayersHit(helper, "TkGolemHit", true);
    }

    /// A village golem differs from a player-built one only in the flag that stops it attacking
    /// players (IronGolem.java:137-139), so its kill is credited the same way.
    public static void villageGolemKillAfterThePlayersHitGoesToThePlayer(GameTestHelper helper) {
        golemKillAfterThePlayersHit(helper, "TkVillageGolem", false);
    }

    private static void golemKillAfterThePlayersHit(
            GameTestHelper helper, String name, boolean playerCreated) {
        GameTestPlayer fixture = enabledPlayer(helper, name, PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            Husk husk = probeMob(helper, "tk_golem_hit", MOB_POS);
            husk.setHealth(20.0F);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            player.attack(husk);
            check(husk.getLastHurtByPlayer() == player, "The game must record the player's hit");
            husk.setHealth(AFTER_HIT_HEALTH);
            IronGolem golem = golem(helper, playerCreated);

            golem.doHurtTarget(level, husk);

            check(husk.isDeadOrDying(), "The golem's blow must kill the husk");
            check(husk.getKillCredit() == player, "The game must credit the player's hit");
            checkCount(player, Items.EMERALD, 1, "a golem's kill after the player's hit");
            check(player.totalExperience > 0, "The kill's experience must go to the player");
            checkNoDropsNear(helper, MOB_POS);
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// A mob's hit on a player gives the player no credit (Mob.java:1395), so a golem's kill of a
    /// mob that only hit the player is the golem's: no experience drops
    /// (LivingEntity.java:1529-1536) and the loot stays.
    public static void golemKillOfAMobThatHitThePlayerStaysInTheWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkGolemDefend", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            player.getInventory().clearContent();
            Husk husk = probeMob(helper, "tk_golem_defend", MOB_POS);
            husk.setHealth(AFTER_HIT_HEALTH);
            check(husk.doHurtTarget(level, player), "The husk's hit must land on the player");
            check(player.getLastHurtByMob() == husk, "The player must record the husk's hit");
            IronGolem golem = golem(helper, true);

            golem.doHurtTarget(level, husk);

            check(husk.isDeadOrDying(), "The golem's blow must kill the husk");
            check(husk.getKillCredit() == golem, "The game must credit the golem");
            checkCount(player, Items.EMERALD, 0, "a kill the game credits to a golem");
            check(
                    near(helper, ItemEntity.class) == 1,
                    "The golem's kill must leave the husk's emerald in the world");
            check(near(helper, ExperienceOrb.class) == 0, "A golem's kill drops no experience");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void snowballFinishingABlazeThePlayerHitGoesToThePlayer(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkSnowHit", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            Blaze blaze = blaze(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.attack(blaze);
            check(!blaze.isDeadOrDying(), "The punch must not kill the blaze");
            SnowGolem snowGolem = snowGolem(helper);

            hitWithSnowball(level, snowGolem, blaze);

            check(blaze.isDeadOrDying(), "The snowball must kill the blaze");
            check(blaze.getKillCredit() == player, "The game must credit the player's punch");
            check(player.totalExperience > 0, "The blaze's experience must go to the player");
            checkNoDropsNear(helper, MOB_POS);
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// The blaze's rod needs a player kill (VanillaEntityLoot.java:58-68) and a kill credited to a
    /// snow golem drops no experience, so nothing is left to capture or to leave.
    public static void snowGolemKillOfABlazeNobodyHitLeavesNothing(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkSnowAlone", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            player.getInventory().clearContent();
            Blaze blaze = blaze(helper);
            SnowGolem snowGolem = snowGolem(helper);

            hitWithSnowball(helper.getLevel(), snowGolem, blaze);

            check(blaze.isDeadOrDying(), "The snowball must kill the blaze");
            check(blaze.getKillCredit() == snowGolem, "The game must credit the snow golem");
            checkCount(player, Items.BLAZE_ROD, 0, "a kill the game credits to a snow golem");
            check(player.totalExperience == 0, "The player gets no experience");
            checkNoDropsNear(helper, MOB_POS);
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// A tamed wolf's bite credits its owner (LivingEntity.java:1369-1371), also when the mob only
    /// ever hit the owner.
    public static void wolfKillOfAMobThatHitTheOwnerGoesToOwner(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkWolfDefend", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            Husk husk = probeMob(helper, "tk_wolf_defend", MOB_POS);
            check(husk.doHurtTarget(level, player), "The husk's hit must land on the player");
            Wolf wolf = tamedWolf(helper, player);

            wolf.doHurtTarget(level, husk);

            check(husk.isDeadOrDying(), "The wolf's bite must kill the husk");
            checkCount(player, Items.EMERALD, 1, "the owner of the wolf that killed the husk");
            check(player.totalExperience > 0, "The kill's experience must go to the owner");
            checkNoDropsNear(helper, MOB_POS);
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// A tamed wolf's bite replaces the player record with its owner (LivingEntity.java:1369-1371),
    /// so the kill is the wolf owner's even after another player's hit.
    public static void anotherPlayersWolfKillGoesToThatPlayer(GameTestHelper helper) {
        GameTestPlayer hitter = enabledPlayer(helper, "TkWolfHitter", PLAYER_POS);
        GameTestPlayer owner = enabledPlayer(helper, "TkWolfOwnerB", OTHER_PLAYER_POS);
        try {
            ServerPlayer first = hitter.player();
            ServerPlayer second = owner.player();
            ServerLevel level = helper.getLevel();
            Husk husk = probeMob(helper, "tk_wolf_other", MOB_POS);
            husk.setHealth(20.0F);
            first.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            first.attack(husk);
            check(husk.getLastHurtByPlayer() == first, "The game must record the first hit");
            husk.setHealth(1.0F);
            Wolf wolf = tamedWolf(helper, second);

            wolf.doHurtTarget(level, husk);

            check(husk.isDeadOrDying(), "The wolf's bite must kill the husk");
            check(husk.getKillCredit() == second, "The game must credit the wolf's owner");
            checkCount(second, Items.EMERALD, 1, "the owner of the wolf that killed the husk");
            checkCount(first, Items.EMERALD, 0, "the player whose hit the wolf's bite replaced");
            checkNoDropsNear(helper, MOB_POS);
        } finally {
            hitter.close();
            owner.close();
        }
        helper.succeed();
    }

    private static IronGolem golem(GameTestHelper helper, boolean playerCreated) {
        IronGolem golem = helper.spawn(EntityTypes.IRON_GOLEM, COMPANION_POS);
        golem.setNoAi(true);
        golem.setPlayerCreated(playerCreated);
        return golem;
    }

    private static SnowGolem snowGolem(GameTestHelper helper) {
        SnowGolem snowGolem = helper.spawn(EntityTypes.SNOW_GOLEM, COMPANION_POS);
        snowGolem.setNoAi(true);
        return snowGolem;
    }

    private static Wolf tamedWolf(GameTestHelper helper, ServerPlayer owner) {
        Wolf wolf = helper.spawn(EntityTypes.WOLF, COMPANION_POS);
        wolf.setNoAi(true);
        wolf.tame(owner);
        return wolf;
    }

    private static Blaze blaze(GameTestHelper helper) {
        Blaze blaze = helper.spawn(EntityTypes.BLAZE, MOB_POS);
        blaze.setNoAi(true);
        blaze.setHealth(BLAZE_HEALTH);
        return blaze;
    }

    /// The damage a snow golem's snowball deals on impact: the snowball as the direct entity and
    /// its owner as the cause (Snowball.java:58-59, DamageSources.java:239-240).
    private static void hitWithSnowball(ServerLevel level, SnowGolem owner, Blaze target) {
        Snowball snowball = new Snowball(level, owner, new ItemStack(Items.SNOWBALL));
        target.hurtServer(level, level.damageSources().thrown(snowball, owner), 3.0F);
    }

    private static int near(GameTestHelper helper, Class<? extends Entity> type) {
        return helper.getLevel()
                .getEntitiesOfClass(type, new AABB(helper.absolutePos(MOB_POS)).inflate(3.0D))
                .size();
    }
}
