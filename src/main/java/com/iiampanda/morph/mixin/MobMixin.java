package com.iiampanda.morph.mixin;

import com.iiampanda.morph.api.mob.trait.TraitHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin
{
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void morph$setTarget(LivingEntity target, CallbackInfo ci)
    {
        if(target != null && !TraitHooks.allowTarget((Mob)(Object)this, target))
        {
            ci.cancel();
        }
    }
}
