package io.github.brainage04.telekinesis.command.core;

import io.github.brainage04.telekinesis.command.TelekinesisCommand;
import io.github.brainage04.telekinesis.platform.TelekinesisPlatform;

public final class ModCommands {
    private ModCommands() {
    }

    public static void initialize(TelekinesisPlatform platform) {
        platform.registerCommands(TelekinesisCommand::initialize);
    }
}
