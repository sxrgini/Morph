package com.iiampanda.morph.api.mob.trait;

import com.iiampanda.morph.api.MorphApi;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class WaterBreatherTrait extends Trait<WaterBreatherTrait>
{
    public Boolean suffocatesOnLand;

    public Boolean nightVisionInWater;

    public Boolean blindnessOnLand;

    private transient boolean gaveNightVision;
    private transient boolean gaveBlindness;

    public WaterBreatherTrait() { type = "traitWaterBreather"; }

    public static boolean blocksAirRegen(Player player)
    {
        if(player.isInWaterOrRain())
        {
            return false;
        }
        for(Trait<?> t : MorphApi.getMorphInfo(player).traits)
        {
            if(t instanceof WaterBreatherTrait wb && wb.suffocatesOnLand != null && wb.suffocatesOnLand)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick(float strength)
    {
        if(strength < 1F || player.isCreative() || player.isSpectator())
        {
            return;
        }
        boolean underwater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        if(nightVisionInWater != null && nightVisionInWater && !player.level().isClientSide())
        {
            if(underwater)
            {
                MobEffectInstance cur = player.getEffect(MobEffects.NIGHT_VISION);
                if(cur == null || (gaveNightVision && cur.getDuration() < 260))
                {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
                    gaveNightVision = true;
                }
            }
            else if(gaveNightVision)
            {
                MobEffectInstance cur = player.getEffect(MobEffects.NIGHT_VISION);
                if(cur != null && cur.isAmbient() && !cur.isVisible() && cur.getDuration() <= 400)
                {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                }
                gaveNightVision = false;
            }
        }
        boolean blind = blindnessOnLand != null ? blindnessOnLand : (suffocatesOnLand != null && suffocatesOnLand);
        if(blind && !player.level().isClientSide())
        {
            MobEffectInstance cur = player.getEffect(MobEffects.BLINDNESS);
            if(!player.isInWaterOrRain())
            {
                if(cur == null || (gaveBlindness && cur.getDuration() < 30))
                {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false, false));
                    gaveBlindness = true;
                }
            }
            else if(gaveBlindness)
            {
                if(cur != null && cur.isAmbient() && !cur.isVisible() && cur.getDuration() <= 60)
                {
                    player.removeEffect(MobEffects.BLINDNESS);
                }
                gaveBlindness = false;
            }
        }
        if(underwater)
        {
            player.setAirSupply(player.getMaxAirSupply());
        }
        else if(suffocatesOnLand != null && suffocatesOnLand && !player.level().isClientSide())
        {
            if(player.isInWaterOrRain())
            {
                player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 4));
            }
            else
            {
                int air = player.getAirSupply() - 1;
                if(air <= -20)
                {
                    air = 0;
                    player.hurt(player.damageSources().drown(), 2F);
                }
                player.setAirSupply(air);
            }
        }
    }

    @Override
    public boolean hideAirBubbles()
    {
        return suffocatesOnLand == null || !suffocatesOnLand;
    }
}
