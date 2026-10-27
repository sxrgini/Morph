package com.iiampanda.morph.api.mob.trait;

public class FallNegateTrait extends Trait<FallNegateTrait>
{
    public FallNegateTrait() { type = "traitFallNegate"; }

    @Override
    public void tick(float strength)
    {
        if(strength >= 1F)
        {
            player.fallDistance = 0F;
        }
    }

    @Override public String toString() { return type; }
}
