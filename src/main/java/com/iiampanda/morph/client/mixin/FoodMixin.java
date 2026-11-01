package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.client.FoodIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.Hud.class)
public abstract class FoodMixin
{
    @org.spongepowered.asm.mixin.Unique private static String morph$last = "";

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void morph$food(GuiGraphicsExtractor g, Player player, int yLineBase, int xRight, CallbackInfo ci)
    {
        ItemStack stack = FoodIcons.stackFor(player);
        String desc = stack.isEmpty() ? "vanilla" : stack.getItem().toString();
        if(!desc.equals(morph$last))
        {
            morph$last = desc;
            com.iiampanda.morph.common.Morph.LOGGER.info("[MorphHud] hunger bar now {}", desc);
        }
        if(stack.isEmpty())
        {
            return;
        }
        ci.cancel();
        int food = player.getFoodData().getFoodLevel();
        for(int i = 0; i < 10; i++)
        {
            int xo = xRight - i * 8 - 9;
            boolean full = i * 2 + 1 < food;
            boolean half = i * 2 + 1 == food;
            if(!full && !half)
            {
                continue;
            }
            if(half)
            {
                g.enableScissor(xo, yLineBase, xo + 5, yLineBase + 9);
            }
            g.pose().pushMatrix();
            g.pose().translate((float)xo, (float)yLineBase);
            g.pose().scale(0.5625F, 0.5625F);
            g.item(stack, 0, 0);
            g.pose().popMatrix();
            if(half)
            {
                g.disableScissor();
            }
        }
    }
}
