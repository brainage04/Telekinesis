package io.github.brainage04.telekinesis.player;

import io.github.brainage04.brainagelib.storage.WorldPlayerStore;
import io.github.brainage04.telekinesis.Telekinesis;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.platform.TelekinesisPlatform;
import net.minecraft.server.level.ServerPlayer;

/**
 * Each player's Telekinesis preference, stored per world in {@code telekinesis-players.json}. A file that fails to
 * load is kept unchanged; preference changes then apply for the session only.
 */
public final class TelekinesisPlayerSettings {
    private static final WorldPlayerStore<PlayerPreference> STORE = WorldPlayerStore
            .builder("telekinesis-players.json", PlayerPreference.class, () -> new PlayerPreference(null, true))
            .description("Telekinesis player settings")
            .logger(Telekinesis.LOGGER)
            .build();

    private TelekinesisPlayerSettings() {
    }

    public static void initialize(TelekinesisPlatform platform) {
        platform.registerServerStarted(STORE::load);
        platform.registerServerStopping(server -> STORE.close());
    }

    public static boolean isEnabled(ServerPlayer player) {
        return STORE.get(player.getUUID()).enabled();
    }

    public static boolean isEffective(ServerPlayer player) {
        return TelekinesisConfigManager.config().enabled() && isEnabled(player);
    }

    /** Changes the player's preference; false when it applies for this session only because it could not be saved. */
    public static synchronized boolean setEnabled(ServerPlayer player, boolean enabled) {
        STORE.put(player.getUUID(), new PlayerPreference(player.getGameProfile().name(), enabled));
        return STORE.save().saved();
    }

    private record PlayerPreference(String lastKnownName, boolean enabled) {
    }
}
