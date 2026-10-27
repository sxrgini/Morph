package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import net.minecraft.resources.Identifier;

/**
 * Active abilities. In Classic mode they are always on and free. In Default (biomass) mode the player toggles them
 * from the Biomass menu / ability key and they drain biomass while they are doing something.
 */
public abstract class Ability<T extends Ability> extends Trait<T>
{
    /** Biomass per second (continuous abilities) or per use (instant abilities). Only used when biomass is in play. */
    public Double useCost;

    /** Whether the player currently has this ability switched on. Synced by the server. */
    public transient boolean enabled = true;

    @Override
    public boolean isAbility()
    {
        return true;
    }

    /** Biomass cost used when the mob's config doesn't set "useCost". */
    protected double defaultCost()
    {
        return 0D;
    }

    public double getCost()
    {
        return useCost == null ? defaultCost() : useCost;
    }

    protected double costPerSecond()
    {
        return getCost();
    }

    /** Spend a one-off cost. */
    protected boolean spendUse()
    {
        return enabled && (costPerSecond() <= 0D || MorphApi.bridge().consumeBiomass(player, costPerSecond()));
    }

    /** Spend one tick of a continuous cost. */
    protected boolean spendTick()
    {
        return enabled && (costPerSecond() <= 0D || MorphApi.bridge().consumeBiomass(player, costPerSecond() / 20D));
    }

    @Override
    public Identifier texIcon()
    {
        return Identifier.fromNamespaceAndPath("morph", "textures/icon/" + iconName() + ".png");
    }

    protected abstract String iconName();
}
