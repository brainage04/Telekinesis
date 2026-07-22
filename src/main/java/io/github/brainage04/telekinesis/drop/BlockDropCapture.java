package io.github.brainage04.telekinesis.drop;

import io.github.brainage04.telekinesis.gamerule.core.ModGameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        ArrayDeque<ServerPlayer> breakers = BREAKERS.get();
        if (breakers == null) {
            return false;
        }

        ServerPlayer player = breakers.peekLast();
        if (player == null || player.level() != level) {
            return false;
        }
        if (!level.getGameRules().get(ModGameRules.ENABLE_TELEKINESIS)) {
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
        if (insertedCount <= 0) {
            return false;
        }

        player.awardStat(Stats.ITEM_PICKED_UP.get(item), insertedCount);
        return stack.isEmpty();
    }
}
