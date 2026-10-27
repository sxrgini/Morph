package com.iiampanda.morph.api.mob.trait;

import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class Trait<T extends Trait> implements Comparable<T>
{
    @NotNull
    public String type;
    public String upgradeFor; //the trait type this trait is a direct upgrade of
    public Double purchaseCost; //biomass cost in the default mode to unlock this trait
    public Boolean hidden;

    public transient Player player;
    public transient ArrayList<Trait<?>> stateTraits;
    @Nullable
    public transient LivingEntity livingInstance;

    //also used to set default values if not set in the JSON
    public void addHooks()
    {
        if(this instanceof IEventBusRequired)
        {
            TraitHooks.register(this);
        }
    }

    /** The player is no longer morphed into this (called once, on the server and the client, when the morph changes; not when the player merely leaves the world). */
    public void onUnmorph() {}

    public void removeHooks()
    {
        if(this instanceof IEventBusRequired)
        {
            TraitHooks.unregister(this);
        }
    }

    public abstract void tick(float strength); //Strength ranges 0 - 1F

    public void transitionalTick(T prevTrait, float transitionProgress)
    {
        this.tick(1F); // by default, no transitional state, just tick at max strength
    }

    public void doTick(float strength)
    {
        this.tick(strength);
    }

    public void doTransitionalTick(T prevTrait, float transitionProgress)
    {
        this.transitionalTick(prevTrait, transitionProgress);
    }

    public boolean canTransitionTo(Trait<?> trait)
    {
        return this.getClass().equals(trait.getClass());
    }

    @SuppressWarnings("unchecked")
    public T copy()
    {
        return (T)TraitHandler.copy(this);
    }

    //EVENT HOOKS. Only called if the trait implements IEventBusRequired and is currently applied.
    public boolean allowDamage(LivingEntity victim, DamageSource source, float amount)
    {
        return true;
    }

    /** Scales damage the victim is about to take (return the new amount). */
    public float modifyDamage(LivingEntity victim, DamageSource source, float amount)
    {
        return amount;
    }

    public boolean allowEffect(LivingEntity victim, MobEffectInstance effect)
    {
        return true;
    }

    public boolean allowTarget(Mob mob, LivingEntity target)
    {
        return true;
    }

    public InteractionResult onInteractEntity(Player interactor, Entity target, InteractionHand hand)
    {
        return InteractionResult.PASS;
    }

    //MOVEMENT / RENDER MODIFIERS. Always queried for the active trait list of the morphed player, no IEventBusRequired needed.
    public double getSwimSpeedMultiplier()
    {
        return 1D;
    }

    public float modifyFogEnd(float end, boolean inWater, boolean inLava)
    {
        return end;
    }

    public float modifyFogStart(float start, boolean inWater, boolean inLava)
    {
        return start;
    }

    public float modifyWaterFogColourScale()
    {
        return 1F;
    }

    public int modifyDisplayedAir(int air)
    {
        return air;
    }

    public boolean hideAirBubbles()
    {
        return false;
    }

    //IN-GAME INFO
    public String getTranslationKeyRoot()
    {
        return "morph.trait." + type;
    }

    public boolean isHidden()
    {
        return hidden != null && hidden;
    }

    private static final java.util.Map<String, String> ICONS = java.util.Map.ofEntries(
            java.util.Map.entry("traitFallNegate", "fall_negate"), java.util.Map.entry("traitFloat", "float"),
            java.util.Map.entry("traitHostile", "hostile"), java.util.Map.entry("traitImmunityFire", "fire_immunity"),
            java.util.Map.entry("traitSink", "sink"), java.util.Map.entry("traitWaterSensitivity", "water_allergy"),
            java.util.Map.entry("traitStepHeight", "step"), java.util.Map.entry("traitSwimmer", "swim"),
            java.util.Map.entry("traitSunburn", "sunburn"), java.util.Map.entry("traitIntimidate", "fear"),
            java.util.Map.entry("traitEffectResistance", "poison_resistance"), java.util.Map.entry("traitImmunityMagic", "wither_resistance"));

    private static final java.util.Map<String, String> EFFECT_ICONS = java.util.Map.ofEntries(
            java.util.Map.entry("traitFullBright", "night_vision"),
            java.util.Map.entry("traitDarknessAura", "darkness"),
            java.util.Map.entry("traitWaterBreather", "water_breathing"),
            java.util.Map.entry("traitImmunityExplosive", "resistance"),
            java.util.Map.entry("traitImmunityDamageSource", "resistance"),
            java.util.Map.entry("traitEmissive", "glowing"),
            java.util.Map.entry("traitDolphinLeap", "jump_boost"), java.util.Map.entry("traitDolphinsGrace", "dolphins_grace"),
            java.util.Map.entry("traitMoistSkin", "water_breathing"));

    public Identifier texIcon()
    {
        String icon = ICONS.get(type);
        if(icon == null)
        {
            String eff = EFFECT_ICONS.get(type);
            return eff == null ? null : Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/" + eff + ".png");
        }
        return Identifier.fromNamespaceAndPath("morph", "textures/icon/" + icon + ".png");
    }
    //END IN-GAME INFO

    public boolean isAbility()
    {
        return false;
    }

    @Override
    public int compareTo(T o)
    {
        if(!this.isAbility() && o.isAbility())
        {
            //we're before
            return -1;
        }
        else if(this.isAbility() && !o.isAbility())
        {
            //we're after
            return 1;
        }
        return Language.getInstance().getOrDefault(getTranslationKeyRoot() + ".name").compareTo(Language.getInstance().getOrDefault(o.getTranslationKeyRoot() + ".name"));
    }
}
