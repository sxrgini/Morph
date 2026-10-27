package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.client.render.MorphArm;
import com.iiampanda.morph.client.render.MorphRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin
{
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void morph$extract(Avatar entity, AvatarRenderState state, float partial, CallbackInfo ci)
    {
        MorphRenderer.remember(entity, state, partial);
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void morph$hideRightHand(PoseStack ps, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci)
    {
        if(MorphArm.render((net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?>)(Object)this, ps, collector, light, net.minecraft.world.entity.HumanoidArm.RIGHT))
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void morph$hideLeftHand(PoseStack ps, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci)
    {
        if(MorphArm.render((net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?>)(Object)this, ps, collector, light, net.minecraft.world.entity.HumanoidArm.LEFT))
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderRightHand", at = @At("RETURN"))
    private void morph$afterRightHand(PoseStack ps, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci)
    {
        MorphArm.finish();
    }

    @Inject(method = "renderLeftHand", at = @At("RETURN"))
    private void morph$afterLeftHand(PoseStack ps, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci)
    {
        MorphArm.finish();
    }
}
