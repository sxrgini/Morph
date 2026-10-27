package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.MorphInfoHolder;
import com.iiampanda.morph.api.mob.trait.IEventBusRequired;
import com.iiampanda.morph.api.mob.trait.Trait;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Other players can hop on your back. */
public class RideableAbility extends Ability<RideableAbility> implements IEventBusRequired
{
    public Boolean requiresSaddle; // if true, the morphed player has to hold a saddle in the off hand
    public Boolean requiresHarness; // if true, the morphed player has to hold a harness (any colour) in the off hand
    public Integer maxPassengers; // default 1; the Happy Ghast carries 4
    public Boolean solidPlatform; // other players can stand on you (needs the harness too when requiresHarness is set)

    public RideableAbility() { type = "abilityRideable"; }

    @Override protected String iconName() { return "step"; }

    @Override public void tick(float strength) {}

    public int seats() { return maxPassengers == null ? 1 : Math.max(1, maxPassengers); }

    private static boolean isHarness(net.minecraft.world.item.ItemStack stack)
    {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_harness");
    }

    /** True when the saddle / harness this ability asks for is in the morphed player's off hand. */
    public boolean gearReady()
    {
        if(requiresSaddle != null && requiresSaddle && !player.getOffhandItem().is(Items.SADDLE))
        {
            return false;
        }
        return requiresHarness == null || !requiresHarness || isHarness(player.getOffhandItem());
    }

    /** The rideable ability of a morphed player, if there is one and it is switched on. */
    public static RideableAbility of(Player p)
    {
        var info = ((MorphInfoHolder)p).morph$getMorphInfo();
        if(info == null || !info.isMorphed() || info.isTransitioning())
        {
            return null;
        }
        for(Trait<?> t : info.traits)
        {
            if(t instanceof RideableAbility r && r.enabled)
            {
                return r;
            }
        }
        return null;
    }

    @Override
    public InteractionResult onInteractEntity(Player interactor, Entity target, InteractionHand hand)
    {
        if(target != player || interactor == player || !enabled || interactor.isPassenger() || interactor.isSecondaryUseActive())
        {
            return InteractionResult.PASS;
        }
        if(!gearReady())
        {
            return InteractionResult.PASS;
        }
        if(player.getPassengers().size() >= seats())
        {
            return InteractionResult.PASS;
        }
        if(!player.level().isClientSide())
        {
            interactor.startRiding(player);
        }
        return InteractionResult.SUCCESS;
    }
}
