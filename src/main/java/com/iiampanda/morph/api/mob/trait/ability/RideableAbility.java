package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.mob.trait.IEventBusRequired;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class RideableAbility extends Ability<RideableAbility> implements IEventBusRequired
{
    public Boolean requiresSaddle;

    public RideableAbility() { type = "abilityRideable"; }

    @Override protected String iconName() { return "step"; }

    @Override public void tick(float strength) {}

    private InteractionResult interactImpl(Player interactor, Entity target, InteractionHand hand)
    {
        if(target != player || interactor == player || !enabled || interactor.isPassenger() || interactor.isSecondaryUseActive())
        {
            return InteractionResult.PASS;
        }
        if(requiresSaddle != null && requiresSaddle && !player.getOffhandItem().is(Items.SADDLE))
        {
            return InteractionResult.PASS;
        }
        if(!player.level().isClientSide && player.getPassengers().isEmpty())
        {
            com.iiampanda.morph.common.morph.RideBypass.ACTIVE.set(true);
            try
            {
                interactor.startRiding(player);
            }
            finally
            {
                com.iiampanda.morph.common.morph.RideBypass.ACTIVE.set(false);
            }
        }
        if(player.level().isClientSide)
        {
            com.iiampanda.morph.client.ClientInteract.send(target, interactor, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onInteractEntity(Player interactor, Entity target, InteractionHand hand)
    {
        InteractionResult result = interactImpl(interactor, target, hand);
        if(target == player && interactor != player)
        {
            com.iiampanda.morph.common.Dbg.log("{} {} by {} sneaking={} holding={} enabled={} result={}", player.level().isClientSide ? "client" : "server", type, interactor.getName().getString(), interactor.isShiftKeyDown(), interactor.getItemInHand(hand), enabled, result);
        }
        return result;
    }
}
