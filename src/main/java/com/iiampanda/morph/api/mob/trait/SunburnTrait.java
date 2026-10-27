package com.iiampanda.morph.api.mob.trait;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Burns in direct daylight unless wearing a helmet. */
public class SunburnTrait extends Trait<SunburnTrait>
{
    public Boolean childrenAreImmune;

    public SunburnTrait() { type = "traitSunburn"; }

    @Override
    public void tick(float strength)
    {
        if(strength < 1F || player.level().isClientSide || player.isCreative() || player.isSpectator())
        {
            return;
        }
        MorphInfo info = MorphApi.getMorphInfo(player);
        if(info.variant != null && info.variant.baby && (childrenAreImmune == null || childrenAreImmune))
        {
            return;
        }
        if(player.level().isDay() && !player.level().isRaining() && !player.isInWaterRainOrBubble()
                && player.level().canSeeSky(player.blockPosition()))
        {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if(!helmet.isEmpty())
            {
                if(helmet.isDamageableItem() && player.getRandom().nextInt(60) == 0)
                {
                    helmet.hurtAndBreak(1, player, EquipmentSlot.HEAD);
                }
                return;
            }
            player.igniteForSeconds(8);
        }
    }
}
