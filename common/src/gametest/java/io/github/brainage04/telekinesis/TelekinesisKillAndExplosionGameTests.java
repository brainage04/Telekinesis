package io.github.brainage04.telekinesis;

import io.github.brainage04.brainagelib.drops.DropCapture;
import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/// Mobs use the test loot table `telekinesis:gametest/kill_probe` (one emerald, plus one per
/// Looting level), so every count is exact. Explosions break a chest holding 64 emeralds: container
/// contents always drop, unlike block loot, which explosions destroy at random.
public final class TelekinesisKillAndExplosionGameTests {
    private static final String KILL_PROBE = "telekinesis:gametest/kill_probe";
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos MOB_POS = new BlockPos(3, 1, 1);

    /// Close to the structure: primed TNT placed 8 blocks out never exploded in a test run. The
    /// explosion tests have extra padding so the blast does not reach neighbouring tests.
    private static final BlockPos TNT_POS = new BlockPos(3, 1, 3);

    private static final BlockPos CHEST_POS = new BlockPos(4, 1, 3);

    /// Outside the 8-block reach of a power-4 explosion's damage.
    private static final BlockPos FAR_PLAYER_POS = new BlockPos(3, 1, 13);

    private TelekinesisKillAndExplosionGameTests() {}

    public static void meleeKillSendsLootEquipmentAndExperienceToKiller(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkMelee", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            Husk husk = probeMob(helper, "tk_melee", MOB_POS);
            husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            husk.setDropChance(EquipmentSlot.HEAD, 2.0F);
            int experienceBefore = player.totalExperience;

            player.attack(husk);

            check(husk.isDeadOrDying(), "The melee hit must kill the husk");
            checkCount(player, Items.EMERALD, 1, "loot-table drop");
            checkCount(player, Items.IRON_HELMET, 1, "guaranteed equipment drop");
            check(
                    player.totalExperience > experienceBefore,
                    "The husk's experience must go to the killer");
            checkNoDropsNear(helper, MOB_POS);
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void projectileKillSendsLootToShooter(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkArrow", PLAYER_POS);
        ServerPlayer player = fixture.player();
        ServerLevel level = helper.getLevel();
        Husk husk = probeMob(helper, "tk_arrow", MOB_POS);
        int experienceBefore = player.totalExperience;
        Arrow arrow =
                new Arrow(level, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        Vec3 target = husk.position().add(0.0D, husk.getBbHeight() / 2.0D, 0.0D);
        arrow.setPos(target.add(-1.5D, 0.0D, 0.0D));
        arrow.shoot(1.0D, 0.0D, 0.0D, 2.0F, 0.0F);
        level.addFreshEntity(arrow);

        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        husk.isDeadOrDying(), "The arrow must kill the husk"))
                .thenExecute(
                        () -> {
                            try {
                                checkCount(player, Items.EMERALD, 1, "arrow kill drop");
                                check(
                                        player.totalExperience > experienceBefore,
                                        "Experience must go to the shooter");
                                checkNoDropsNear(helper, MOB_POS);
                            } finally {
                                arrow.discard();
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    public static void lootingAppliesToCapturedLoot(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkLooting", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(
                    helper.getLevel()
                            .registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.LOOTING),
                    3);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            Husk husk = probeMob(helper, "tk_looting", MOB_POS);

            player.attack(husk);

            check(husk.isDeadOrDying(), "The melee hit must kill the husk");
            checkCount(player, Items.EMERALD, 4, "Looting III drop (1 + 3)");
            checkNoDropsNear(helper, MOB_POS);
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void fullInventoryDropsKillLootAtKillerFeet(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkFullKill", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            Husk husk = probeMob(helper, "tk_fullkill", MOB_POS);

            player.attack(husk);

            check(husk.isDeadOrDying(), "The melee hit must kill the husk");
            checkCount(player, Items.EMERALD, 0, "full inventory");
            ItemEntity remainder =
                    helper
                            .getLevel()
                            .getEntitiesOfClass(
                                    ItemEntity.class,
                                    player.getBoundingBox().inflate(1.0D),
                                    item -> item.getItem().is(Items.EMERALD))
                            .stream()
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new AssertionError(
                                                    "Expected the emerald at the killer's feet"));
            check(
                    remainder.position().distanceToSqr(player.position()) < 0.01D,
                    "Expected the overflow at the killer's exact position, found "
                            + remainder.position());
            remainder.discard();
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void tntLitByPlayerSendsBlockDropsToPlayer(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkTnt", FAR_PLAYER_POS);
        ServerPlayer player = fixture.player();
        placeEmeraldChest(helper);
        primeTnt(helper, player);

        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        helper.getBlockState(CHEST_POS).isAir(),
                                        "The TNT must destroy the chest"))
                .thenExecute(
                        () -> {
                            try {
                                checkCount(
                                        player,
                                        Items.EMERALD,
                                        64,
                                        "chest contents from the player's TNT");
                                check(
                                        emeraldsNear(helper, CHEST_POS) == 0,
                                        "No emerald may stay at the chest");
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    public static void explosionWithoutPlayerLeavesDropsInWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkTntNobody", FAR_PLAYER_POS);
        ServerPlayer player = fixture.player();
        placeEmeraldChest(helper);
        primeTnt(helper, null);

        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        helper.getBlockState(CHEST_POS).isAir(),
                                        "The TNT must destroy the chest"))
                .thenExecute(
                        () -> {
                            try {
                                checkCount(player, Items.EMERALD, 0, "TNT nobody lit");
                                int dropped = emeraldsNear(helper, CHEST_POS);
                                check(
                                        dropped == 64,
                                        "Expected the 64 emeralds on the ground, found " + dropped);
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    public static void mobDeathWithoutPlayerLeavesDropsInWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkNoKiller", PLAYER_POS);
        try {
            Husk husk = probeMob(helper, "tk_nokiller", MOB_POS);

            husk.kill(helper.getLevel());

            checkCount(fixture.player(), Items.EMERALD, 0, "mob death nobody caused");
            check(emeraldsNear(helper, MOB_POS) == 1, "Expected the husk's emerald on the ground");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void anotherPlayersKillGoesToThatPlayer(GameTestHelper helper) {
        GameTestPlayer watcher = enabledPlayer(helper, "TkWatcher", PLAYER_POS);
        GameTestPlayer killer = enabledPlayer(helper, "TkKiller", new BlockPos(1, 1, 3));
        try {
            killer.player()
                    .setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            Husk husk = probeMob(helper, "tk_other", MOB_POS);

            killer.player().attack(husk);

            check(husk.isDeadOrDying(), "The melee hit must kill the husk");
            checkCount(killer.player(), Items.EMERALD, 1, "the killer's drop");
            checkCount(watcher.player(), Items.EMERALD, 0, "a bystander");
        } finally {
            killer.close();
            watcher.close();
        }
        helper.succeed();
    }

    public static void killedPlayersDropsStayInWorld(GameTestHelper helper) {
        GameTestPlayer killer = enabledPlayer(helper, "TkPvpKiller", PLAYER_POS);
        GameTestPlayer victim = enabledPlayer(helper, "TkPvpVictim", MOB_POS);
        try {
            killer.player()
                    .setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            victim.player().getInventory().setItem(9, new ItemStack(Items.EMERALD, 5));
            victim.player().setHealth(0.1F);

            killer.player().attack(victim.player());

            check(victim.player().isDeadOrDying(), "The melee hit must kill the victim");
            checkCount(killer.player(), Items.EMERALD, 0, "another player's death drops");
            check(
                    emeraldsNear(helper, MOB_POS) == 5,
                    "Expected the victim's emeralds on the ground");
        } finally {
            victim.close();
            killer.close();
        }
        helper.succeed();
    }

    /// A dead player still resolves as the TNT's owner until they respawn, and the corpse's
    /// inventory is discarded at respawn, so the chest's contents must stay in the world.
    public static void deadTntOwnerLeavesDropsInWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkTntDead", FAR_PLAYER_POS);
        ServerPlayer player = fixture.player();
        placeEmeraldChest(helper);
        primeTnt(helper, player);
        player.kill(helper.getLevel());

        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        helper.getBlockState(CHEST_POS).isAir(),
                                        "The TNT must destroy the chest"))
                .thenExecute(
                        () -> {
                            try {
                                check(player.isDeadOrDying(), "The TNT's owner must be dead");
                                checkCount(player, Items.EMERALD, 0, "a dead TNT owner");
                                int dropped = emeraldsNear(helper, CHEST_POS);
                                check(
                                        dropped == 64,
                                        "Expected the 64 emeralds on the ground, found " + dropped);
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    public static void deadKillerLeavesKillLootInWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkKillerDead", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            Husk husk = probeMob(helper, "tk_deadkiller", MOB_POS);
            husk.setLastHurtByPlayer(player, 100);
            player.kill(helper.getLevel());

            husk.kill(helper.getLevel());

            checkCount(player, Items.EMERALD, 0, "a dead killer");
            check(emeraldsNear(helper, MOB_POS) == 1, "Expected the husk's emerald on the ground");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// Vanilla forgets the player's hit 100 ticks after it; a death after that is nobody's.
    public static void expiredKillCreditLeavesKillLootInWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkKillerOld", PLAYER_POS);
        ServerPlayer player = fixture.player();
        ServerLevel level = helper.getLevel();
        Husk husk = probeMob(helper, "tk_oldkiller", MOB_POS);
        husk.setLastHurtByPlayer(player, 100);

        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        husk.getLastHurtByPlayer() == null,
                                        "The game must forget the player's hit"))
                .thenExecute(
                        () -> {
                            try {
                                husk.kill(level);

                                check(
                                        husk.getKillCredit() == null,
                                        "The game must credit nobody with the kill");
                                checkCount(player, Items.EMERALD, 0, "an expired kill credit");
                                check(
                                        emeraldsNear(helper, MOB_POS) == 1,
                                        "Expected the husk's emerald on the ground");
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    public static void playerDeathInsideAnotherScopeStaysInWorld(GameTestHelper helper) {
        GameTestPlayer outer = enabledPlayer(helper, "TkScopeOwner", PLAYER_POS);
        GameTestPlayer victim = enabledPlayer(helper, "TkScopeVictim", MOB_POS);
        try {
            victim.player().getInventory().setItem(9, new ItemStack(Items.EMERALD, 5));

            DropCapture.begin(outer.player());
            try {
                victim.player().kill(helper.getLevel());
            } finally {
                DropCapture.end();
            }

            check(victim.player().isDeadOrDying(), "The victim must be dead");
            checkCount(
                    outer.player(),
                    Items.EMERALD,
                    0,
                    "another player's death drops inside a scope");
            check(
                    emeraldsNear(helper, MOB_POS) == 5,
                    "Expected the victim's emeralds on the ground");
        } finally {
            victim.close();
            outer.close();
        }
        helper.succeed();
    }

    /// The dragon releases 500 experience (no previous dragon fight) between death ticks 155 and
    /// 200, after the 100-tick kill credit has expired.
    public static void dragonDeathExperienceGoesToKiller(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkDragon", FAR_PLAYER_POS);
        ServerPlayer player = fixture.player();
        int experienceBefore = player.totalExperience;
        EnderDragon dragon = helper.spawn(EntityTypes.ENDER_DRAGON, MOB_POS);
        dragon.setNoAi(true);
        dragon.setLastHurtByPlayer(player, 100);
        dragon.setHealth(0.0F);

        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        dragon.isRemoved(),
                                        "The dragon must finish its death animation"))
                .thenExecute(
                        () -> {
                            try {
                                int gained = player.totalExperience - experienceBefore;
                                check(
                                        gained == 500,
                                        "Expected the dragon's 500 experience, found " + gained);
                                check(
                                        helper.getLevel()
                                                .getEntitiesOfClass(
                                                        ExperienceOrb.class,
                                                        new AABB(helper.absolutePos(MOB_POS))
                                                                .inflate(32.0D))
                                                .isEmpty(),
                                        "No experience orb may drop at the dragon");
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    /// Vanilla credits a tamed wolf's kill to its owner, as if the owner had hit the mob.
    public static void tamedWolfKillGoesToOwner(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkWolfOwner", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            Wolf wolf = helper.spawn(EntityTypes.WOLF, new BlockPos(3, 1, 3));
            wolf.setNoAi(true);
            wolf.tame(player);
            Husk husk = probeMob(helper, "tk_wolf", MOB_POS);

            wolf.doHurtTarget(helper.getLevel(), husk);

            check(husk.isDeadOrDying(), "The wolf's bite must kill the husk");
            checkCount(player, Items.EMERALD, 1, "the tamed wolf's kill");
            checkNoDropsNear(helper, MOB_POS);
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// Harming heals undead, so this probe is a pig.
    public static void harmingPotionKillGoesToThrower(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkPotion", FAR_PLAYER_POS);
        ServerPlayer player = fixture.player();
        ServerLevel level = helper.getLevel();
        Pig pig = probeMob(helper, "minecraft:pig", Pig.class, "tk_potion", MOB_POS);
        ThrownSplashPotion potion =
                new ThrownSplashPotion(
                        level,
                        player,
                        PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HARMING));
        potion.setPos(pig.position().add(0.0D, 2.0D, 0.0D));
        potion.setDeltaMovement(0.0D, -0.5D, 0.0D);
        level.addFreshEntity(potion);

        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        pig.isDeadOrDying(),
                                        "The Harming splash must kill the pig"))
                .thenExecute(
                        () -> {
                            try {
                                checkCount(player, Items.EMERALD, 1, "the potion kill's drop");
                                checkNoDropsNear(helper, MOB_POS);
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    /// The player's TNT sets off a row of TNT blocks; only the chain reaches the chest, which the
    /// test checks before the chain goes off. Each ray of a power-4 blast starts with 4 times 0.7
    /// to 1.3 of power and spends 0.225 per 0.3-block step plus a block's resistance (TNT 0, a
    /// chest 2.5) as it enters it: a straight ray needs about 5.07 to break a chest whose near face
    /// is 4.5 blocks away, which about one blast in ten has, so the first TNT stands 6.5 blocks
    /// from the chest, beyond any ray.
    public static void chainedTntCreditsPlayerWhoLitTheFirst(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkTntChain", FAR_PLAYER_POS);
        ServerPlayer player = fixture.player();
        BlockPos chest = new BlockPos(8, 1, 3);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.TNT);
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.TNT);
        helper.setBlock(new BlockPos(7, 1, 3), Blocks.TNT);
        helper.setBlock(chest, Blocks.CHEST);
        helper.getBlockEntity(chest, ChestBlockEntity.class)
                .setItem(0, new ItemStack(Items.EMERALD, 64));
        PrimedTnt first = primeTnt(helper, player, new BlockPos(1, 1, 3));

        helper.startSequence()
                .thenWaitUntil(
                        () -> helper.assertTrue(first.isRemoved(), "The first TNT must explode"))
                .thenExecute(
                        () ->
                                check(
                                        helper.getBlockState(chest).is(Blocks.CHEST),
                                        "The first TNT's blast must not reach the chest"))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        helper.getBlockState(chest).isAir(),
                                        "The chained TNT must destroy the chest"))
                .thenExecute(
                        () -> {
                            try {
                                checkCount(
                                        player,
                                        Items.EMERALD,
                                        64,
                                        "chest contents from chained TNT");
                                check(
                                        emeraldsNear(helper, chest) == 0,
                                        "No emerald may stay at the chest");
                            } finally {
                                fixture.close();
                            }
                        })
                .thenSucceed();
    }

    public static void commandItemsAreNotCaptured(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkCommandItems", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            CommandSourceStack source =
                    helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
            Vec3 at = player.position();
            helper.getLevel()
                    .getServer()
                    .getCommands()
                    .performPrefixedCommand(
                            source,
                            String.format(
                                    java.util.Locale.ROOT,
                                    "summon minecraft:item %.2f %.2f %.2f {Item:{id:\"minecraft:emerald\",count:5}}",
                                    at.x(),
                                    at.y(),
                                    at.z()));
            checkCount(player, Items.EMERALD, 0, "/summon item");
            check(
                    emeraldsNear(helper, PLAYER_POS) == 5,
                    "Expected the summoned emeralds to stay in the world");

            // /give also spawns a "fake" item entity for the pickup animation; capturing it would
            // give more than 3.
            helper.getLevel()
                    .getServer()
                    .getCommands()
                    .performPrefixedCommand(
                            source, "give " + player.getScoreboardName() + " minecraft:diamond 3");
            checkCount(player, Items.DIAMOND, 3, "/give");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// Vanilla explodes a bed in the Nether without recording a cause.
    public static void netherBedExplosionSendsBlockDropsToUser(GameTestHelper helper) {
        GameTestPlayer user = enabledPlayer(helper, "TkBedUser", PLAYER_POS);
        GameTestPlayer bystander = enabledPlayer(helper, "TkBedBystander", new BlockPos(1, 1, 4));
        try {
            // The explosion hurts entities before it breaks blocks, so both players must be immune
            // to it.
            user.gameMode(GameType.CREATIVE);
            bystander.gameMode(GameType.CREATIVE);
            ServerLevel nether = nether(helper);
            BlockPos base = netherBase(helper);
            BlockPos chest = placeBedWithChest(nether, base);
            BlockPos foot = base.above();
            moveIntoLevel(user.player(), nether, foot.south(2));
            moveIntoLevel(bystander.player(), nether, foot.north().east(1));

            useWithEmptyHand(nether, user.player(), foot);

            checkCount(user.player(), Items.EMERALD, 64, "the bed user");
            checkCount(bystander.player(), Items.EMERALD, 0, "a bystander nearer the blast");
            check(emeraldsNear(nether, chest) == 0, "No emerald may stay at the chest");
        } finally {
            bystander.close();
            user.close();
        }
        helper.succeed();
    }

    public static void overworldRespawnAnchorExplosionSendsBlockDropsToUser(GameTestHelper helper) {
        GameTestPlayer user = enabledPlayer(helper, "TkAnchorUser", PLAYER_POS);
        GameTestPlayer bystander =
                enabledPlayer(helper, "TkAnchorBystander", new BlockPos(1, 1, 4));
        try {
            user.gameMode(GameType.CREATIVE);
            bystander.gameMode(GameType.CREATIVE);
            ServerLevel level = helper.getLevel();
            BlockPos anchor = new BlockPos(4, 1, 6);
            placeChargedAnchor(helper, anchor);
            check(
                    !RespawnAnchorBlock.canSetSpawn(level, helper.absolutePos(anchor)),
                    "The anchor must explode outside the Nether");
            BlockPos chest = new BlockPos(6, 1, 6);
            placeEmeraldChest(helper, chest);
            bystander.moveTo(helper, new BlockPos(5, 1, 7));

            useWithEmptyHand(level, user.player(), helper.absolutePos(anchor));

            checkCount(user.player(), Items.EMERALD, 64, "the anchor user");
            checkCount(bystander.player(), Items.EMERALD, 0, "a bystander nearer the blast");
            check(emeraldsNear(helper, chest) == 0, "No emerald may stay at the chest");
        } finally {
            bystander.close();
            user.close();
        }
        helper.succeed();
    }

    public static void bedExplosionIsCreditedToThePlayerWhoUsedIt(GameTestHelper helper) {
        GameTestPlayer bystander = enabledPlayer(helper, "TkBedNearest", PLAYER_POS);
        GameTestPlayer user = enabledPlayer(helper, "TkBedActor", new BlockPos(1, 1, 4));
        try {
            bystander.gameMode(GameType.CREATIVE);
            user.gameMode(GameType.CREATIVE);
            ServerLevel nether = nether(helper);
            BlockPos base = netherBase(helper).offset(32, 0, 0);
            BlockPos chest = placeBedWithChest(nether, base);
            BlockPos foot = base.above();
            moveIntoLevel(user.player(), nether, foot.south(2));
            moveIntoLevel(bystander.player(), nether, foot.north().east(1));

            useWithEmptyHand(nether, user.player(), foot);

            checkCount(user.player(), Items.EMERALD, 64, "the player who used the bed");
            checkCount(bystander.player(), Items.EMERALD, 0, "a player nearer the blast");
            check(emeraldsNear(nether, chest) == 0, "No emerald may stay at the chest");
        } finally {
            user.close();
            bystander.close();
        }
        helper.succeed();
    }

    public static GameTestPlayer enabledPlayer(
            GameTestHelper helper, String name, BlockPos position) {
        TelekinesisConfigManager.setEnabled(true);
        GameTestPlayer fixture =
                GameTestPlayer.join(helper, name)
                        .markLoaded()
                        .gameMode(GameType.SURVIVAL)
                        .moveTo(helper, position);
        TelekinesisPlayerSettings.setEnabled(fixture.player(), true);
        return fixture;
    }

    /// A husk does not burn in daylight, so only the test can kill it. Its 0.1 health falls to any
    /// hit: a player who just joined attacks with no charge, and a held sword's damage applies from
    /// the next tick.
    public static Husk probeMob(GameTestHelper helper, String tag, BlockPos relative) {
        return probeMob(helper, "minecraft:husk", Husk.class, tag, relative);
    }

    private static <T extends LivingEntity> T probeMob(
            GameTestHelper helper,
            String type,
            Class<T> entityClass,
            String tag,
            BlockPos relative) {
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(relative));
        CommandSourceStack source =
                helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
        helper.getLevel()
                .getServer()
                .getCommands()
                .performPrefixedCommand(
                        source,
                        String.format(
                                java.util.Locale.ROOT,
                                "summon %s %.2f %.2f %.2f {NoAI:1b,Health:0.1f,DeathLootTable:\"%s\",Tags:[\"%s\"]}",
                                type,
                                at.x(),
                                at.y(),
                                at.z(),
                                KILL_PROBE,
                                tag));
        return helper
                .getLevel()
                .getEntitiesOfClass(
                        entityClass,
                        new AABB(at, at).inflate(1.0D),
                        entity -> entity.entityTags().contains(tag))
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not summon the " + tag + " " + type));
    }

    private static void placeEmeraldChest(GameTestHelper helper) {
        placeEmeraldChest(helper, CHEST_POS);
    }

    private static void placeEmeraldChest(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, Blocks.CHEST);
        helper.getBlockEntity(relative, ChestBlockEntity.class)
                .setItem(0, new ItemStack(Items.EMERALD, 64));
    }

    private static ServerLevel nether(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        check(nether != null, "The test server must have a Nether");
        return nether;
    }

    /// A position in the Nether above the bedrock roof, where the chunk is air, at the test
    /// structure's own x/z so no two test instances share ground.
    private static BlockPos netherBase(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return new BlockPos(origin.getX(), 200, origin.getZ());
    }

    private static BlockPos placeBedWithChest(ServerLevel level, BlockPos base) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlockAndUpdate(base.offset(dx, 0, dz), Blocks.STONE.defaultBlockState());
            }
        }
        BlockPos foot = base.above();
        BlockState bed =
                Blocks.BED
                        .red()
                        .defaultBlockState()
                        .setValue(BedBlock.PART, BedPart.FOOT)
                        .setValue(BedBlock.FACING, Direction.NORTH);
        level.setBlockAndUpdate(foot, bed);
        level.setBlockAndUpdate(foot.north(), bed.setValue(BedBlock.PART, BedPart.HEAD));
        BlockPos chest = foot.east(2);
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chestEntity = (ChestBlockEntity) level.getBlockEntity(chest);
        check(chestEntity != null, "The chest must be placed in the Nether");
        chestEntity.setItem(0, new ItemStack(Items.EMERALD, 64));
        return chest;
    }

    private static void placeChargedAnchor(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(
                relative,
                Blocks.RESPAWN_ANCHOR.defaultBlockState().setValue(RespawnAnchorBlock.CHARGE, 4));
    }

    private static void moveIntoLevel(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.teleportTo(
                level,
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                Set.of(),
                0.0F,
                0.0F,
                false);
        check(player.level() == level, "The player must end up in the Nether");
    }

    private static void useWithEmptyHand(Level level, ServerPlayer player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        check(!state.isAir(), "Expected a block to use at " + pos);
        state.useWithoutItem(
                level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static int emeraldsNear(ServerLevel level, BlockPos absolutePos) {
        return level
                .getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(absolutePos).inflate(6.0D),
                        item -> item.getItem().is(Items.EMERALD))
                .stream()
                .mapToInt(item -> item.getItem().getCount())
                .sum();
    }

    private static PrimedTnt primeTnt(GameTestHelper helper, ServerPlayer owner) {
        return primeTnt(helper, owner, TNT_POS);
    }

    private static PrimedTnt primeTnt(
            GameTestHelper helper, ServerPlayer owner, BlockPos relative) {
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(relative));
        PrimedTnt tnt = new PrimedTnt(helper.getLevel(), at.x(), at.y(), at.z(), owner);
        tnt.setFuse(1);
        helper.getLevel().addFreshEntity(tnt);
        return tnt;
    }

    public static void checkNoDropsNear(GameTestHelper helper, BlockPos relative) {
        AABB area = new AABB(helper.absolutePos(relative)).inflate(3.0D);
        check(
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).isEmpty(),
                "No item may drop at the mob");
        check(
                helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, area).isEmpty(),
                "No experience orb may drop at the mob");
    }

    private static int emeraldsNear(GameTestHelper helper, BlockPos relative) {
        return helper
                .getLevel()
                .getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(helper.absolutePos(relative)).inflate(6.0D),
                        item -> item.getItem().is(Items.EMERALD))
                .stream()
                .mapToInt(item -> item.getItem().getCount())
                .sum();
    }

    public static void checkCount(ServerPlayer player, Item item, int expected, String what) {
        int actual = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                actual += stack.getCount();
            }
        }
        check(
                actual == expected,
                "Expected "
                        + expected
                        + " "
                        + item
                        + " in the inventory ("
                        + what
                        + "), found "
                        + actual);
    }

    public static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
