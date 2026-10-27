package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.client.MorphBodies;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AxolotlModel.class)
public abstract class AxolotlModelMixin
{
    @ModifyVariable(method = "setupAnim(Lnet/minecraft/world/entity/animal/axolotl/Axolotl;FFFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float morph$tempo(float ageInTicks, Axolotl entity)
    {
        return MorphBodies.tempo(entity, ageInTicks);
    }
}
