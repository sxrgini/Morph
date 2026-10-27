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

/**
 * Central dispatcher for the events traits can hook into. Fabric has no per-object event bus, so traits that are marked with
 * {@link IEventBusRequired} are kept in this list while they are applied and the relevant Fabric events / mixins call into it.
 */
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

    /** @return false if any active trait wants to nullify this damage */
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

    /** @return false if any active trait wants to deny this effect */
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

    /** @return false if any active trait wants to stop this mob from targeting that entity */
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

    /** @return the first non-PASS result */
    public static InteractionResult onInteractEntity(Player interactor, Entity target, InteractionHand hand)
    {
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
