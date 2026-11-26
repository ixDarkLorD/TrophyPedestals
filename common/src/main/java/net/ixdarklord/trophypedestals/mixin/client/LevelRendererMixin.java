package net.ixdarklord.trophypedestals.mixin.client;

import net.ixdarklord.trophypedestals.client.PedestalLighting;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the pedestals' lighting a block changed, so the light around it is worked out again at once. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "blockChanged", at = @At("HEAD"))
    private void trophypedestals$blockChanged(BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        if (oldState != newState) PedestalLighting.blockChanged(pos);
    }
}
