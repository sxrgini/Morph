package com.iiampanda.morph.api.mob.trait;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class ExplosiveImmunityTrait extends Trait<ExplosiveImmunityTrait> implements IEventBusRequired
{
    public ExplosiveImmunityTrait() { type = "traitImmunityExplosive"; }

    @Override public void tick(float strength) {}

    @Override
    public boolean allowDamage(LivingEntity victim, DamageSource source, float amount)
    {
        return !(victim == player && source.is(DamageTypeTags.IS_EXPLOSION));
    }
}
