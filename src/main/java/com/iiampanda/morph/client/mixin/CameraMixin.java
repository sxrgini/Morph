package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.api.MorphApi;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin
{
    /** Tiny morphs sit so close to the ground that the camera's collision check hits the floor and pulls the camera into the model. */
    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void morph$tinyZoom(float dist, CallbackInfoReturnable<Float> cir)
    {
        Entity e = ((Camera)(Object)this).getEntity();
        if(e instanceof Player p && p.getEyeHeight() < 0.7F && MorphApi.getMorphInfo(p).isMorphed())
        {
            cir.setReturnValue(dist);
        }
    }
}
