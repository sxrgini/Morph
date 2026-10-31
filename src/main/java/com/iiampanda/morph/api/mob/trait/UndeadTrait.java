package com.iiampanda.morph.api.mob.trait;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class UndeadTrait extends Trait<UndeadTrait> implements IEventBusRequired
{
    public UndeadTrait() { type = "traitUndead"; }

    @Override public void tick(float strength) {}

    @Override
    public boolean allowEffect(LivingEntity victim, MobEffectInstance effect)
    {
        return true;
    }

    public static boolean onInstantEffect(LivingEntity target, boolean healing, int amplifier)
    {
        if(target.level().isClientSide() || !(target instanceof net.minecraft.world.entity.player.Player p))
        {
            return false;
        }
        for(Trait<?> t : com.iiampanda.morph.api.MorphApi.getMorphInfo(p).traits)
        {
            if(t instanceof UndeadTrait)
            {
                if(healing)
                {
                    target.hurt(target.damageSources().magic(), 6 << amplifier);
                }
                else
                {
                    target.heal(4 << amplifier);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allowDamage(LivingEntity victim, DamageSource source, float amount)
    {
        return true;
    }
}
