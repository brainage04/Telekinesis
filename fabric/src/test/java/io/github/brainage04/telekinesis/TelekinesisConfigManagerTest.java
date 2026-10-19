package io.github.brainage04.telekinesis;

import io.github.brainage04.telekinesis.config.TelekinesisConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelekinesisConfigManagerTest {
    @TempDir
    Path directory;

    @Test
    void changesArePersistedAndReloaded() throws IOException {
        Files.writeString(directory.resolve("telekinesis.json"), "{\"enabled\":true}");
        TelekinesisConfigManager.initialize(directory);
        assertTrue(TelekinesisConfigManager.setEnabled(false));
        assertFalse(Files.exists(directory.resolve("telekinesis.json.tmp")));
        TelekinesisConfigManager.config().setEnabled(true);
        assertTrue(TelekinesisConfigManager.reload());
        assertFalse(TelekinesisConfigManager.config().enabled());
    }

    @Test
    void malformedAndEmptyReloadPreserveCurrentSetting() throws IOException {
        Path config = directory.resolve("telekinesis.json");
        Files.writeString(config, "{\"enabled\":false}");
        TelekinesisConfigManager.initialize(directory);
        for (String invalid : new String[]{"{broken", "null", ""}) {
            Files.writeString(config, invalid);
            assertFalse(TelekinesisConfigManager.reload());
            assertFalse(TelekinesisConfigManager.config().enabled());
        }
    }

    @Test
    void saveFailureReportsFailureButKeepsSessionSetting() throws IOException {
        Path config = directory.resolve("telekinesis.json");
        Files.writeString(config, "{\"enabled\":true}");
        TelekinesisConfigManager.initialize(directory);
        Files.createDirectory(directory.resolve("telekinesis.json.tmp"));
        Files.writeString(directory.resolve("telekinesis.json.tmp/occupied"), "block replacement");
        assertFalse(TelekinesisConfigManager.setEnabled(false));
        assertFalse(TelekinesisConfigManager.config().enabled());
        assertTrue(Files.readString(config).contains("true"));
    }
}
