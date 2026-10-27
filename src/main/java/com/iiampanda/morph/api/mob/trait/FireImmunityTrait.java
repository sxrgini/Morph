package com.iiampanda.morph.api.mob.trait;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class FireImmunityTrait extends Trait<FireImmunityTrait> implements IEventBusRequired
{
    public FireImmunityTrait() { type = "traitImmunityFire"; }

    @Override
    public void tick(float strength)
    {
        if(strength >= 1F && player.isOnFire() && !player.level().isClientSide())
        {
            player.clearFire();
        }
    }

    @Override
    public boolean allowDamage(LivingEntity victim, DamageSource source, float amount)
    {
        return !(victim == player && source.is(DamageTypeTags.IS_FIRE));
    }
}
