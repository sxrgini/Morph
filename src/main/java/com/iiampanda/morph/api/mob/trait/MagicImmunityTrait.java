package com.iiampanda.morph.api.mob.trait;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

public class MagicImmunityTrait extends Trait<MagicImmunityTrait> implements IEventBusRequired
{
    public MagicImmunityTrait() { type = "traitImmunityMagic"; }

    @Override public void tick(float strength) {}

    @Override
    public boolean allowDamage(LivingEntity victim, DamageSource source, float amount)
    {
        return !(victim == player && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)));
    }
}
