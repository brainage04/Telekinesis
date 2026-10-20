package io.github.brainage04.telekinesis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.brainage04.brainagelib.storage.JsonFile;
import io.github.brainage04.telekinesis.Telekinesis;

import java.nio.file.Path;

/**
 * The server-wide {@code telekinesis.json}. A file that fails to load keeps the current setting and is not
 * overwritten until a later reload succeeds; changes made meanwhile apply for the session only.
 */
public final class TelekinesisConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static JsonFile<TelekinesisConfig> file;
    private static TelekinesisConfig config = new TelekinesisConfig();

    private TelekinesisConfigManager() {
    }

    public static synchronized void initialize(Path configDirectory) {
        file = JsonFile.builder(configDirectory.resolve("telekinesis.json"), GSON, TelekinesisConfig.class)
                .description("Telekinesis configuration")
                .logger(Telekinesis.LOGGER)
                .build();
        if (file.load() instanceof JsonFile.LoadResult.Loaded<TelekinesisConfig>(TelekinesisConfig loaded)) {
            config = loaded;
        } else if (file.lastLoad() instanceof JsonFile.LoadResult.Missing<TelekinesisConfig>) {
            save();
        }
    }

    public static synchronized TelekinesisConfig config() {
        return config;
    }

    public static synchronized boolean setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        return save();
    }

    /** Reloads the file, keeping the current setting when it is missing or cannot be loaded. */
    public static synchronized boolean reload() {
        if (file.load() instanceof JsonFile.LoadResult.Loaded<TelekinesisConfig>(TelekinesisConfig loaded)) {
            config = loaded;
            return true;
        }
        return false;
    }

    private static boolean save() {
        return file.save(config).saved();
    }
}
