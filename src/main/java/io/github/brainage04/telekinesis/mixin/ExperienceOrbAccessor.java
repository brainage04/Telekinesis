package io.github.brainage04.telekinesis.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccessor {
    @Invoker("repairPlayerItems")
    int telekinesis$repairPlayerItems(ServerPlayer player, int experience);
}
