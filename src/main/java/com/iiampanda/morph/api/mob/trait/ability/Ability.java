package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import net.minecraft.resources.Identifier;

public abstract class Ability<T extends Ability> extends Trait<T>
{

    public Double useCost;

    public transient boolean enabled = true;

    @Override
    public boolean isAbility()
    {
        return true;
    }

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

    protected boolean spendUse()
    {
        return enabled && (costPerSecond() <= 0D || MorphApi.bridge().consumeBiomass(player, costPerSecond()));
    }

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
