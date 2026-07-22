package io.github.brainage04.telekinesis.gamerule.core;

import io.github.brainage04.telekinesis.Telekinesis;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public final class ModGameRules {
    public static final GameRule<Boolean> ENABLE_TELEKINESIS = GameRuleBuilder.forBoolean(true)
            .category(GameRuleCategory.PLAYER)
            .buildAndRegister(Identifier.fromNamespaceAndPath(Telekinesis.MOD_ID, "enabled"));

    private ModGameRules() {
    }

    public static void initialize() {
        // Loading this class registers the game rule before a server starts.
    }
}
