package io.github.brainage04.telekinesis.neoforge;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import io.github.brainage04.telekinesis.Telekinesis;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Telekinesis.MOD_ID)
public final class TelekinesisNeoForge {
    private static final DeferredRegister<GameRule<?>> GAME_RULES =
            DeferredRegister.create(Registries.GAME_RULE, Telekinesis.MOD_ID);
    private static final DeferredHolder<GameRule<?>, GameRule<Boolean>> ENABLED =
            GAME_RULES.register(
                    "enabled",
                    () ->
                            new GameRule<>(
                                    GameRuleCategory.PLAYER,
                                    GameRuleType.BOOL,
                                    BoolArgumentType.bool(),
                                    (visitor, rule) -> visitor.visitBoolean(rule),
                                    Codec.BOOL,
                                    value -> value ? 1 : 0,
                                    true,
                                    FeatureFlagSet.of()));

    public TelekinesisNeoForge(IEventBus modEventBus) {
        GAME_RULES.register(modEventBus);
        modEventBus.addListener(
                (FMLCommonSetupEvent event) ->
                        Telekinesis.initialize(new NeoForgeTelekinesisPlatform(), ENABLED.get()));
    }
}
