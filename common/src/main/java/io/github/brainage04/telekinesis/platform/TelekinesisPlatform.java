package io.github.brainage04.telekinesis.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.function.Consumer;

/** Loader services required by Telekinesis' shared server implementation. */
public interface TelekinesisPlatform {
    Path configDirectory();

    void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback);

    void registerServerStarted(Consumer<MinecraftServer> callback);

    void registerServerStopping(Consumer<MinecraftServer> callback);
}
