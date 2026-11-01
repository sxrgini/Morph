package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.common.MorphRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
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
    @Unique private static final ResourceLocation MORPH_LOCKED = ResourceLocation.fromNamespaceAndPath("morph", "textures/gui/locked.png");

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void morph$locks(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci)
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
                graphics.blit(MORPH_LOCKED, ((AbstractContainerScreenAccessor)this).morph$leftPos() + s.x, ((AbstractContainerScreenAccessor)this).morph$topPos() + s.y, 0, 0, 16, 16, 16, 16);
            }
        }
    }
}
