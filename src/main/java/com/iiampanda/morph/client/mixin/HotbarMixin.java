package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.common.MorphRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class HotbarMixin
{
    @Shadow protected abstract Player getCameraPlayer();

    @Shadow protected abstract void renderSlot(GuiGraphics graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed);

    @Unique private static final ResourceLocation MORPH_BAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
    @Unique private static final ResourceLocation MORPH_SEL = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void morph$hotbar(GuiGraphics g, DeltaTracker dt, CallbackInfo ci)
    {
        Player player = getCameraPlayer();
        if(player == null || !MorphRules.hotbarOnly(player))
        {
            return;
        }
        int n = MorphRules.hotbarSlots(player);
        if(n >= 9)
        {
            return;
        }
        ci.cancel();
        int cx = g.guiWidth() / 2;
        int y = g.guiHeight() - 22;
        int left = cx - 91;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, -90.0F);
        g.blitSprite(MORPH_BAR, 182, 22, 0, 0, left, y, 20 * n + 1, 22);
        g.blitSprite(MORPH_BAR, 182, 22, 181, 0, left + 20 * n + 1, y, 1, 22);
        int sel = player.getInventory().selected;
        if(sel < n)
        {
            g.blitSprite(MORPH_SEL, left - 1 + sel * 20, y - 1, 24, 23);
        }
        g.pose().popPose();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        int seed = 1;
        for(int i = 0; i < n; i++)
        {
            this.renderSlot(g, cx - 90 + i * 20 + 2, g.guiHeight() - 19, dt, player, player.getInventory().getItem(i), seed++);
        }
    }
}
