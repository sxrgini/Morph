package com.iiampanda.morph.api.mob.trait;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class DamageSourceImmunityTrait extends Trait<DamageSourceImmunityTrait> implements IEventBusRequired
{
    public String damageType;

    public DamageSourceImmunityTrait() { type = "traitImmunityDamageSource"; }

    @Override public void tick(float strength) {}

    @Override
    public String getTranslationKeyRoot()
    {
        String key = "morph.trait.traitImmunityDamageSource." + (damageType == null ? "unknown" : damageType.replace(':', '.'));
        return net.minecraft.locale.Language.getInstance().has(key + ".name") ? key : super.getTranslationKeyRoot();
    }

    @Override
    public boolean allowDamage(LivingEntity victim, DamageSource source, float amount)
    {
        if(victim != player || damageType == null)
        {
            return true;
        }
        String id = damageType.contains(":") ? damageType.substring(damageType.indexOf(':') + 1) : damageType;
        return !(source.getMsgId().equals(id) || source.typeHolder().unwrapKey().map(k -> k.location().getPath().equals(id)).orElse(false));
    }
}
