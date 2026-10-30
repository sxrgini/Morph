package com.iiampanda.morph.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ClientInteract
{
    private ClientInteract() {}

    public static void send(Entity target, Player interactor, InteractionHand hand)
    {
        Minecraft mc = Minecraft.getInstance();
        com.iiampanda.morph.common.Dbg.log("client sends interact for {} hand={}", target.getName().getString(), hand);
        if(mc.getConnection() != null)
        {
            mc.getConnection().send(new ServerboundInteractPacket(target.getId(), hand, net.minecraft.world.phys.Vec3.ZERO, interactor.isShiftKeyDown()));
        }
    }
}
