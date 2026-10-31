package com.iiampanda.morph.mixin;

import com.iiampanda.morph.api.mob.trait.TraitHooks;
import com.iiampanda.morph.api.mob.trait.ability.ClimbAbility;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void morph$bucketed(CallbackInfoReturnable<Boolean> cir)
    {
        if((Object)this instanceof Player p && com.iiampanda.morph.common.morph.BucketHold.VIEW.contains(p.getUUID()))
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void morph$hurt(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
    {
        LivingEntity self = (LivingEntity)(Object)this;
        if(!self.level().isClientSide() && !TraitHooks.allowDamage(self, source, amount))
        {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float morph$scaleDamage(float amount, net.minecraft.server.level.ServerLevel level, DamageSource source)
    {
        LivingEntity self = (LivingEntity)(Object)this;
        return self.level().isClientSide() ? amount : TraitHooks.modifyDamage(self, source, amount);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void morph$noJumpWhenRolled(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci)
    {
        if((Object)this instanceof Player player && (com.iiampanda.morph.api.mob.trait.ability.SpecialRollUpAbility.isRolled(player) || com.iiampanda.morph.api.mob.trait.ability.SpecialFrogHopAbility.holdsJump(player)))
        {
            ci.cancel();
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void morph$addEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir)
    {
        if(!TraitHooks.allowEffect((LivingEntity)(Object)this, effect))
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "increaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void morph$noAirOnLand(int current, CallbackInfoReturnable<Integer> cir)
    {
        if((Object)this instanceof Player player && com.iiampanda.morph.api.mob.trait.WaterBreatherTrait.blocksAirRegen(player))
        {
            cir.setReturnValue(current);
        }
    }

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void morph$climb(CallbackInfoReturnable<Boolean> cir)
    {
        if(!cir.getReturnValue() && (Object)this instanceof Player player && ClimbAbility.shouldClimb(player))
        {
            cir.setReturnValue(true);
        }
    }
}
