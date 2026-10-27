package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.client.render.MorphArm;
import com.iiampanda.morph.client.render.MorphRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin
{
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void morph$render(AbstractClientPlayer player, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light, CallbackInfo ci)
    {
        if(MorphRenderer.render((PlayerRenderer)(Object)this, player, yaw, partial, ps, buffers, light))
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void morph$hideRightHand(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci)
    {
        if(MorphArm.render((PlayerRenderer)(Object)this, ps, buffers, light, player, net.minecraft.world.entity.HumanoidArm.RIGHT))
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void morph$hideLeftHand(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci)
    {
        if(MorphArm.render((PlayerRenderer)(Object)this, ps, buffers, light, player, net.minecraft.world.entity.HumanoidArm.LEFT))
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderRightHand", at = @At("RETURN"))
    private void morph$afterRightHand(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci)
    {
        MorphArm.finish();
    }

    @Inject(method = "renderLeftHand", at = @At("RETURN"))
    private void morph$afterLeftHand(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci)
    {
        MorphArm.finish();
    }
}
