package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.mob.trait.IEventBusRequired;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CanBucketAbility extends Ability<CanBucketAbility> implements IEventBusRequired
{
    public String bucketItem;
    public Integer cooldown;

    public transient long lastBucketed = -999999L;

    public CanBucketAbility() { type = "abilityCanBucket"; }

    @Override protected String iconName() { return "swim"; }

    @Override public void tick(float strength) {}

    private InteractionResult interactImpl(Player interactor, Entity target, InteractionHand hand)
    {
        if(target != player || interactor == player || !enabled || bucketItem == null || !interactor.getItemInHand(hand).is(Items.WATER_BUCKET))
        {
            return InteractionResult.PASS;
        }
        long now = player.level().getGameTime();
        if(now - lastBucketed < (cooldown == null ? 1200 : cooldown))
        {
            return InteractionResult.PASS;
        }
        Identifier id = Identifier.tryParse(bucketItem);
        if(id == null || !BuiltInRegistries.ITEM.containsKey(id))
        {
            return InteractionResult.PASS;
        }
        if(!player.level().isClientSide())
        {
            lastBucketed = now;
            ItemStack bucket = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            com.iiampanda.morph.common.morph.BucketHold.mark(bucket, player.getUUID());
            if(!interactor.getAbilities().instabuild)
            {
                interactor.setItemInHand(hand, bucket);
            }
            else if(!interactor.getInventory().add(bucket))
            {
                interactor.drop(bucket, false);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL_FISH, SoundSource.PLAYERS, 1F, 1F);
            if(player instanceof net.minecraft.server.level.ServerPlayer held && interactor instanceof net.minecraft.server.level.ServerPlayer picker)
            {
                com.iiampanda.morph.common.morph.BucketHold.start(held, picker);
            }
        }
        if(player.level().isClientSide())
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
            com.iiampanda.morph.common.Dbg.log("{} {} by {} sneaking={} holding={} enabled={} result={}", player.level().isClientSide() ? "client" : "server", type, interactor.getName().getString(), interactor.isShiftKeyDown(), interactor.getItemInHand(hand), enabled, result);
        }
        return result;
    }
}
