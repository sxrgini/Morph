package com.iiampanda.morph.api.mob.trait;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Sulfur Cube: blocks can be put inside you (right click with one, a dispenser or a piston) and sheared back out, like the mob. */
public class SwallowBlocksTrait extends Trait<SwallowBlocksTrait> implements IEventBusRequired
{
    public SwallowBlocksTrait() { type = "traitSwallowBlocks"; }

    @Override public void tick(float strength) {}

    public static boolean swallowable(ItemStack stack)
    {
        return !stack.isEmpty() && stack.is(ItemTags.SULFUR_CUBE_SWALLOWABLE);
    }

    /** The trait of a morphed player, if they are a sulfur cube. */
    public static SwallowBlocksTrait of(Player p)
    {
        var info = ((com.iiampanda.morph.api.MorphInfoHolder)p).morph$getMorphInfo();
        if(info == null || !info.isMorphed() || info.isTransitioning())
        {
            return null;
        }
        for(Trait<?> t : info.traits)
        {
            if(t instanceof SwallowBlocksTrait s)
            {
                return s;
            }
        }
        return null;
    }

    /** Puts one of the item inside the player (a block already inside is spat out). @return false if nothing happened */
    public boolean swallow(ServerPlayer sp, ItemStack item)
    {
        ItemStack current = sp.getItemBySlot(EquipmentSlot.BODY);
        if(!swallowable(item) || (!current.isEmpty() && item.is(current.getItem())))
        {
            return false;
        }
        if(!current.isEmpty())
        {
            sp.spawnAtLocation(sp.level(), current, sp.getBbHeight() / 2F);
        }
        sp.setItemSlot(EquipmentSlot.BODY, item.copyWithCount(1));
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.SULFUR_CUBE_ABSORB, SoundSource.PLAYERS, 1F, 1F);
        return true;
    }

    @Override
    public InteractionResult onInteractEntity(Player interactor, Entity target, InteractionHand hand)
    {
        if(target != player || interactor == player || !(player instanceof ServerPlayer sp))
        {
            return InteractionResult.PASS;
        }
        ItemStack held = interactor.getItemInHand(hand);
        if(held.is(Items.SHEARS) && !sp.getItemBySlot(EquipmentSlot.BODY).isEmpty())
        {
            sp.spawnAtLocation(sp.level(), sp.getItemBySlot(EquipmentSlot.BODY), sp.getBbHeight() / 2F);
            sp.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.SULFUR_CUBE_EJECT, SoundSource.PLAYERS, 1F, 1F);
            held.hurtAndBreak(1, interactor, hand.asEquipmentSlot());
            return InteractionResult.SUCCESS_SERVER;
        }
        if(swallowable(held) && swallow(sp, held))
        {
            if(!interactor.hasInfiniteMaterials())
            {
                held.shrink(1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    /** Whatever was inside comes out when you change shape. */
    @Override
    public void onUnmorph()
    {
        if(player instanceof ServerPlayer sp && sp.isAlive())
        {
            ItemStack current = sp.getItemBySlot(EquipmentSlot.BODY);
            if(!current.isEmpty())
            {
                sp.spawnAtLocation(sp.level(), current, sp.getBbHeight() / 2F);
                sp.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
            }
        }
    }
}
