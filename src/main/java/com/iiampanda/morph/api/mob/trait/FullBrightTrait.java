package com.iiampanda.morph.api.mob.trait;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Sees in the dark (night vision, without the potion particles). */
public class FullBrightTrait extends Trait<FullBrightTrait>
{
    public Boolean showIcon;

    public FullBrightTrait() { type = "traitFullBright"; }

    @Override
    public void tick(float strength)
    {
        if(strength >= 1F && !player.level().isClientSide && player.tickCount % 20 == 0)
        {
            MobEffectInstance cur = player.getEffect(MobEffects.NIGHT_VISION);
            if(cur == null || cur.getDuration() < 260)
            {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, showIcon != null && showIcon));
            }
        }
    }

    @Override
    public void removeHooks()
    {
        super.removeHooks();
        if(player != null && !player.level().isClientSide)
        {
            MobEffectInstance cur = player.getEffect(MobEffects.NIGHT_VISION);
            if(cur != null && cur.isAmbient() && !cur.isVisible() && cur.getDuration() <= 400)
            {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
}
