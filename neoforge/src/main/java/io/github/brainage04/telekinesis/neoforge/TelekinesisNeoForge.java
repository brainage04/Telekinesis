package io.github.brainage04.telekinesis.neoforge;

import io.github.brainage04.telekinesis.Telekinesis;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Telekinesis.MOD_ID)
public final class TelekinesisNeoForge {
    public TelekinesisNeoForge(IEventBus modBus) {
        Telekinesis.initialize(new NeoForgeTelekinesisPlatform());
    }
}
