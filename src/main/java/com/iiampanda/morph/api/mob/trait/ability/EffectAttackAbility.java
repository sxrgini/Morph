package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.mob.trait.IEventBusRequired;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class EffectAttackAbility extends Ability<EffectAttackAbility> implements IEventBusRequired
{
    public String effectId;
    public Integer duration;
    public Integer amplifier;

    public EffectAttackAbility() { type = "abilityEffectAttack"; }

    @Override protected double defaultCost() { return 2.0; }

    @Override protected String iconName() { return "poison_resistance"; }

    @Override public void tick(float strength) {}

    @Override
    public boolean allowDamage(LivingEntity victim, DamageSource source, float amount)
    {
        if(source.getEntity() == player && victim != player && enabled && effectId != null && !player.level().isClientSide)
        {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effectId)).orElse(null);
            if(effect != null && spendUse())
            {
                victim.addEffect(new MobEffectInstance(effect, duration == null ? 100 : duration, amplifier == null ? 0 : amplifier), player);
            }
        }
        return true;
    }
}
