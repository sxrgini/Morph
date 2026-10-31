package com.iiampanda.morph.common.mode;

import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.biomass.Upgrades;
import com.iiampanda.morph.common.mob.MobProfile;
import com.iiampanda.morph.common.mob.MobProfileManager;
import com.iiampanda.morph.common.mob.VariantFactory;
import com.iiampanda.morph.common.morph.MorphManager;
import com.iiampanda.morph.common.save.PlayerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class DefaultMode extends BaseMode
{
    @Override public String name() { return "default"; }

    @Override public boolean usesBiomass() { return true; }

    @Override public boolean abilitiesNeedActivation() { return true; }

    @Override
    public void onKill(ServerPlayer player, LivingEntity killed)
    {
        if(!MorphManager.hasUnlockedBiomass(player))
        {
            return;
        }
        PlayerData data = MorphManager.data(player);
        boolean gainedBiomass = false;
        if(canAcquireBiomass(player, killed))
        {
            double amount = biomassAmount(data, killed);
            if(amount > 0D)
            {
                MorphManager.addBiomass(player, amount);
                gainedBiomass = true;
            }
        }
        boolean gainedMorph = false;
        if(Upgrades.has(data, Upgrades.ID_MORPH_ABILITY) && allowed(player))
        {
            MorphVariant variant = VariantFactory.create(killed);
            if(variant != null)
            {
                variant = MorphManager.restrictVariant(player, variant);
                if(canAcquire(player, variant) && MorphManager.acquire(player, variant))
                {
                    gainedMorph = true;
                }
            }
        }
        if(gainedBiomass || gainedMorph)
        {
            MorphManager.sendAcquisition(player, killed, gainedMorph, gainedBiomass);
        }
    }

    public boolean canAcquireBiomass(Player player, LivingEntity living)
    {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        for(Pattern p : Morph.configServer.disabledMobsID)
        {
            if(p.matcher(id).matches())
            {
                return false;
            }
        }
        if(!MorphManager.isPlayerAllowed(player, Morph.configServer.biomassFilterType, Morph.configServer.biomassFilterNames))
        {
            return false;
        }
        return true;
    }

    public double mass(LivingEntity living)
    {
        double volume = living.getBbWidth() * living.getBbWidth() * living.getBbHeight();
        double weight = 1000D * volume * Morph.configServer.biomassValue;
        MobProfile profile = MobProfileManager.getOrCreate(living);
        if(profile.biomassMultiplier != null)
        {
            weight *= profile.biomassMultiplier;
        }
        return weight;
    }

    public double biomassAmount(PlayerData data, LivingEntity living)
    {
        double amount = living.getMaxHealth() * Morph.configServer.biomassHealthFraction;
        try
        {
            MobProfile profile = MobProfileManager.getOrCreate(living);
            if(profile.biomassMultiplier != null)
            {
                amount *= profile.biomassMultiplier;
            }
        }
        catch(Throwable ignored) {}
        amount = Math.round(amount * 10D) / 10D;
        return Math.max(0.1D, amount);
    }

    @Override
    public boolean canShowSelector(Player player)
    {
        return selectorAllowed(player) && MorphManager.hasMorphAbility(player);
    }

    @Override
    public boolean canMorph(Player player)
    {
        return allowed(player) && MorphManager.hasMorphAbility(player) && notMidMorph(player);
    }

    @Override
    public boolean canMorphEver(Player player)
    {
        return allowed(player) && MorphManager.hasMorphAbility(player);
    }

    @Override
    public boolean canAcquire(Player player, MorphVariant variant)
    {
        return allowed(player) && !MorphManager.data((ServerPlayer)player).contains(variant);
    }

    @Override
    public ArrayList<Trait<?>> traitsFor(Player player, MorphVariant variant)
    {
        PlayerData data = MorphManager.data((ServerPlayer)player);
        return profileTraits(player, variant, Upgrades.has(data, Upgrades.ID_BIOMASS_ABILITIES));
    }
}
