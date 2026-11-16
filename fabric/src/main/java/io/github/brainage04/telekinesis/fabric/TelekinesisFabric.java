package io.github.brainage04.telekinesis.fabric;

import io.github.brainage04.telekinesis.Telekinesis;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public final class TelekinesisFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Telekinesis.initialize(
                FabricTelekinesisPlatform.INSTANCE,
                GameRuleBuilder.forBoolean(true)
                        .category(GameRuleCategory.PLAYER)
                        .buildAndRegister(
                                Identifier.fromNamespaceAndPath(Telekinesis.MOD_ID, "enabled")));
    }
}
