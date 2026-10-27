package com.iiampanda.morph.mixin;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.ability.FlightFlapAbility;
import com.iiampanda.morph.api.mob.trait.ability.FlyAbility;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin
{
    /** Morphs that can fly shouldn't be kicked for "floating too long". */
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
