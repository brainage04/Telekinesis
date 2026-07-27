package io.github.brainage04.telekinesis.fabric;

import io.github.brainage04.telekinesis.Telekinesis;
import net.fabricmc.api.ModInitializer;

public final class TelekinesisFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Telekinesis.initialize(FabricTelekinesisPlatform.INSTANCE);
    }
}
