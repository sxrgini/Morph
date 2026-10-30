package com.iiampanda.morph.mixin;

import com.iiampanda.morph.common.morph.RideBypass;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin
{
    @Inject(method = "canSerialize", at = @At("HEAD"), cancellable = true)
    private void morph$playerMount(CallbackInfoReturnable<Boolean> cir)
    {
        if(RideBypass.ACTIVE.get() && (Object)this == net.minecraft.world.entity.EntityTypes.PLAYER)
        {
            cir.setReturnValue(true);
        }
    }
}
