package com.iiampanda.morph.mixin;

import com.iiampanda.morph.api.MorphInfoHolder;
import com.iiampanda.morph.api.morph.MorphInfo;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Avatar.class)
public abstract class AvatarMixin
{
    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void morph$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir)
    {
        if((Object)this instanceof Player player)
        {
            MorphInfo info = ((MorphInfoHolder)player).morph$peekMorphInfo();
            EntityDimensions dims = info == null ? null : info.getDimensions(pose);
            if(dims != null)
            {
                cir.setReturnValue(dims);
            }
        }
    }
}
