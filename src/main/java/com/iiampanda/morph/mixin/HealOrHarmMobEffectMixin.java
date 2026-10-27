package com.iiampanda.morph.mixin;

import com.iiampanda.morph.api.mob.trait.UndeadTrait;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class HealOrHarmMobEffectMixin
{
    @Inject(method = "applyInstantenousEffect", at = @At("HEAD"), cancellable = true)
    private void morph$undead(Entity source, Entity indirect, LivingEntity target, int amplifier, double scale, CallbackInfo ci)
    {
        boolean healing = (Object)this == MobEffects.HEAL.value();
        if(UndeadTrait.onInstantEffect(target, healing, amplifier))
        {
            ci.cancel();
        }
    }
}
