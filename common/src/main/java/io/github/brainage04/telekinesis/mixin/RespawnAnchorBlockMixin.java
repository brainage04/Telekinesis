package io.github.brainage04.telekinesis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.brainage04.telekinesis.drop.DropCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// Vanilla explodes a charged respawn anchor outside the Nether without a source entity, so the game records no
/// cause. The player who used the anchor is recorded around the inner `explode` call and becomes the explosion's
/// cause for drop capture.
@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorBlockMixin {
    @WrapOperation(
            method = "useWithoutItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/RespawnAnchorBlock;explode("
                            + "Lnet/minecraft/world/level/block/state/BlockState;"
                            + "Lnet/minecraft/server/level/ServerLevel;"
                            + "Lnet/minecraft/core/BlockPos;)V"
            )
    )
    private void telekinesis$recordAnchorUser(
            RespawnAnchorBlock anchor,
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            Operation<Void> original,
            @Local(argsOnly = true) Player player
    ) {
        DropCapture.beginBlockExplosion(player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
        try {
            original.call(anchor, state, level, pos);
        } finally {
            DropCapture.endBlockExplosion();
        }
    }
}
