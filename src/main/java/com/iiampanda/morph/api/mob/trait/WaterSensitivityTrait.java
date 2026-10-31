package com.iiampanda.morph.api.mob.trait;

public class WaterSensitivityTrait extends Trait<WaterSensitivityTrait>
{
    public WaterSensitivityTrait() { type = "traitWaterSensitivity"; }

    @Override
    public void tick(float strength)
    {
        if(strength >= 1F && !player.level().isClientSide() && player.isInWaterOrRain() && player.tickCount % 10 == 0 && !player.isCreative())
        {
            player.hurt(player.damageSources().generic(), 1F);
        }
    }
}
