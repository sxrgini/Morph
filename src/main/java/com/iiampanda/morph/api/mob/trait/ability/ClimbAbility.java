package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import net.minecraft.world.entity.player.Player;

public class ClimbAbility extends Ability<ClimbAbility>
{
    public ClimbAbility() { type = "abilityClimb"; }

    @Override protected double defaultCost() { return 0.5; }

    @Override protected String iconName() { return "climb"; }

    @Override
    public void tick(float strength)
    {
        if(strength >= 1F && enabled && player.horizontalCollision && !player.onGround() && !player.isCreative() && !player.getAbilities().flying)
        {
            if(!spendTick())
            {
                enabled = false;
            }
        }
    }

    public static boolean shouldClimb(Player player)
    {
        if(!player.horizontalCollision || player.getAbilities().flying)
        {
            return false;
        }
        for(Trait<?> trait : MorphApi.getMorphInfo(player).traits)
        {
            if(trait instanceof ClimbAbility climb && climb.enabled)
            {
                return true;
            }
        }
        return false;
    }
}
