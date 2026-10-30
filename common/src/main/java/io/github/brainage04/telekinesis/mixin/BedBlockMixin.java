package io.github.brainage04.telekinesis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.brainage04.telekinesis.drop.DropCapture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// Vanilla explodes a bed in the Nether without a source entity, so the game records no cause. The player who used
/// the bed is recorded around that one `explode` call and becomes the explosion's cause for drop capture.
@Mixin(BedBlock.class)
public abstract class BedBlockMixin {
    @WrapOperation(
            method = "destroyOnUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;"
                            + "Lnet/minecraft/world/damagesource/DamageSource;"
                            + "Lnet/minecraft/world/level/ExplosionDamageCalculator;"
                            + "Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
            )
    )
    private void telekinesis$recordBedUser(
            Level level,
            Entity source,
            DamageSource damageSource,
            ExplosionDamageCalculator damageCalculator,
            Vec3 center,
            float radius,
            boolean fire,
            Level.ExplosionInteraction interaction,
            Operation<Void> original,
            @Local(argsOnly = true) Player player
    ) {
        DropCapture.beginBlockExplosion(player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
        try {
            original.call(level, source, damageSource, damageCalculator, center, radius, fire, interaction);
        } finally {
            DropCapture.endBlockExplosion();
        }
    }
}
