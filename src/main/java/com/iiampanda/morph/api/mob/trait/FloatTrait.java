package com.iiampanda.morph.api.mob.trait;

import net.minecraft.world.phys.Vec3;

public class FloatTrait extends Trait<FloatTrait>
{
    public FloatTrait() { type = "traitFloat"; }

    @Override
    public void tick(float strength)
    {
        if(strength >= 1F && player.isInWater() && !player.isShiftKeyDown() && !player.getAbilities().flying)
        {
            Vec3 m = player.getDeltaMovement();
            player.setDeltaMovement(m.x, Math.min(m.y + 0.02D, 0.3D), m.z);
        }
    }
}
