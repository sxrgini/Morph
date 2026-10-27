package com.iiampanda.morph.common.packet;

import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.morph.MorphManager;
import com.iiampanda.morph.common.save.PlayerData;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ServerPackets
{
    private ServerPackets() {}

    public static void handle(CustomPacketPayload packet, ServerPlayer player)
    {
        if(packet instanceof Packets.RequestMorph payload)
        {
            if(payload.variant().isPresent())
            {
                MorphVariant v = MorphVariant.read(payload.variant().get());
                if(v != null)
                {
                    MorphManager.morph(player, v, false);
                }
            }
            else
            {
                MorphManager.morph(player, null, false);
            }
            MorphManager.sendData(player);
        }
        if(packet instanceof Packets.SetFavourite payload)
        {
            MorphVariant v = MorphVariant.read(payload.variant());
            PlayerData data = MorphManager.data(player);
            if(v != null && data.contains(v))
            {
                if(payload.favourite())
                {
                    data.favourites.add(v.key());
                }
                else
                {
                    data.favourites.remove(v.key());
                }
                MorphManager.sendData(player);
            }
        }
        if(packet instanceof Packets.ForgetMorph payload)
        {
            MorphVariant v = MorphVariant.read(payload.variant());
            if(v != null)
            {
                MorphManager.forget(player, v);
            }
        }
        if(packet instanceof Packets.ToggleAbility payload)
        {
            MorphManager.toggleAbility(player, payload.abilityType());
        }
        if(packet instanceof Packets.UseSpecial payload)
        {
            com.iiampanda.morph.common.mob.SpecialAbilities.use(player);
        }
        if(packet instanceof Packets.PurchaseUpgrade payload)
        {
            MorphManager.purchase(player, payload.id());
        }
    }
}
