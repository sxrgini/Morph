package com.iiampanda.morph.api.mob.trait;

import net.minecraft.world.phys.Vec3;

public class SinkTrait extends Trait<SinkTrait>
{
    public SinkTrait() { type = "traitSink"; }

    @Override
    public void tick(float strength)
    {
        if(strength >= 1F && player.isInWater() && !player.onGround() && !player.getAbilities().flying)
        {
            Vec3 m = player.getDeltaMovement();
            player.setDeltaMovement(m.x, m.y - (((com.iiampanda.morph.mixin.LivingEntityInvoker)player).morph$isJumping() ? 0.0D : 0.03D), m.z);
        }
    }
}
