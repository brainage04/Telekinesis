package io.github.brainage04.telekinesis.mixin;

import io.github.brainage04.telekinesis.drop.BlockDropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void telekinesis$captureBlockDrop(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        ServerLevel level = (ServerLevel) (Object) this;
        boolean captured = entity instanceof ItemEntity itemEntity
                ? BlockDropCapture.capture(level, itemEntity)
                : entity instanceof ExperienceOrb experienceOrb
                && BlockDropCapture.captureExperience(level, experienceOrb.getValue());
        if (captured) {
            callback.setReturnValue(false);
        }
    }
}
