package com.iiampanda.morph.api.mob.trait;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Swimming near a dolphin makes you swim faster: players nearby get Dolphin's Grace, then it recharges. */
public class DolphinsGraceTrait extends Trait<DolphinsGraceTrait>
{
    public Double radius;        // blocks, default 10
    public Integer duration;     // ticks of Dolphin's Grace given, default 600
    public Integer cooldown;     // ticks until it can be given again, default 12000 (10 minutes)
    public Boolean includeSelf;  // also give it to the dolphin player, default false
    public Boolean onlyInWater;  // only players who are in water, default true

    /** per player: game time at which the grace is ready again (kept here so changing form can't reset it) */
    private static final Map<UUID, Long> READY = new HashMap<>();

    public DolphinsGraceTrait() { type = "traitDolphinsGrace"; }

    @Override
    public void tick(float strength)
    {
        if(strength < 1F || player.level().isClientSide() || player.isSpectator() || player.tickCount % 20 != 0)
        {
            return;
        }
        long now = player.level().getGameTime();
        Long ready = READY.get(player.getUUID());
        if(ready != null && now < ready)
        {
            return;
        }
        double r = radius == null ? 10D : radius;
        boolean water = onlyInWater == null || onlyInWater;
        boolean any = false;
        for(ServerPlayer other : ((net.minecraft.server.level.ServerLevel)player.level()).players())
        {
            if((other == player && (includeSelf == null || !includeSelf)) || other.isSpectator() || other.distanceToSqr(player) > r * r || (water && !other.isInWater()))
            {
                continue;
            }
            other.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, duration == null ? 600 : duration, 0, true, true, true), player);
            any = true;
        }
        if(any)
        {
            READY.put(player.getUUID(), now + (cooldown == null ? 12000 : Math.max(0, cooldown)));
        }
    }
}
