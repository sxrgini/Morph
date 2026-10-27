package com.iiampanda.morph.util;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.mixin.EntityInvoker;
import com.iiampanda.morph.mixin.LivingEntityInvoker;
import com.iiampanda.morph.mixin.MobInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Lets a morphed player make the noises of the mob they are. */
public final class SoundHelper
{
    private static final java.util.Map<java.util.UUID, Integer> AMBIENT = new java.util.HashMap<>();

    private SoundHelper() {}

    private static LivingEntity body(Player player)
    {
        MorphInfo info = MorphApi.getMorphInfo(player);
        return info.isMorphed() ? info.getEntity() : null;
    }

    public static SoundEvent hurt(Player player, DamageSource source)
    {
        LivingEntity e = body(player);
        return e == null ? null : ((LivingEntityInvoker)e).morph$getHurtSound(source);
    }

    public static SoundEvent death(Player player)
    {
        LivingEntity e = body(player);
        return e == null ? null : ((LivingEntityInvoker)e).morph$getDeathSound();
    }

    /** @return true if the vanilla step sound should be skipped */
    public static boolean step(Player player, BlockPos pos, BlockState state)
    {
        LivingEntity e = body(player);
        if(e == null)
        {
            return false;
        }
        if(player.level().isClientSide)
        {
            // the server broadcasts the mob's step sound to everyone, including us
            return true;
        }
        e.setPos(player.getX(), player.getY(), player.getZ());
        e.setOnGround(player.onGround());
        ((EntityInvoker)e).morph$playStepSound(pos, state);
        return true;
    }

    /** Random idle noises, server side only. */
    public static void tickAmbient(Player player)
    {
        if(player.level().isClientSide || player.isSpectator())
        {
            return;
        }
        LivingEntity e = body(player);
        if(!(e instanceof Mob mob))
        {
            return;
        }
        int t = AMBIENT.getOrDefault(player.getUUID(), 0);
        if(player.getRandom().nextInt(1000) < t++)
        {
            t = -mob.getAmbientSoundInterval();
            mob.setPos(player.getX(), player.getY(), player.getZ());
            ((MobInvoker)mob).morph$playAmbientSound();
        }
        AMBIENT.put(player.getUUID(), t);
    }
}
