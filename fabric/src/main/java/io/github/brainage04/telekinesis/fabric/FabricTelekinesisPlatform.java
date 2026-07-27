package io.github.brainage04.telekinesis.fabric;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.telekinesis.platform.TelekinesisPlatform;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class FabricTelekinesisPlatform implements TelekinesisPlatform {
    public static final FabricTelekinesisPlatform INSTANCE = new FabricTelekinesisPlatform();

    private FabricTelekinesisPlatform() {
    }

    @Override public Path configDirectory() { return FabricLoader.getInstance().getConfigDir(); }
    @Override public void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback) { CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> callback.accept(dispatcher)); }
    @Override public void registerServerStarted(Consumer<MinecraftServer> callback) { ServerLifecycleEvents.SERVER_STARTED.register(callback::accept); }
    @Override public void registerServerStopping(Consumer<MinecraftServer> callback) { ServerLifecycleEvents.SERVER_STOPPING.register(callback::accept); }
}
