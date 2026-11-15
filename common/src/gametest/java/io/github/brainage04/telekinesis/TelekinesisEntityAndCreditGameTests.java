package io.github.brainage04.telekinesis;

import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.check;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.checkCount;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.enabledPlayer;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.probeMob;

import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/// The entities a player breaks, and the kills the game credits to them when the killing blow was
/// somebody else's. Positions stay inside the default eight-block test area.
public final class TelekinesisEntityAndCreditGameTests {
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos MOB_POS = new BlockPos(3, 1, 1);
    private static final BlockPos STAND_POS = new BlockPos(4, 1, 5);
    private static final BlockPos FRAME_WALL = new BlockPos(2, 1, 5);
    private static final BlockPos GLOW_WALL = new BlockPos(6, 1, 5);

    private TelekinesisEntityAndCreditGameTests() {}

    public static void armorStandBrokenByPlayerSendsStandAndEquipment(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkStand", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(STAND_POS);
            ArmorStand stand = new ArmorStand(level, at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
            stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            check(level.addFreshEntity(stand), "The stand must spawn");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

            // A stand shakes on the first punch and breaks on the second within five ticks
            player.attack(stand);
            player.attack(stand);

            check(stand.isRemoved(), "The second punch must break the stand");
            checkCount(player, Items.ARMOR_STAND, 1, "the stand a player broke");
            checkCount(player, Items.IRON_HELMET, 1, "the broken stand's helmet");
            checkCount(player, Items.DIAMOND_SWORD, 1, "the broken stand's sword");
            check(
                    countNear(level, at, Items.ARMOR_STAND, 6.0D) == 0,
                    "No stand item may stay in the world");
            check(
                    countNear(level, at.above(), Items.IRON_HELMET, 6.0D) == 0,
                    "No equipment of a broken stand may stay in the world");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void itemFramesBrokenByPlayerSendFramesAndContents(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkFrames", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos wall = helper.absolutePos(FRAME_WALL);
            ItemFrame frame =
                    hangingFrame(
                            level,
                            wall,
                            new ItemFrame(level, wall.north(), Direction.NORTH),
                            Items.DIAMOND);

            player.attack(frame);
            checkCount(player, Items.DIAMOND, 1, "the item a punch knocked out of a frame");
            check(!frame.isRemoved(), "The frame must survive the punch that emptied it");

            player.attack(frame);
            check(frame.isRemoved(), "The second punch must break the frame");
            checkCount(player, Items.ITEM_FRAME, 1, "the frame a player broke");
            check(
                    countNear(level, wall.north(), Items.ITEM_FRAME, 6.0D) == 0,
                    "No frame item may stay in the world");

            BlockPos glowWall = helper.absolutePos(GLOW_WALL);
            ItemFrame glow =
                    hangingFrame(
                            level,
                            glowWall,
                            new GlowItemFrame(level, glowWall.north(), Direction.NORTH),
                            Items.EMERALD);

            player.attack(glow);
            player.attack(glow);

            check(glow.isRemoved(), "The second punch must break the glow frame");
            checkCount(player, Items.EMERALD, 1, "the item a punch knocked out of a glow frame");
            checkCount(player, Items.GLOW_ITEM_FRAME, 1, "the glow frame a player broke");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void paintingBrokenByPlayerSendsThePainting(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkPainting", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos wall = helper.absolutePos(FRAME_WALL);
            level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            Painting painting =
                    new Painting(
                            level,
                            wall.north(),
                            Direction.NORTH,
                            VariantUtils.getAny(
                                    level.registryAccess(), Registries.PAINTING_VARIANT));
            check(level.addFreshEntity(painting), "The painting must spawn");

            player.attack(painting);

            check(painting.isRemoved(), "The punch must break the painting");
            checkCount(player, Items.PAINTING, 1, "the painting a player broke");
            check(
                    countNear(level, wall.north(), Items.PAINTING, 6.0D) == 0,
                    "A broken painting may not stay in the world");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void cushionBrokenByPlayerSendsTheCushion(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkCushion", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(STAND_POS);
            Cushion cushion = new Cushion(EntityTypes.CUSHION, level);
            cushion.setColor(DyeColor.LIGHT_BLUE);
            cushion.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
            check(level.addFreshEntity(cushion), "The cushion must spawn");
            Item dyedCushion = Items.CUSHION.pick(DyeColor.LIGHT_BLUE);

            player.attack(cushion);

            check(cushion.isRemoved(), "The punch must break the cushion");
            checkCount(player, dyedCushion, 1, "the cushion a player broke");
            check(
                    countNear(level, at, dyedCushion, 6.0D) == 0,
                    "A broken cushion may not stay in the world");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void brokenEntityWithTheSettingOffStaysInTheWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkStandOff", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            check(TelekinesisPlayerSettings.setEnabled(player, false), "The preference must save");
            BlockPos at = helper.absolutePos(STAND_POS);
            ArmorStand stand = new ArmorStand(level, at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
            stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            check(level.addFreshEntity(stand), "The stand must spawn");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));

            player.attack(stand);
            player.attack(stand);

            check(stand.isRemoved(), "The stand must break");
            checkCount(player, Items.ARMOR_STAND, 0, "a player whose setting is off");
            checkCount(player, Items.IRON_HELMET, 0, "a player whose setting is off");
            check(
                    countNear(level, at, Items.ARMOR_STAND, 6.0D) == 1,
                    "The stand item must stay in the world");
            check(
                    countNear(level, at.above(), Items.IRON_HELMET, 6.0D) == 1,
                    "The broken stand's helmet must stay in the world");
        } finally {
            TelekinesisPlayerSettings.setEnabled(fixture.player(), true);
            fixture.close();
        }
        helper.succeed();
    }

    /// A hit from the player, then a death from something else inside the game's five seconds: the
    /// game credits the player, so the loot is theirs.
    public static void killCreditedAfterThePlayersHitGoesToKiller(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkIndirect", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            Husk husk = probeMob(helper, "tk_indirect", MOB_POS);
            husk.setHealth(20.0F);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            player.attack(husk);
            check(!husk.isDeadOrDying(), "The player's hit must not kill the husk");
            check(
                    husk.getLastHurtByPlayerMemoryTime() > 0,
                    "The game must credit the player's hit");

            husk.hurtServer(level, level.damageSources().lava(), 100.0F);

            check(husk.isDeadOrDying(), "The lava must kill the husk");
            checkCount(player, Items.EMERALD, 1, "a death the game credits after the player's hit");
            check(
                    countNear(level, helper.absolutePos(MOB_POS), Items.EMERALD, 6.0D) == 0,
                    "No emerald of a credited kill may stay in the world");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// The game credits a kill through its last-hurt record alone, with its 100-tick player timer
    /// already out; the player who gets the kill statistic gets the loot.
    public static void killCreditedOnlyByTheLastHurtRecordGoesToKiller(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkRecord", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            Husk husk = probeMob(helper, "tk_record", MOB_POS);
            husk.setLastHurtByMob(player);
            check(
                    husk.getLastHurtByPlayerMemoryTime() == 0,
                    "The husk must have no player hit on its timer");

            husk.kill(helper.getLevel());

            check(husk.isDeadOrDying(), "The husk must die");
            checkCount(player, Items.EMERALD, 1, "a kill the game credits through its record");
            check(
                    countNear(helper.getLevel(), helper.absolutePos(MOB_POS), Items.EMERALD, 6.0D)
                            == 0,
                    "No emerald of a credited kill may stay in the world");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// A death the game credits to a mob is not the player's: the loot stays in the world, as it
    /// does for a kill nobody is credited with.
    public static void killCreditedToAMobStaysInTheWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkMobKill", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            player.getInventory().clearContent();
            Husk attacker = probeMob(helper, "tk_attacker", new BlockPos(5, 1, 1));
            Husk victim = probeMob(helper, "tk_victim", MOB_POS);
            victim.setHealth(20.0F);

            victim.hurtServer(level, level.damageSources().mobAttack(attacker), 1.0F);
            check(!victim.isDeadOrDying(), "One husk's blow must not kill the other");
            check(victim.getKillCredit() == attacker, "The game must credit the attacking husk");

            victim.hurtServer(level, level.damageSources().lava(), 100.0F);

            check(victim.isDeadOrDying(), "The lava must kill the victim");
            checkCount(player, Items.EMERALD, 0, "a kill the game credits to a mob");
            check(
                    countNear(level, helper.absolutePos(MOB_POS), Items.EMERALD, 6.0D) == 1,
                    "A mob's kill loot must stay in the world");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    private static ItemFrame hangingFrame(
            ServerLevel level, BlockPos wall, ItemFrame frame, Item content) {
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        frame.setItem(new ItemStack(content));
        check(level.addFreshEntity(frame), "The " + content + " frame must spawn");
        return frame;
    }

    private static int countNear(ServerLevel level, BlockPos pos, Item item, double radius) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(radius)).stream()
                .filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }
}
