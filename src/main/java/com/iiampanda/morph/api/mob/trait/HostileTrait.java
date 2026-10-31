package com.iiampanda.morph.api.mob.trait;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

public class HostileTrait extends Trait<HostileTrait> implements IEventBusRequired
{
    public HostileTrait() { type = "traitHostile"; }

    @Override
    public void tick(float strength) {}

    @Override
    public boolean allowTarget(Mob mob, LivingEntity target)
    {
        return !(target == player && mob instanceof Enemy && mob.getLastHurtByMob() != player);
    }
}
