package io.github.brainage04.telekinesis.drop;

import io.github.brainage04.telekinesis.mixin.ExperienceOrbAccessor;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

public final class BlockDropCapture {
    private static final ThreadLocal<ArrayDeque<ServerPlayer>> BREAKERS = new ThreadLocal<>();
    private BlockDropCapture() {
    }

    public static void begin(ServerPlayer player) {
        ArrayDeque<ServerPlayer> breakers = BREAKERS.get();
        if (breakers == null) {
            breakers = new ArrayDeque<>();
            BREAKERS.set(breakers);
        }
        breakers.addLast(player);
    }
    public static void end() {
        ArrayDeque<ServerPlayer> breakers = BREAKERS.get();
        breakers.removeLast();
        if (breakers.isEmpty()) {
            BREAKERS.remove();
        }
    }

    public static boolean capture(ServerLevel level, ItemEntity itemEntity) {
        ServerPlayer player = activePlayer(level);
        if (player == null) {
            return false;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        int originalCount = stack.getCount();
        player.getInventory().add(stack);

        int insertedCount = originalCount - stack.getCount();
        if (insertedCount > 0) {
            player.awardStat(Stats.ITEM_PICKED_UP.get(item), insertedCount);
        }
        if (stack.isEmpty()) {
            return true;
        }

        itemEntity.setPos(player.getX(), player.getY(), player.getZ());
        itemEntity.setDeltaMovement(Vec3.ZERO);
        return false;
    }

    public static boolean captureExperience(ServerLevel level, int amount) {
        ServerPlayer player = activePlayer(level);
        if (player == null || amount <= 0) {
            return false;
        }

        ExperienceOrb orb = new ExperienceOrb(level, player.position(), Vec3.ZERO, amount);
        int remaining = ((ExperienceOrbAccessor) orb).telekinesis$repairPlayerItems(player, amount);
        if (remaining > 0) {
            player.giveExperiencePoints(remaining);
        }
        return true;
    }

    private static ServerPlayer activePlayer(ServerLevel level) {
        ArrayDeque<ServerPlayer> breakers = BREAKERS.get();
        if (breakers == null) {
            return null;
        }

        ServerPlayer player = breakers.peekLast();
        if (player == null || player.level() != level || !TelekinesisPlayerSettings.isEffective(player)) {
            return null;
        }
        return player;
    }
}
