package com.iiampanda.morph.client.mixin;

import com.iiampanda.morph.common.MorphRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin
{
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void morph$active(CallbackInfoReturnable<Boolean> cir)
    {
        LocalPlayer p = Minecraft.getInstance().player;
        if(p == null)
        {
            return;
        }
        Slot self = (Slot)(Object)this;
        int i = self.index;
        if(i >= 0 && i < p.inventoryMenu.slots.size() && p.inventoryMenu.slots.get(i) == self && !MorphRules.slotAllowed(p, i))
        {
            cir.setReturnValue(false);
        }
    }
}
