package com.iiampanda.morph.api.morph;

import com.iiampanda.morph.api.MorphEntityAccess;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.ability.Ability;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MorphInfo
{
    public final Player player;

    @Nullable public MorphVariant variant;
    @Nullable public MorphVariant prevVariant;

    public int transition = 1;
    public int transitionMax = 1;

    public final ArrayList<Trait<?>> traits = new ArrayList<>();

    @Nullable private LivingEntity entity;
    @Nullable private LivingEntity prevEntity;
    @Nullable private EntityDimensions dims;

    public MorphInfo(Player player)
    {
        this.player = player;
    }

    public boolean isMorphed()
    {
        return variant != null;
    }

    public boolean isTransitioning()
    {
        return transition < transitionMax;
    }

    public float getProgress(float partialTick)
    {
        if(transition >= transitionMax)
        {
            return 1F;
        }
        return Math.min(1F, (transition + partialTick) / (float)transitionMax);
    }

    public void set(@Nullable MorphVariant newVariant, List<Trait<?>> newTraits, int duration, boolean animate)
    {
        for(Trait<?> trait : traits)
        {
            trait.removeHooks();
        }
        traits.clear();

        if(animate)
        {
            prevVariant = variant;
            prevEntity = entity;
            transition = 0;
            transitionMax = Math.max(1, duration);
        }
        else
        {
            prevVariant = null;
            prevEntity = null;
            transition = transitionMax = 1;
        }
        if(!animate || newVariant != null || variant == null)
        {

        }
        variant = newVariant;
        entity = null;
        dims = null;

        if(newVariant != null)
        {
            for(Trait<?> trait : newTraits)
            {
                trait.player = player;
                trait.stateTraits = traits;
                traits.add(trait);
            }
            for(Trait<?> trait : traits)
            {
                trait.addHooks();
            }
        }
        if(newVariant != null)
        {
            StringBuilder names = new StringBuilder();
            for(Trait<?> trait : traits)
            {
                names.append(trait.type).append(' ');
            }
            com.iiampanda.morph.common.Dbg.log("{} morphed, traits: {}", player.getName().getString(), names);
        }
        player.refreshDimensions();
    }

    public void detach()
    {
        for(Trait<?> trait : traits)
        {
            trait.removeHooks();
        }
    }

    public void tick()
    {
        if(transition < transitionMax)
        {
            transition++;
            if(transition >= transitionMax)
            {
                prevVariant = null;
                prevEntity = null;
            }
        }
        if(variant != null)
        {
            float strength = getProgress(1F);
            for(Trait<?> trait : traits)
            {
                trait.livingInstance = getEntity();
                trait.doTick(strength);
            }
        }
    }

    public void setAbilityEnabled(String type, boolean enabled)
    {
        for(Trait<?> trait : traits)
        {
            if(trait instanceof Ability<?> ability && ability.type.equals(type))
            {
                ability.enabled = enabled;
            }
        }
    }

    public void resetEntity()
    {
        entity = null;
    }

    @Nullable
    public LivingEntity getEntity()
    {
        if(entity == null && variant != null)
        {
            entity = variant.createEntity(player.level(), player.getUUID());
        }
        return entity;
    }

    @Nullable
    public LivingEntity getPrevEntity()
    {
        if(prevEntity == null && prevVariant != null)
        {
            prevEntity = prevVariant.createEntity(player.level(), player.getUUID());
        }
        return prevEntity;
    }

    @Nullable
    public EntityDimensions getDimensions(Pose pose)
    {
        if(variant == null)
        {
            return null;
        }
        if(dims == null)
        {
            LivingEntity e = getEntity();
            if(e == null)
            {
                return null;
            }
            dims = e.getDimensions(Pose.STANDING);
            if(com.iiampanda.morph.common.Morph.configServer != null)
            {
                float maxW = com.iiampanda.morph.common.Morph.configServer.maxHitboxWidth;
                float maxH = com.iiampanda.morph.common.Morph.configServer.maxHitboxHeight;
                if(dims.width() > maxW || dims.height() > maxH)
                {
                    float w = Math.min(dims.width(), maxW);
                    float h = Math.min(dims.height(), maxH);
                    dims = EntityDimensions.scalable(w, h).withEyeHeight(Math.min(dims.eyeHeight(), h * 0.9F));
                }
            }
        }
        if(pose == Pose.SLEEPING || pose == Pose.DYING)
        {
            return null;
        }
        if(pose == Pose.CROUCHING)
        {
            return dims.withEyeHeight(dims.eyeHeight() * 0.93F);
        }
        return dims;
    }
}
