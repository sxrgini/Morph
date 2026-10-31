package com.iiampanda.morph.mixin;

import com.iiampanda.morph.api.MorphEntityAccess;
import com.iiampanda.morph.api.MorphInfoHolder;
import com.iiampanda.morph.api.mob.trait.SwimmerTrait;
import com.iiampanda.morph.api.mob.trait.Trait;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.iiampanda.morph.api.mob.trait.ability.RideableAbility;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin implements MorphEntityAccess
{
    @Unique
    private UUID morph$playerId;

    @Override
    public UUID morph$getMorphPlayerId()
    {
        return morph$playerId;
    }

    @Override
    public void morph$setMorphPlayerId(UUID id)
    {
        morph$playerId = id;
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void morph$removed(Entity.RemovalReason reason, CallbackInfo ci)
    {
        if((Object)this instanceof Player player)
        {
            ((MorphInfoHolder)player).morph$getMorphInfo().detach();
        }
    }

    @ModifyVariable(method = "moveRelative", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float morph$swimSpeed(float speed)
    {
        if((Object)this instanceof Player player)
        {
            var info = ((MorphInfoHolder)player).morph$getMorphInfo();
            if(info.isMorphed() && !info.isTransitioning())
            {
                for(Trait<?> trait : info.traits)
                {
                    if(trait instanceof SwimmerTrait swim)
                    {
                        if(player.isInWater())
                        {
                            return speed * (float)swim.getSwimSpeedMultiplier();
                        }
                        else if(player.onGround())
                        {
                            return speed * (float)swim.getLandMultiplier();
                        }
                    }
                }
            }
        }
        return speed;
    }

    @Inject(method = "canBeCollidedWith", at = @At("RETURN"), cancellable = true)
    private void morph$platform(Entity other, CallbackInfoReturnable<Boolean> cir)
    {
        if(cir.getReturnValueZ() || !((Object)this instanceof Player p) || !(other instanceof Player standing) || standing == p || !p.isAlive() || p.isSpectator())
        {
            return;
        }
        if(standing.position().y >= p.getBoundingBox().maxY - 0.05D && com.iiampanda.morph.api.mob.trait.SwallowBlocksTrait.hasBlock(p))
        {
            cir.setReturnValue(true);
            return;
        }
        RideableAbility ride = RideableAbility.of(p);
        if(ride != null && ride.solidPlatform != null && ride.solidPlatform && ride.gearReady() && standing.position().y >= p.getBoundingBox().maxY - 0.05D)
        {
            cir.setReturnValue(true);
        }
    }
}
