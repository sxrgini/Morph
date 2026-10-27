package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.MorphInfoHolder;
import com.iiampanda.morph.api.mob.trait.Trait;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

/** Frog Hop: hold the jump key to charge a bigger jump, let go to leap (like a horse). */
public class SpecialFrogHopAbility extends Ability<SpecialFrogHopAbility>
{
    public Double maxHeight;    // blocks cleared by a fully charged hop, default 2
    public Integer chargeTicks; // ticks of holding jump for a full charge, default 20

    public SpecialFrogHopAbility() { type = "abilitySpecialFrogHop"; }

    @Override protected String iconName() { return "step"; }

    @Override
    public ResourceLocation texIcon()
    {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/jump_boost.png");
    }

    public double height() { return maxHeight == null ? 2D : Math.max(1.25D, maxHeight); }
    public int fullCharge() { return chargeTicks == null ? 20 : Math.max(2, chargeTicks); }

    @Override
    public void tick(float strength)
    {
        if(strength >= 1F && player.level().isClientSide())
        {
            com.iiampanda.morph.client.FrogHopClient.tick(player, this);
        }
    }

    @Override
    public void removeHooks()
    {
        super.removeHooks();
        if(player != null && player.level().isClientSide())
        {
            com.iiampanda.morph.client.FrogHopClient.reset(player);
        }
    }

    /** The frog hop of a morphed player, if switched on. */
    public static SpecialFrogHopAbility of(Player p)
    {
        var info = ((MorphInfoHolder)p).morph$getMorphInfo();
        if(info == null || !info.isMorphed() || info.isTransitioning())
        {
            return null;
        }
        for(Trait<?> t : info.traits)
        {
            if(t instanceof SpecialFrogHopAbility f && f.enabled)
            {
                return f;
            }
        }
        return null;
    }

    /** True when the normal jump has to be held back so the charge can build up (client side, on the ground). */
    public static boolean holdsJump(Player p)
    {
        return p.level().isClientSide() && p.onGround() && !p.isInWater() && !p.isPassenger() && of(p) != null
                && com.iiampanda.morph.client.FrogHopClient.isLocal(p);
    }
}
