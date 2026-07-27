package io.github.brainage04.telekinesis.neoforge;

import io.github.brainage04.telekinesis.Telekinesis;
import io.github.brainage04.telekinesis.TelekinesisNeoForgeGameTestSuite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Telekinesis.MOD_ID)
public final class NeoForgeServerGameTests {
    private NeoForgeServerGameTests() {
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        for (TelekinesisNeoForgeGameTestSuite.TestCase test : TelekinesisNeoForgeGameTestSuite.tests()) {
            Identifier id = Identifier.fromNamespaceAndPath(Telekinesis.MOD_ID, test.path());
            event.register(BuiltInRegistries.TEST_FUNCTION.key(), id, test::function);
        }
    }
}
