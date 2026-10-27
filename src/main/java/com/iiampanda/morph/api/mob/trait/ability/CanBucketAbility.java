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

/** Use a water bucket on a morphed player to scoop up a bucket of their mob (fish, axolotl, tadpole...). The player is not harmed. */
public class CanBucketAbility extends Ability<CanBucketAbility> implements IEventBusRequired
{
    public String bucketItem;  // e.g. minecraft:cod_bucket
    public Integer cooldown;   // ticks, default 1200

    public transient long lastBucketed = -999999L;

    public CanBucketAbility() { type = "abilityCanBucket"; }

    @Override protected String iconName() { return "swim"; }

    @Override public void tick(float strength) {}

    @Override
    public InteractionResult onInteractEntity(Player interactor, Entity target, InteractionHand hand)
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
            if(!interactor.getAbilities().instabuild)
            {
                interactor.setItemInHand(hand, bucket);
            }
            else if(!interactor.getInventory().add(bucket))
            {
                interactor.drop(bucket, false);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL_FISH, SoundSource.PLAYERS, 1F, 1F);
        }
        return InteractionResult.SUCCESS;
    }
}
