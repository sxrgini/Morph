package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;

/** Creative style flight. */
public class FlyAbility extends Ability<FlyAbility>
{
    public Boolean slowdownInWater;

    public transient boolean grantedFlight;

    public FlyAbility() { type = "abilityFlight"; }

    @Override protected double defaultCost() { return 1.5; }

    @Override protected String iconName() { return "fly"; }

    @Override
    public void tick(float strength)
    {
        if(player.isCreative() || player.isSpectator())
        {
            return;
        }
        Abilities ab = player.getAbilities();
        boolean canFly = strength >= 1F && enabled;
        if(canFly)
        {
            if(!ab.mayfly)
            {
                ab.mayfly = true;
                grantedFlight = true;
                sync();
            }
            if(ab.flying)
            {
                if(!spendTick())
                {
                    ab.flying = false;
                    sync();
                    return;
                }
                if(slowdownInWater != null && slowdownInWater && player.isInWater())
                {
                    Vec3 m = player.getDeltaMovement();
                    player.setDeltaMovement(m.scale(0.8D));
                }
            }
        }
        else
        {
            revoke();
        }
    }

    @Override
    public void removeHooks()
    {
        super.removeHooks();
        revoke();
    }

    private void revoke()
    {
        if(grantedFlight && player != null && !player.isCreative() && !player.isSpectator())
        {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            sync();
        }
        grantedFlight = false;
    }

    private void sync()
    {
        if(player instanceof ServerPlayer sp)
        {
            sp.onUpdateAbilities();
        }
    }
}
