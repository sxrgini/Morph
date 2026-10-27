package com.iiampanda.morph.api.mob.trait;

import com.iiampanda.morph.api.MorphApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** The Warden's aura: players near you are plunged into Darkness (unless they carry the same aura). */
public class DarknessAuraTrait extends Trait<DarknessAuraTrait>
{
    public Double radius;      // blocks, default 20
    public Integer duration;   // ticks the effect is applied for, default 260
    public Integer interval;   // ticks between applications, default 40

    public DarknessAuraTrait() { type = "traitDarknessAura"; }

    @Override
    public void tick(float strength)
    {
        if(strength < 1F || player.level().isClientSide || player.isSpectator())
        {
            return;
        }
        int every = interval == null ? 40 : Math.max(1, interval);
        if(player.tickCount % every != 0)
        {
            return;
        }
        double r = radius == null ? 20D : radius;
        for(ServerPlayer other : ((net.minecraft.server.level.ServerLevel)player.level()).players())
        {
            if(other == player || other.isCreative() || other.isSpectator() || other.distanceToSqr(player) > r * r)
            {
                continue;
            }
            boolean immune = false;
            for(Trait<?> t : MorphApi.getMorphInfo(other).traits)
            {
                immune |= t instanceof DarknessAuraTrait;
            }
            if(!immune)
            {
                other.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration == null ? 260 : duration, 0, true, false, false), player);
            }
        }
    }
}
