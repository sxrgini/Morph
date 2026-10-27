package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.client.MorphBodies;
import net.minecraft.client.renderer.entity.AxolotlRenderer;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AxolotlRenderer.class)
public abstract class AxolotlRendererMixin
{
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/axolotl/Axolotl;Lnet/minecraft/client/renderer/entity/state/AxolotlRenderState;F)V", at = @At("TAIL"))
    private void morph$tempo(Axolotl entity, AxolotlRenderState state, float partial, CallbackInfo ci)
    {
        state.ageInTicks = MorphBodies.tempo(entity, state.ageInTicks);
    }
}
