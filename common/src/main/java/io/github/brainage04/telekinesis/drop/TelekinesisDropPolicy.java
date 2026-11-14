package io.github.brainage04.telekinesis.drop;

import io.github.brainage04.brainagelib.drops.DropPolicy;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import net.minecraft.server.level.ServerPlayer;

/**
 * It registers at {@link io.github.brainage04.brainagelib.drops.DropPolicies#DEFAULT_PRIORITY}, so
 * a minigame that owns its players during a match keeps its own rules for them and this mod covers
 * everyone else.
 */
public final class TelekinesisDropPolicy implements DropPolicy {
    public static final TelekinesisDropPolicy INSTANCE = new TelekinesisDropPolicy();

    private TelekinesisDropPolicy() {}

    @Override
    public boolean governs(ServerPlayer player) {
        return true;
    }

    @Override
    public boolean delivers(ServerPlayer player) {
        return TelekinesisPlayerSettings.isEffective(player);
    }
}
