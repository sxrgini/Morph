package com.iiampanda.morph.api.mob.trait;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Blocks a status effect by id, or "*" for every harmful effect. */
public class EffectResistanceTrait extends Trait<EffectResistanceTrait> implements IEventBusRequired
{
    public String effectId;

    public EffectResistanceTrait() { type = "traitEffectResistance"; }

    @Override public void tick(float strength) {}

    @Override
    public boolean allowEffect(LivingEntity victim, MobEffectInstance effect)
    {
        if(victim != player || effectId == null)
        {
            return true;
        }
        if(effectId.equals("*"))
        {
            return effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL;
        }
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        return !(id != null && id.toString().equals(effectId));
    }

    @Override
    public String getTranslationKeyRoot()
    {
        return "morph.trait.traitEffectResistance." + (effectId == null ? "unknown" : effectId.equals("*") ? "all" : effectId.replace(':', '.'));
    }
}
