package io.github.brainage04.telekinesis.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import io.github.brainage04.telekinesis.platform.TelekinesisPlatform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class NeoForgeTelekinesisPlatform implements TelekinesisPlatform {
    @Override public Path configDirectory() { return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get(); }
    @Override public void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback) { NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> callback.accept(event.getDispatcher())); }
    @Override public void registerServerStarted(Consumer<MinecraftServer> callback) { NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> callback.accept(event.getServer())); }
    @Override public void registerServerStopping(Consumer<MinecraftServer> callback) { NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> callback.accept(event.getServer())); }
}
