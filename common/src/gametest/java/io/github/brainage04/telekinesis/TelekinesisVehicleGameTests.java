package io.github.brainage04.telekinesis;

import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.check;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.checkCount;
import static io.github.brainage04.telekinesis.TelekinesisKillAndExplosionGameTests.enabledPlayer;

import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/// The vehicles a player breaks: boats, chest boats and every minecart, with the contents of the
/// container variants. Positions stay inside the default eight-block test area.
public final class TelekinesisVehicleGameTests {
    private static final BlockPos PLAYER_POS = new BlockPos(1, 1, 1);
    private static final BlockPos VEHICLE_POS = new BlockPos(3, 1, 3);

    private TelekinesisVehicleGameTests() {}

    public static void boatsBrokenByPlayerSendBoatAndContents(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkBoat", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(VEHICLE_POS);
            player.getInventory().clearContent();

            Boat boat = helper.spawn(EntityTypes.OAK_BOAT, VEHICLE_POS);
            breakVehicle(boat, player);

            check(boat.isRemoved(), "The punches must break the boat");
            checkCount(player, Items.OAK_BOAT, 1, "a boat a player broke");
            check(
                    countNear(level, at, Items.OAK_BOAT, 4.0D) == 0,
                    "A broken boat's item must not stay in the world");

            player.getInventory().clearContent();
            ChestBoat chestBoat = helper.spawn(EntityTypes.OAK_CHEST_BOAT, VEHICLE_POS);
            chestBoat.setItem(0, new ItemStack(Items.EMERALD, 3));
            breakVehicle(chestBoat, player);

            check(chestBoat.isRemoved(), "The punches must break the chest boat");
            checkCount(player, Items.OAK_CHEST_BOAT, 1, "a chest boat a player broke");
            checkCount(player, Items.EMERALD, 3, "the contents of a chest boat");
            check(
                    countNear(level, at, Items.EMERALD, 4.0D) == 0,
                    "A broken chest boat's contents must not stay in the world");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void minecartsBrokenByPlayerSendCartAndContents(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkCarts", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(VEHICLE_POS);
            player.getInventory().clearContent();

            Minecart minecart = helper.spawn(EntityTypes.MINECART, VEHICLE_POS);
            breakVehicle(minecart, player);
            checkCount(player, Items.MINECART, 1, "a minecart a player broke");

            player.getInventory().clearContent();
            MinecartChest chestCart = helper.spawn(EntityTypes.CHEST_MINECART, VEHICLE_POS);
            chestCart.setItem(0, new ItemStack(Items.EMERALD, 2));
            breakVehicle(chestCart, player);
            checkCount(player, Items.CHEST_MINECART, 1, "a chest minecart a player broke");
            checkCount(player, Items.EMERALD, 2, "the contents of a chest minecart");

            player.getInventory().clearContent();
            MinecartHopper hopperCart = helper.spawn(EntityTypes.HOPPER_MINECART, VEHICLE_POS);
            hopperCart.setItem(0, new ItemStack(Items.DIAMOND, 4));
            breakVehicle(hopperCart, player);
            checkCount(player, Items.HOPPER_MINECART, 1, "a hopper minecart a player broke");
            checkCount(player, Items.DIAMOND, 4, "the contents of a hopper minecart");

            player.getInventory().clearContent();
            MinecartFurnace furnaceCart = helper.spawn(EntityTypes.FURNACE_MINECART, VEHICLE_POS);
            breakVehicle(furnaceCart, player);
            checkCount(player, Items.FURNACE_MINECART, 1, "a furnace minecart a player broke");

            player.getInventory().clearContent();
            MinecartTNT tntCart = helper.spawn(EntityTypes.TNT_MINECART, VEHICLE_POS);
            // One hit that breaks it while it stands still: a moving or burning TNT minecart primes
            // instead of dropping, which is vanilla's own rule and not a break.
            check(
                    tntCart.hurtServer(level, level.damageSources().playerAttack(player), 10.0F),
                    "The hit must break the TNT minecart");
            checkCount(player, Items.TNT_MINECART, 1, "a TNT minecart a player broke");
            check(
                    countNear(level, at, Items.TNT_MINECART, 4.0D) == 0,
                    "A broken TNT minecart's item must not stay in the world");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    public static void vehicleBrokenWithTheSettingOffStaysInTheWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkCartOff", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(VEHICLE_POS);
            player.getInventory().clearContent();
            check(TelekinesisPlayerSettings.setEnabled(player, false), "The preference must save");
            MinecartChest cart = helper.spawn(EntityTypes.CHEST_MINECART, VEHICLE_POS);
            cart.setItem(0, new ItemStack(Items.EMERALD, 3));

            breakVehicle(cart, player);

            check(cart.isRemoved(), "The punches must break the chest minecart");
            checkCount(player, Items.CHEST_MINECART, 0, "a player whose setting is off");
            checkCount(player, Items.EMERALD, 0, "a player whose setting is off");
            check(
                    countNear(level, at, Items.CHEST_MINECART, 4.0D) == 1,
                    "The broken minecart's item must stay in the world");
            check(
                    countNear(level, at, Items.EMERALD, 4.0D) == 3,
                    "The broken minecart's contents must stay in the world");
        } finally {
            TelekinesisPlayerSettings.setEnabled(fixture.player(), true);
            fixture.close();
        }
        helper.succeed();
    }

    /// A creative player's punch discards a vehicle without dropping it, as it always did. What the
    /// game drops anyway on the way out, a chest boat's contents, stays in the world.
    public static void creativeBreakOfVehicleStaysInTheWorld(GameTestHelper helper) {
        TelekinesisConfigManager.setEnabled(true);
        try (GameTestPlayer fixture =
                GameTestPlayer.join(helper, "TkCartCreative")
                        .markLoaded()
                        .gameMode(GameType.CREATIVE)
                        .moveTo(helper, PLAYER_POS)) {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(VEHICLE_POS);
            player.getInventory().clearContent();
            TelekinesisPlayerSettings.setEnabled(player, true);
            ChestBoat chestBoat = helper.spawn(EntityTypes.OAK_CHEST_BOAT, VEHICLE_POS);
            chestBoat.setItem(0, new ItemStack(Items.EMERALD, 3));

            player.attack(chestBoat);

            check(chestBoat.isRemoved(), "A creative punch must discard the chest boat");
            checkCount(player, Items.OAK_CHEST_BOAT, 0, "a creative player");
            checkCount(player, Items.EMERALD, 0, "the contents of a creative break");
            check(
                    countNear(level, at, Items.EMERALD, 4.0D) == 3,
                    "The chest boat's contents must stay in the world for a creative break");
        }
        helper.succeed();
    }

    /// Lava names no player, so the minecart and its contents stay where the game spawned them.
    public static void vehicleBrokenWithoutAPlayerStaysInTheWorld(GameTestHelper helper) {
        GameTestPlayer fixture = enabledPlayer(helper, "TkCartLava", PLAYER_POS);
        try {
            ServerPlayer player = fixture.player();
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(VEHICLE_POS);
            player.getInventory().clearContent();
            MinecartChest cart = helper.spawn(EntityTypes.CHEST_MINECART, VEHICLE_POS);
            cart.setItem(0, new ItemStack(Items.EMERALD, 3));

            check(
                    cart.hurtServer(level, level.damageSources().lava(), 100.0F),
                    "Lava must break the chest minecart");

            check(cart.isRemoved(), "The lava must remove the chest minecart");
            checkCount(player, Items.CHEST_MINECART, 0, "a minecart lava broke");
            checkCount(player, Items.EMERALD, 0, "the contents of a minecart lava broke");
            check(
                    countNear(level, at, Items.CHEST_MINECART, 4.0D) == 1,
                    "The lava-broken minecart's item must stay in the world");
            check(
                    countNear(level, at, Items.EMERALD, 4.0D) == 3,
                    "The lava-broken minecart's contents must stay in the world");
        } finally {
            fixture.close();
        }
        helper.succeed();
    }

    /// A vehicle breaks once its accumulated damage passes 40, and a punch deals a fifth of its
    /// damage while the attack cooldown runs, so keep punching rather than counting hits.
    private static void breakVehicle(VehicleEntity vehicle, ServerPlayer player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        for (int punch = 0; punch < 40 && !vehicle.isRemoved(); punch++) {
            player.attack(vehicle);
        }
        check(vehicle.isRemoved(), "The player's punches must break the vehicle");
    }

    private static int countNear(ServerLevel level, BlockPos pos, Item item, double radius) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(radius)).stream()
                .filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }
}
