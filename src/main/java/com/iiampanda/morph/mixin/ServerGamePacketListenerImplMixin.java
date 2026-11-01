package com.iiampanda.morph.mixin;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.ability.FlightFlapAbility;
import com.iiampanda.morph.api.mob.trait.ability.FlyAbility;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin
{
    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void morph$click(ServerboundContainerClickPacket packet, CallbackInfo ci)
    {
        if(player.level().getServer() != null && player.level().getServer().isSameThread() && com.iiampanda.morph.common.MorphRules.blockClick(player, packet.getContainerId(), packet.getSlotNum(), packet.getClickType().name(), packet.getButtonNum()))
        {
            player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void morph$action(net.minecraft.network.protocol.game.ServerboundPlayerActionPacket packet, CallbackInfo ci)
    {
        if(packet.getAction() == net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND && player.level().getServer() != null && player.level().getServer().isSameThread() && !com.iiampanda.morph.common.MorphRules.slotAllowed(player, 45))
        {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void morph$recipe(net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket packet, CallbackInfo ci)
    {
        if(packet.getContainerId() == 0 && player.level().getServer() != null && player.level().getServer().isSameThread() && !com.iiampanda.morph.common.MorphRules.recipeAllowed(player))
        {
            ci.cancel();
        }
    }


    @Inject(method = "getMaximumFlyingTicks", at = @At("HEAD"), cancellable = true)
    private void morph$maxFlyingTicks(Entity entity, CallbackInfoReturnable<Integer> cir)
    {
        if(entity instanceof Player player)
        {
            for(Trait<?> trait : MorphApi.getMorphInfo(player).traits)
            {
                if(trait instanceof FlyAbility || trait instanceof FlightFlapAbility)
                {
                    cir.setReturnValue(Integer.MAX_VALUE / 2);
                    return;
                }
            }
        }
    }
}
