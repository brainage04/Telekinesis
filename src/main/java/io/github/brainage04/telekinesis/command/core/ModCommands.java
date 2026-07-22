package io.github.brainage04.telekinesis.command.core;

import io.github.brainage04.telekinesis.command.TelekinesisCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TelekinesisCommand.initialize(dispatcher);
        });
    }
}
