package com.iiampanda.morph.client;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.TraitHandler;
import com.iiampanda.morph.api.mob.trait.ability.Ability;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.client.gui.BiomassScreen;
import com.iiampanda.morph.common.biomass.Upgrades;
import com.iiampanda.morph.common.packet.Packets;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

public final class ClientPackets
{
    private ClientPackets() {}

    /** Installed as {@link Packets#clientHandler}; the loader module forwards every S2C payload here (on the client thread). */
    public static void register()
    {
        Packets.clientHandler = ClientPackets::handle;
    }

    private static void handle(CustomPacketPayload packet)
    {
        Minecraft mc = Minecraft.getInstance();
        if(packet instanceof Packets.SyncMorph payload)
        {
            Player p = mc.level == null ? null : mc.level.getPlayerByUUID(payload.player());
            if(p == null)
            {
                ClientState.PENDING.put(payload.player(), payload);
            }
            else
            {
                applySyncMorph(p, payload);
            }
        }
        else if(packet instanceof Packets.SyncData payload)
        {
            ClientState.DATA.read(payload.data());
            Upgrades.fromJson(payload.upgradesJson());
            ClientState.mode = payload.mode();
            ClientState.canSelect = payload.canSelect();
            ClientState.canMorph = payload.canMorph();
            ClientState.biomassUnlocked = payload.biomassUnlocked();
            ClientState.abilitiesUnlocked = payload.abilitiesUnlocked();
            ClientState.clearGuiEntities();
            if(mc.gui.screen() instanceof BiomassScreen screen)
            {
                screen.dataChanged();
            }
        }
        else if(packet instanceof Packets.SyncBiomass payload)
        {
            ClientState.DATA.biomass = payload.biomass();
        }
        else if(packet instanceof Packets.Acquire payload)
        {
            AcquisitionEffects.start(payload);
        }
    }

    public static void applySyncMorph(Player p, Packets.SyncMorph payload)
    {
        MorphInfo info = MorphApi.getMorphInfo(p);
        MorphVariant variant = payload.variant().map(MorphVariant::read).orElse(null);
        ArrayList<Trait<?>> traits = variant == null ? new ArrayList<>() : TraitHandler.fromJson(payload.traitsJson());
        for(Trait<?> t : traits)
        {
            if(t instanceof Ability<?> a)
            {
                a.enabled = payload.enabledAbilities().contains(a.type);
            }
        }
        info.set(variant, traits, payload.duration(), payload.animate());
        if(p == Minecraft.getInstance().player)
        {
            ClientState.DATA.current = variant;
        }
    }
}
