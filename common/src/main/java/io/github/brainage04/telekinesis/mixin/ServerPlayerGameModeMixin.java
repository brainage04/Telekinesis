package io.github.brainage04.telekinesis.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.brainage04.brainagelib.drops.LeafOwners;
import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/// The block-break scope itself lives in BrainageLib's shared capture engine. What belongs to this
/// mod is the tree: a log a Telekinesis player breaks hands the leaves it held up to them, so the
/// ones that decay later pay the player who cut the tree instead of dropping where nobody sees
/// them.
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @WrapMethod(method = "destroyBlock")
    private boolean telekinesis$rememberFelledTree(BlockPos pos, Operation<Boolean> original) {
        ServerLevel level = (ServerLevel) this.player.level();
        BlockState state = level.getBlockState(pos);
        boolean broken = original.call(pos);
        if (broken
                && state.is(BlockTags.LOGS)
                && TelekinesisPlayerSettings.isEffective(this.player)) {
            LeafOwners.felled(this.player, level, List.of(pos.immutable()));
        }
        return broken;
    }
}
