package io.github.brainage04.telekinesis.drop;

import io.github.brainage04.telekinesis.mixin.ExperienceOrbAccessor;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/// Sends the items and experience spawned inside a capture scope to the scope's player.
///
/// A scope covers one player block break, one mob's death loot credited to a player, or the block drops of one
/// explosion a player caused. Scopes nest; the innermost one decides. A mob death or explosion without a player
/// cause opens no scope, so inside a block break its drops still go to the breaker, as before mob and explosion
/// support existed. A player's own death drops open a scope for nobody (a `null` player), which keeps any outer
/// scope from handing that inventory to someone else.
public final class DropCapture {
    private static final ThreadLocal<ArrayList<ServerPlayer>> RECIPIENTS = new ThreadLocal<>();

    private DropCapture() {
    }

    public static void begin(ServerPlayer player) {
        ArrayList<ServerPlayer> recipients = RECIPIENTS.get();
        if (recipients == null) {
            recipients = new ArrayList<>();
            RECIPIENTS.set(recipients);
        }
        recipients.add(player);
    }

    public static void end() {
        ArrayList<ServerPlayer> recipients = RECIPIENTS.get();
        recipients.removeLast();
        if (recipients.isEmpty()) {
            RECIPIENTS.remove();
        }
    }

    /// The player the game credits with killing `victim`, or `null`: the player who hurt it within the last 100
    /// ticks, directly, with a projectile or damaging potion they own, or through their tamed wolf. Vanilla uses the
    /// same record, with the same `lastHurtByPlayerMemoryTime > 0` test, for player-kill loot and experience.
    public static ServerPlayer mobKiller(LivingEntity victim) {
        if (victim.getLastHurtByPlayerMemoryTime() <= 0) {
            return null;
        }
        return victim.getLastHurtByPlayer() instanceof ServerPlayer player ? player : null;
    }

    /// The player who caused `explosion`: its indirect source (the player who lit the TNT, or who owns the
    /// projectile or fireball that exploded), else the attacker in its damage source (the player who hit an end
    /// crystal). Creepers, beds, respawn anchors and TNT nobody lit have no player cause (`null`).
    public static ServerPlayer explosionCause(ServerExplosion explosion) {
        if (explosion.getIndirectSourceEntity() instanceof ServerPlayer player) {
            return player;
        }
        return explosion.getDamageSource().getEntity() instanceof ServerPlayer player ? player : null;
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
        ArrayList<ServerPlayer> recipients = RECIPIENTS.get();
        if (recipients == null) {
            return null;
        }

        // A dead player still resolves as a killer or TNT owner until they respawn, and a corpse's inventory is
        // discarded at respawn: their drops stay in the world instead.
        ServerPlayer player = recipients.getLast();
        if (player == null || !player.isAlive() || player.level() != level
                || !TelekinesisPlayerSettings.isEffective(player)) {
            return null;
        }
        return player;
    }
}
