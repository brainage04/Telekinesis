package io.github.brainage04.telekinesis.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.brainage04.telekinesis.Telekinesis;
import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import io.github.brainage04.telekinesis.platform.TelekinesisPlatform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TelekinesisPlayerSettings {
    private static final int SCHEMA_VERSION = 1;
    private static final String FILE_NAME = "telekinesis-players.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<UUID, PlayerPreference> PREFERENCES = new HashMap<>();
    private static Path statePath;

    private TelekinesisPlayerSettings() {
    }

    public static void initialize(TelekinesisPlatform platform) {
        platform.registerServerStarted(TelekinesisPlayerSettings::load);
        platform.registerServerStopping(server -> {
            save();
            synchronized (TelekinesisPlayerSettings.class) {
                PREFERENCES.clear();
                statePath = null;
            }
        });
    }

    public static synchronized boolean isEnabled(ServerPlayer player) {
        PlayerPreference preference = PREFERENCES.get(player.getUUID());
        return preference == null || preference.enabled();
    }

    public static synchronized boolean isEffective(ServerPlayer player) {
        return TelekinesisConfigManager.config().enabled() && isEnabled(player);
    }

    public static synchronized boolean setEnabled(ServerPlayer player, boolean enabled) {
        PREFERENCES.put(
                player.getUUID(),
                new PlayerPreference(player.getGameProfile().name(), enabled)
        );
        return save();
    }

    public static synchronized boolean toggle(ServerPlayer player) {
        boolean enabled = !isEnabled(player);
        setEnabled(player, enabled);
        return enabled;
    }

    private static synchronized void load(MinecraftServer server) {
        statePath = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        PREFERENCES.clear();
        if (!Files.exists(statePath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(statePath)) {
            SavedState loaded = GSON.fromJson(reader, SavedState.class);
            if (loaded == null || loaded.schemaVersion() != SCHEMA_VERSION || loaded.players() == null) {
                throw new JsonParseException("Unsupported or incomplete player settings document");
            }
            for (Map.Entry<String, PlayerPreference> entry : loaded.players().entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    PlayerPreference preference = entry.getValue();
                    if (preference != null) {
                        PREFERENCES.put(uuid, preference);
                    }
                } catch (IllegalArgumentException exception) {
                    Telekinesis.LOGGER.warn("Ignoring invalid player UUID in {}: {}", statePath, entry.getKey());
                }
            }
        } catch (IOException | JsonParseException exception) {
            Telekinesis.LOGGER.error("Failed to load Telekinesis player settings from {}", statePath, exception);
        }
    }

    private static synchronized boolean save() {
        if (statePath == null) {
            return false;
        }

        Path temporaryPath = statePath.resolveSibling(statePath.getFileName() + ".tmp");
        Map<String, PlayerPreference> serializedPreferences = new HashMap<>();
        PREFERENCES.forEach((uuid, preference) -> serializedPreferences.put(uuid.toString(), preference));
        SavedState state = new SavedState(SCHEMA_VERSION, serializedPreferences);

        try {
            Files.createDirectories(statePath.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryPath)) {
                GSON.toJson(state, writer);
            }
            moveAtomically(temporaryPath, statePath);
            return true;
        } catch (IOException exception) {
            Telekinesis.LOGGER.error("Failed to save Telekinesis player settings to {}", statePath, exception);
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            return false;
        }
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record SavedState(int schemaVersion, Map<String, PlayerPreference> players) {
    }

    private record PlayerPreference(String lastKnownName, boolean enabled) {
    }
}
