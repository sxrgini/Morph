package com.iiampanda.morph.api.mob.trait;

public class SwimmerTrait extends Trait<SwimmerTrait>
{
    public Double swimMultiplier;
    public Double landMultiplier;
    public Boolean doNotAffectFog;

    public SwimmerTrait() { type = "traitSwimmer"; }

    @Override public void tick(float strength) {}

    @Override
    public double getSwimSpeedMultiplier()
    {
        return swimMultiplier == null ? 1D : swimMultiplier;
    }

    public double getLandMultiplier()
    {
        return landMultiplier == null ? 1D : landMultiplier;
    }

    @Override
    public float modifyFogEnd(float end, boolean inWater, boolean inLava)
    {
        if(inWater && (doNotAffectFog == null || !doNotAffectFog))
        {
            return end * (float)Math.max(1D, Math.min(4D, getSwimSpeedMultiplier()));
        }
        return end;
    }
}
