package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.common.MorphRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.Hud.class)
public abstract class HotbarMixin
{
    @Shadow protected abstract Player getCameraPlayer();

    @Shadow protected abstract void extractSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed);

    @Unique private static final Identifier MORPH_BAR = Identifier.withDefaultNamespace("hud/hotbar");
    @Unique private static final Identifier MORPH_SEL = Identifier.withDefaultNamespace("hud/hotbar_selection");

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void morph$hotbar(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci)
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
        g.blitSprite(RenderPipelines.GUI_TEXTURED, MORPH_BAR, 182, 22, 0, 0, left, y, 20 * n + 1, 22);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, MORPH_BAR, 182, 22, 181, 0, left + 20 * n + 1, y, 1, 22);
        int sel = player.getInventory().getSelectedSlot();
        if(sel < n)
        {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, MORPH_SEL, left - 1 + sel * 20, y - 1, 24, 23);
        }
        int seed = 1;
        for(int i = 0; i < n; i++)
        {
            this.extractSlot(g, cx - 90 + i * 20 + 2, g.guiHeight() - 19, dt, player, player.getInventory().getItem(i), seed++);
        }
    }
}
