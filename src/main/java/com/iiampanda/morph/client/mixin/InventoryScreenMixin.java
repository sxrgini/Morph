package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.common.MorphRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin
{
    @Unique private static final Identifier MORPH_LOCKED = Identifier.fromNamespaceAndPath("morph", "textures/gui/locked.png");

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void morph$locks(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci)
    {
        LocalPlayer p = Minecraft.getInstance().player;
        if(p == null)
        {
            return;
        }
        for(Slot s : ((AbstractContainerScreen<?>)(Object)this).getMenu().slots)
        {
            if(!MorphRules.slotAllowed(p, s.index))
            {
                graphics.blit(RenderPipelines.GUI_TEXTURED, MORPH_LOCKED, ((AbstractContainerScreenAccessor)this).morph$leftPos() + s.x, ((AbstractContainerScreenAccessor)this).morph$topPos() + s.y, 0F, 0F, 16, 16, 16, 16);
            }
        }
    }
}
