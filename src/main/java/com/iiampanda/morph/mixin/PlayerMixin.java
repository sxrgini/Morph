package com.iiampanda.morph.mixin;

import com.iiampanda.morph.api.MorphInfoHolder;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.util.SoundHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin implements MorphInfoHolder
{
    @Unique
    private MorphInfo morph$info;

    @Override
    public MorphInfo morph$getMorphInfo()
    {
        if(morph$info == null)
        {
            morph$info = new MorphInfo((Player)(Object)this);
        }
        return morph$info;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void morph$tick(CallbackInfo ci)
    {
        morph$getMorphInfo().tick();
        SoundHelper.tickAmbient((Player)(Object)this);
    }

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void morph$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir)
    {
        if(morph$info != null)
        {
            EntityDimensions dims = morph$info.getDimensions(pose);
            if(dims != null)
            {
                cir.setReturnValue(dims);
            }
        }
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void morph$hurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir)
    {
        SoundEvent s = SoundHelper.hurt((Player)(Object)this, source);
        if(s != null)
        {
            cir.setReturnValue(s);
        }
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void morph$deathSound(CallbackInfoReturnable<SoundEvent> cir)
    {
        SoundEvent s = SoundHelper.death((Player)(Object)this);
        if(s != null)
        {
            cir.setReturnValue(s);
        }
    }

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void morph$stepSound(BlockPos pos, BlockState state, CallbackInfo ci)
    {
        if(SoundHelper.step((Player)(Object)this, pos, state))
        {
            ci.cancel();
        }
    }
}
