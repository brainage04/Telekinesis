package io.github.brainage04.telekinesis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.brainage04.telekinesis.Telekinesis;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TelekinesisConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static Path configPath;
    private static TelekinesisConfig config = new TelekinesisConfig();

    private TelekinesisConfigManager() {
    }

    public static synchronized void initialize(Path configDirectory) {
        configPath = configDirectory.resolve("telekinesis.json");
        if (!Files.exists(configPath)) {
            save();
            return;
        }
        load();
    }

    public static synchronized TelekinesisConfig config() {
        return config;
    }

    public static synchronized boolean setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        return save();
    }

    public static synchronized boolean reload() {
        return load();
    }

    private static boolean load() {
        try (Reader reader = Files.newBufferedReader(configPath)) {
            TelekinesisConfig loaded = GSON.fromJson(reader, TelekinesisConfig.class);
            if (loaded == null) {
                throw new JsonParseException("Configuration document is empty");
            }
            config = loaded;
            return true;
        } catch (IOException | JsonParseException exception) {
            Telekinesis.LOGGER.error("Failed to load Telekinesis configuration from {}", configPath, exception);
            return false;
        }
    }

    private static boolean save() {
        Path temporaryPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryPath)) {
                GSON.toJson(config, writer);
            }
            moveAtomically(temporaryPath, configPath);
            return true;
        } catch (IOException exception) {
            Telekinesis.LOGGER.error("Failed to save Telekinesis configuration to {}", configPath, exception);
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
}
