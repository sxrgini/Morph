package com.iiampanda.morph.api.mob.trait;

import net.minecraft.nbt.CompoundTag;

/** Needs to stay wet, like a dolphin. */
public class MoistSkinTrait extends Trait<MoistSkinTrait>
{
    public Integer maxMoistness;

    public transient int moistness = -1;

    public MoistSkinTrait() { type = "traitMoistSkin"; }

    private int max()
    {
        return maxMoistness == null ? 2400 : maxMoistness;
    }

    @Override
    public void tick(float strength)
    {
        if(moistness < 0)
        {
            moistness = max();
        }
        if(strength < 1F)
        {
            return;
        }
        if(player.isInWaterRainOrBubble())
        {
            moistness = max();
        }
        else if(!player.level().isClientSide)
        {
            moistness--;
            if(moistness <= -20)
            {
                moistness = 0;
                if(!player.isCreative())
                {
                    player.hurt(player.damageSources().dryOut(), 1F);
                }
            }
        }
    }

    public int getMoistness() { return moistness < 0 ? max() : moistness; }
    public int getMaxMoistness() { return max(); }
}
