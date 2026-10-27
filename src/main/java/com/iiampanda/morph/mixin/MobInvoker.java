package com.iiampanda.morph.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mob.class)
public interface MobInvoker
{
    @Invoker("playAmbientSound")
    void morph$playAmbientSound();
}
