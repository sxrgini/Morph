package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.world.phys.Vec3;

/** Bat/bee style flight: tap jump in mid-air to flap upwards. */
public class FlightFlapAbility extends Ability<FlightFlapAbility>
{
    public Boolean resetVerticalVelocity;
    public Double velocityAdded;
    public Integer flapLimit;
    public Boolean slowdownInWater;

    public transient int flaps;
    public transient boolean keyHeld;

    public FlightFlapAbility() { type = "abilityFlightFlap"; }

    @Override protected double defaultCost() { return 3.0; }

    @Override protected String iconName() { return "fly"; }

    @Override
    public void tick(float strength)
    {
        if(strength < 1F || player.isCreative() || player.isSpectator() || player.getAbilities().flying)
        {
            return;
        }
        if(player.onGround() || player.isInWater() || player.onClimbable())
        {
            flaps = 0;
            keyHeld = ((com.iiampanda.morph.mixin.LivingEntityInvoker)player).morph$isJumping();
            return;
        }
        boolean pressed = ((com.iiampanda.morph.mixin.LivingEntityInvoker)player).morph$isJumping();
        if(pressed && !keyHeld && enabled)
        {
            int limit = flapLimit == null ? 6 : flapLimit;
            if((limit <= 0 || flaps < limit) && spendUse())
            {
                flaps++;
                Vec3 m = player.getDeltaMovement();
                double y = (resetVerticalVelocity == null || resetVerticalVelocity) ? 0D : m.y;
                y = Math.max(y, 0D) + (velocityAdded == null ? 0.38D : velocityAdded);
                player.setDeltaMovement(m.x, Math.min(y, 0.7D), m.z);
                player.fallDistance = 0F;
                player.needsSync = true;
            }
        }
        keyHeld = pressed;
    }

    /** Used by the server to avoid kicking flapping players for "flying". */
    public boolean isActive()
    {
        return enabled;
    }
}
