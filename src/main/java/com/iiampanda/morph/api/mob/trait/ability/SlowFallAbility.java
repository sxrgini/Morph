package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.world.phys.Vec3;

public class SlowFallAbility extends Ability<SlowFallAbility>
{
    public Double velocityMultiplier;
    public Boolean resetFallDistance;

    public SlowFallAbility() { type = "abilitySlowFall"; }

    @Override protected double defaultCost() { return 0.3; }

    @Override protected String iconName() { return "fall_negate"; }

    @Override
    public void tick(float strength)
    {
        if(strength < 1F || !enabled || player.onGround() || player.isInWater() || player.getAbilities().flying || player.isFallFlying())
        {
            return;
        }
        Vec3 m = player.getDeltaMovement();
        if(m.y < -0.1D && !player.isShiftKeyDown())
        {
            if(!spendTick())
            {
                enabled = false;
                return;
            }
            double mult = velocityMultiplier == null ? 0.6D : velocityMultiplier;
            player.setDeltaMovement(m.x, Math.max(m.y * mult, -0.3D), m.z);
            if(resetFallDistance == null || resetFallDistance)
            {
                player.fallDistance = 0F;
            }
        }
    }
}
