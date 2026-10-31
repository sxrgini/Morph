package com.iiampanda.morph.api.mob.trait;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TraitHooks
{
    private static final List<Trait<?>> ACTIVE = new CopyOnWriteArrayList<>();

    private TraitHooks() {}

    public static void register(Trait<?> trait)
    {
        if(!ACTIVE.contains(trait))
        {
            ACTIVE.add(trait);
        }
    }

    public static void unregister(Trait<?> trait)
    {
        ACTIVE.remove(trait);
    }

    public static List<Trait<?>> active()
    {
        return ACTIVE;
    }

    public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount)
    {
        for(Trait<?> trait : ACTIVE)
        {
            if(!trait.allowDamage(victim, source, amount))
            {
                return false;
            }
        }
        return true;
    }

    public static float modifyDamage(LivingEntity victim, DamageSource source, float amount)
    {
        for(Trait<?> trait : ACTIVE)
        {
            amount = trait.modifyDamage(victim, source, amount);
        }
        return amount;
    }

    public static boolean allowEffect(LivingEntity victim, MobEffectInstance effect)
    {
        for(Trait<?> trait : ACTIVE)
        {
            if(!trait.allowEffect(victim, effect))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean allowTarget(Mob mob, LivingEntity target)
    {
        for(Trait<?> trait : ACTIVE)
        {
            if(!trait.allowTarget(mob, target))
            {
                return false;
            }
        }
        return true;
    }

    public static InteractionResult onInteractEntity(Player interactor, Entity target, InteractionHand hand)
    {
        if(target instanceof Player)
        {
            int forTarget = 0;
            for(Trait<?> trait : ACTIVE)
            {
                if(trait.player == target)
                {
                    forTarget++;
                }
            }
            com.iiampanda.morph.common.Dbg.log("interact {} -> {} hand={} active traits={} for target={}", interactor.getName().getString(), target.getName().getString(), hand, ACTIVE.size(), forTarget);
        }
        for(Trait<?> trait : ACTIVE)
        {
            InteractionResult result = trait.onInteractEntity(interactor, target, hand);
            if(result != InteractionResult.PASS)
            {
                return result;
            }
        }
        return InteractionResult.PASS;
    }
}
