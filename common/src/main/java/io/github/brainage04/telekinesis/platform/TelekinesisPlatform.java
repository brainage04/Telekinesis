package io.github.brainage04.telekinesis.platform;

import com.mojang.brigadier.CommandDispatcher;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public interface TelekinesisPlatform {
    void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback);

    void registerServerStarted(Consumer<MinecraftServer> callback);

    void registerServerStopping(Consumer<MinecraftServer> callback);
}
