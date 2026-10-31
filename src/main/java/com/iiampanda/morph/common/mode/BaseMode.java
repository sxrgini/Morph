package com.iiampanda.morph.common.mode;

import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.mob.MobProfile;
import com.iiampanda.morph.common.mob.MobProfileManager;
import com.iiampanda.morph.common.morph.MorphManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

abstract class BaseMode implements MorphMode
{

    protected ArrayList<Trait<?>> profileTraits(Player player, MorphVariant variant, boolean useUpgrades)
    {
        ArrayList<Trait<?>> traits = new ArrayList<>();
        LivingEntity sample = variant.createEntity(player.level(), null);
        if(sample == null)
        {
            return traits;
        }
        MobProfile profile = MobProfileManager.getOrCreate(sample);
        ArrayList<String> upgraded = new ArrayList<>();
        for(Trait<?> trait : profile.instantiateTraits())
        {
            if(Morph.configServer.disabledTraits.contains(trait.type))
            {
                continue;
            }
            if(trait.upgradeFor != null)
            {
                if(!useUpgrades)
                {
                    continue;
                }
                upgraded.add(trait.upgradeFor);
            }
            traits.add(trait);
        }
        traits.removeIf(t -> upgraded.contains(t.type));
        return traits;
    }

    protected boolean notMidMorph(Player player)
    {
        MorphInfo info = ((com.iiampanda.morph.api.MorphInfoHolder)player).morph$getMorphInfo();
        return !info.isTransitioning();
    }

    protected boolean allowed(Player player)
    {
        return MorphManager.isPlayerAllowed(player, Morph.configServer.morphFilterType, Morph.configServer.morphFilterNames);
    }

    protected boolean selectorAllowed(Player player)
    {
        return MorphManager.isPlayerAllowed(player, Morph.configServer.selectorFilterType, Morph.configServer.selectorFilterNames);
    }
}
