package com.iiampanda.morph.api.mob.trait;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StepHeightTrait extends Trait<StepHeightTrait>
{
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("morph", "trait_step_height");

    public Double amount;

    public StepHeightTrait() { type = "traitStepHeight"; }

    @Override
    public void addHooks()
    {
        super.addHooks();
        if(player != null && !player.level().isClientSide)
        {
            AttributeInstance inst = player.getAttribute(Attributes.STEP_HEIGHT);
            if(inst != null)
            {
                inst.removeModifier(MODIFIER_ID);
                double diff = (amount == null ? 1D : amount) - inst.getBaseValue();
                if(diff > 0)
                {
                    inst.addTransientModifier(new AttributeModifier(MODIFIER_ID, diff, AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }
    }

    @Override
    public void removeHooks()
    {
        super.removeHooks();
        if(player != null && !player.level().isClientSide)
        {
            AttributeInstance inst = player.getAttribute(Attributes.STEP_HEIGHT);
            if(inst != null)
            {
                inst.removeModifier(MODIFIER_ID);
            }
        }
    }

    @Override public void tick(float strength) {}
}
