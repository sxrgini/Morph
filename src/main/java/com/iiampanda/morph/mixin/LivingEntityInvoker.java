package com.iiampanda.morph.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker
{
    @Accessor("jumping")
    boolean morph$isJumping();

    @Invoker("getHurtSound")
    SoundEvent morph$getHurtSound(DamageSource source);

    @Invoker("getDeathSound")
    SoundEvent morph$getDeathSound();
}
