package com.iiampanda.morph.common.morph;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.TraitHandler;
import com.iiampanda.morph.api.mob.trait.ability.Ability;
import com.iiampanda.morph.api.morph.AttributeConfig;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.biomass.Upgrades;
import com.iiampanda.morph.common.config.ConfigServer;
import com.iiampanda.morph.common.mode.MorphMode;
import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.common.save.MorphSavedData;
import com.iiampanda.morph.common.save.PlayerData;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;
import java.util.*;

public final class MorphManager
{
    private static final Set<UUID> BIOMASS_DIRTY = new HashSet<>();

    private MorphManager() {}

    public static MorphMode mode()
    {
        return Morph.mode;
    }

    public static PlayerData data(ServerPlayer player)
    {
        return MorphSavedData.get(player.level().getServer()).get(player.getUUID());
    }

    public static boolean isPlayerAllowed(Player player, ConfigServer.FilterType type, List<String> names)
    {
        boolean listed = false;
        for(String n : names)
        {
            if(n.equalsIgnoreCase(player.getGameProfile().name()))
            {
                listed = true;
                break;
            }
        }
        return type == ConfigServer.FilterType.ALLOW ? listed : !listed;
    }

    public static boolean hasMorphAbility(Player player)
    {
        return !mode().usesBiomass() || (player instanceof ServerPlayer sp && Upgrades.has(data(sp), Upgrades.ID_MORPH_ABILITY));
    }

    public static boolean hasUnlockedBiomass(ServerPlayer player)
    {
        if(!mode().usesBiomass() || !isPlayerAllowed(player, Morph.configServer.biomassFilterType, Morph.configServer.biomassFilterNames))
        {
            return false;
        }
        if(Morph.configServer.biomassBypassAdvancement)
        {
            return true;
        }
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("morph", "morph/unlock_biomass"));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    public static boolean hasAbilityControl(ServerPlayer player)
    {
        return mode().usesBiomass() && Upgrades.has(data(player), Upgrades.ID_BIOMASS_ABILITIES);
    }

    public static MorphVariant restrictVariant(ServerPlayer player, MorphVariant variant)
    {
        boolean baby = variant.baby;
        boolean variants = !variant.nbt.isEmpty();
        boolean allowBaby = Morph.configServer.allowBabyForms;
        boolean allowVariants = Morph.configServer.allowVariants;
        if(mode().usesBiomass() && !Upgrades.has(data(player), Upgrades.ID_MORPH_VARIANTS))
        {
            allowBaby = false;
            allowVariants = false;
        }
        return new MorphVariant(variant.type, allowVariants || !variants ? variant.nbt : new net.minecraft.nbt.CompoundTag(), baby && allowBaby);
    }

    public static boolean acquire(ServerPlayer player, MorphVariant variant)
    {
        PlayerData data = data(player);
        if(data.contains(variant))
        {
            return false;
        }
        data.morphs.add(variant);
        sendData(player);
        return true;
    }

    public static boolean forget(ServerPlayer player, MorphVariant variant)
    {
        PlayerData data = data(player);
        boolean removed = data.morphs.removeIf(m -> m.equals(variant));
        data.favourites.remove(variant.key());
        if(removed)
        {
            if(data.current != null && data.current.equals(variant) && !Morph.configServer.morphMode.equals(ConfigServer.ModeType.DISGUISE))
            {
                morph(player, null, true);
            }
            sendData(player);
        }
        return removed;
    }

    public static boolean morph(ServerPlayer player, @Nullable MorphVariant variant, boolean force)
    {
        SitState.set(player, false);
        PlayerData data = data(player);
        if(!force)
        {
            if(!mode().canMorph(player))
            {
                return false;
            }
            if(variant != null && !mode().owns(player, variant))
            {
                return false;
            }
        }
        MorphInfo info = MorphApi.getMorphInfo(player);
        if(variant == null && !info.isMorphed())
        {
            return false;
        }
        if(!force && variant == null && !mode().canDemorph(player))
        {
            return false;
        }
        if(variant != null && info.isMorphed() && variant.equals(info.variant))
        {
            return false;
        }
        data.current = variant;
        if(variant != null)
        {
            data.last = variant;
        }
        applyMorph(player, variant, true);
        mode().onMorphed(player, variant);
        if(!Morph.configServer.silentMorphs && Morph.MORPH_SOUND != null)
        {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), Morph.MORPH_SOUND, SoundSource.PLAYERS, 1F, 1F);
        }
        return true;
    }

    public static void applyMorph(ServerPlayer player, @Nullable MorphVariant variant, boolean animate)
    {
        MorphInfo info = MorphApi.getMorphInfo(player);
        ArrayList<Trait<?>> traits = variant == null ? new ArrayList<>() : mode().traitsFor(player, variant);
        int duration = Morph.configServer.morphTime;
        info.set(variant, traits, duration, animate);
        refreshAbilityFlags(player);
        updateAttributes(player);
        syncMorph(player, null, duration, animate);
        com.iiampanda.morph.common.MorphRules.sync(player);
    }

    public static void refreshAbilityFlags(ServerPlayer player)
    {
        MorphInfo info = MorphApi.getMorphInfo(player);
        PlayerData data = data(player);
        boolean needsActivation = mode().abilitiesNeedActivation();
        boolean control = hasAbilityControl(player);
        for(Trait<?> trait : info.traits)
        {
            if(trait instanceof Ability<?> ability)
            {
                ability.enabled = !needsActivation || (control && data.enabledAbilities.contains(ability.type));
            }
        }
    }

    public static void toggleAbility(ServerPlayer player, String type)
    {
        if(!hasAbilityControl(player))
        {
            return;
        }
        MorphInfo info = MorphApi.getMorphInfo(player);
        boolean has = false;
        for(Trait<?> t : info.traits)
        {
            has |= t instanceof Ability<?> && t.type.equals(type);
        }
        if(!has)
        {
            return;
        }
        PlayerData data = data(player);
        if(!data.enabledAbilities.remove(type))
        {
            data.enabledAbilities.add(type);
        }
        refreshAbilityFlags(player);
        syncMorph(player, null, 1, false);
        sendData(player);
    }

    public static void addBiomass(ServerPlayer player, double amount)
    {
        PlayerData data = data(player);
        data.biomass = Math.min(Upgrades.maxBiomass(data), data.biomass + amount);
        BIOMASS_DIRTY.add(player.getUUID());
    }

    public static boolean consumeBiomass(ServerPlayer player, double amount)
    {
        if(!mode().usesBiomass())
        {
            return true;
        }
        PlayerData data = data(player);
        double cost = amount * (1D - Math.min(0.9D, Upgrades.value(data, Upgrades.ID_ABILITY_EFFICIENCY)));
        if(data.biomass < cost)
        {
            return false;
        }
        data.biomass -= cost;
        BIOMASS_DIRTY.add(player.getUUID());
        return true;
    }

    public static void purchase(ServerPlayer player, String id)
    {
        if(!mode().usesBiomass() || !hasUnlockedBiomass(player))
        {
            return;
        }
        PlayerData data = data(player);
        if(Upgrades.purchase(data, id))
        {
            if(mode().usesBiomass())
            {

                MorphInfo info = MorphApi.getMorphInfo(player);
                if(info.isMorphed() && info.variant != null)
                {
                    applyMorph(player, info.variant, false);
                }
            }
            sendData(player);
        }
    }

    public static void tickDirtyBiomass(net.minecraft.server.MinecraftServer server)
    {
        if(BIOMASS_DIRTY.isEmpty())
        {
            return;
        }
        for(UUID id : BIOMASS_DIRTY)
        {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if(p != null)
            {
                Platform.sendToPlayer(p, new Packets.SyncBiomass(data(p).biomass));
            }
        }
        BIOMASS_DIRTY.clear();
    }

    private static Identifier modId(Identifier attr)
    {
        return Identifier.fromNamespaceAndPath("morph", "attr_" + attr.getNamespace() + "_" + attr.getPath().replace('.', '_'));
    }

    public static void updateAttributes(ServerPlayer player)
    {
        MorphInfo info = MorphApi.getMorphInfo(player);
        LivingEntity mob = info.getEntity();
        float healthRatio = player.getHealth() / Math.max(1F, player.getMaxHealth());
        for(Map.Entry<Identifier, AttributeConfig> e : Morph.configServer.supportedAttributesMap.entrySet())
        {
            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.get(e.getKey());
            if(holder.isEmpty())
            {
                continue;
            }
            AttributeInstance inst = player.getAttribute(holder.get());
            if(inst == null)
            {
                continue;
            }
            Identifier mid = modId(e.getKey());
            inst.removeModifier(mid);
            if(mob == null)
            {
                continue;
            }
            AttributeInstance mobInst = mob.getAttribute(holder.get());
            if(mobInst == null)
            {
                continue;
            }
            double target = mobInst.getBaseValue();
            AttributeConfig cfg = e.getValue();
            if(cfg.cap != null)
            {
                target = cfg.moreIsBetter ? Math.min(target, cfg.cap) : Math.max(target, cfg.cap);
            }
            if(target <= 0D)
            {
                continue;
            }
            double diff = target - inst.getBaseValue();
            if(Math.abs(diff) > 1.0E-6D)
            {
                inst.addTransientModifier(new AttributeModifier(mid, diff, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        player.setHealth(Math.max(1F, Math.min(player.getMaxHealth(), player.getMaxHealth() * healthRatio)));
    }

    public static void syncMorph(ServerPlayer player, @Nullable ServerPlayer onlyTo, int duration, boolean animate)
    {
        MorphInfo info = MorphApi.getMorphInfo(player);
        List<String> enabled = new ArrayList<>();
        for(Trait<?> t : info.traits)
        {
            if(t instanceof Ability<?> a && a.enabled)
            {
                enabled.add(a.type);
            }
        }
        Packets.SyncMorph payload = new Packets.SyncMorph(player.getUUID(), info.variant == null ? Optional.empty() : Optional.of(info.variant.write()),
                TraitHandler.toJson(info.traits), duration, animate, enabled);
        if(onlyTo != null)
        {
            Platform.sendToPlayer(onlyTo, payload);
            SitState.resend(player, onlyTo);
            return;
        }
        Platform.sendToPlayer(player, payload);
        Platform.sendToTracking(player, payload);
    }

    public static void sendData(ServerPlayer player)
    {
        PlayerData data = data(player);
        Platform.sendToPlayer(player, new Packets.SyncData(data.write(), Upgrades.toJson(), mode().name(),
                mode().canShowSelector(player), mode().canMorphEver(player), hasUnlockedBiomass(player), hasAbilityControl(player)));
    }

    public static void sendAcquisition(ServerPlayer player, LivingEntity killed, boolean morph, boolean biomass)
    {
        sendData(player);
        Platform.sendToPlayer(player, new Packets.Acquire(killed.getX(), killed.getY(), killed.getZ(), killed.getBbWidth(), killed.getBbHeight(), morph, biomass));
    }

    public static void onJoin(ServerPlayer player)
    {
        mode().onJoin(player);
        PlayerData data = data(player);
        if(data.current != null && mode() != null)
        {
            applyMorph(player, data.current, false);
        }
        sendData(player);
    }

    public static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive)
    {
        PlayerData data = data(newPlayer);
        if(!alive)
        {
            data.current = null;
        }
        mode().onRespawn(newPlayer, alive);
        if(data.current != null)
        {
            applyMorph(newPlayer, data.current, false);
        }
        sendData(newPlayer);
    }

    public static void onChangeWorld(ServerPlayer player)
    {
        syncMorph(player, null, 1, false);
    }

    public static void onKill(ServerPlayer killer, LivingEntity killed)
    {
        if(killed == killer || killed instanceof Player || mode() == null)
        {
            return;
        }
        mode().onKill(killer, killed);
    }
}
