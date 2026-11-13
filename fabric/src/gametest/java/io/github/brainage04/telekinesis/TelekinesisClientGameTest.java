package io.github.brainage04.telekinesis;

import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

@SuppressWarnings("UnstableApiUsage")
public final class TelekinesisClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        ClientGameTestServers.withDedicatedServer(
                context,
                TelekinesisShowcase.serverProperties(),
                "Telekinesis recording GameTest",
                TelekinesisShowcase::buildWorld,
                server -> TelekinesisShowcase.film(context, server));
    }
}
