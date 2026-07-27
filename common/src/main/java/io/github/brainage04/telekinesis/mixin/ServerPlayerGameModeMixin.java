package io.github.brainage04.telekinesis.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.brainage04.telekinesis.drop.BlockDropCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @WrapMethod(method = "destroyBlock")
    private boolean telekinesis$captureBlockDrops(BlockPos pos, Operation<Boolean> original) {
        BlockDropCapture.begin(this.player);
        try {
            return original.call(pos);
        } finally {
            BlockDropCapture.end();
        }
    }
}
