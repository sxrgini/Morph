package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.client.render.MorphBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartCaptureMixin
{

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void morph$capture(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color, CallbackInfo ci)
    {
        MorphBlocks cap = MorphBlocks.capturing;
        if(cap != null)
        {
            cap.addPart((ModelPart)(Object)this, pose.pose());
            ci.cancel();
        }
    }
}
