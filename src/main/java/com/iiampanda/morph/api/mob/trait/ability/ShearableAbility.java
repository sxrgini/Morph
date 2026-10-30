package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.mob.trait.IEventBusRequired;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Other players can shear you with shears (sheep wool, mooshroom mushrooms...). */
public class ShearableAbility extends Ability<ShearableAbility>implements IEventBusRequired
{
    public String dropItem;      // item id, default minecraft:white_wool
    public Integer minCount;
    public Integer maxCount;
    public Integer cooldown;     // ticks between shearings, default 3000 (2.5 minutes)

    public transient long lastSheared = -999999L;

    public ShearableAbility() { type = "abilityShearable"; }

    @Override protected String iconName() { return "step"; }

    private boolean announced;

    private void broadcastSheared(boolean sheared)
    {
        if(player instanceof net.minecraft.server.level.ServerPlayer sp)
        {
            com.iiampanda.morph.common.packet.Packets.SyncSheared payload = new com.iiampanda.morph.common.packet.Packets.SyncSheared(sp.getUUID(), sheared);
            com.iiampanda.morph.platform.Platform.sendToPlayer(sp, payload);
            com.iiampanda.morph.platform.Platform.sendToTracking(sp, payload);
            announced = sheared;
        }
    }

    @Override
    public void tick(float strength)
    {
        if(player.level().isClientSide())
        {
            return;
        }
        boolean sheared = player.level().getGameTime() - lastSheared < (cooldown == null ? 3000 : cooldown);
        if(sheared != announced || (sheared && player.tickCount % 40 == 0))
        {
            broadcastSheared(sheared);
        }
    }

    private InteractionResult interactImpl(Player interactor, Entity target, InteractionHand hand)
    {
        if(target != player || interactor == player || !enabled || !interactor.getItemInHand(hand).is(Items.SHEARS))
        {
            return InteractionResult.PASS;
        }
        long now = player.level().getGameTime();
        if(now - lastSheared < (cooldown == null ? 3000 : cooldown))
        {
            return InteractionResult.PASS;
        }
        if(!player.level().isClientSide())
        {
            lastSheared = now;
            broadcastSheared(true);
            Identifier id = Identifier.tryParse(dropItem == null ? "minecraft:white_wool" : dropItem);
            int min = minCount == null ? 1 : minCount;
            int max = Math.max(min, maxCount == null ? 3 : maxCount);
            int count = min + player.getRandom().nextInt(max - min + 1);
            if(id != null)
            {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id), count);
                if(!stack.isEmpty())
                {
                    if(player.level() instanceof net.minecraft.server.level.ServerLevel sl) player.spawnAtLocation(sl, stack, player.getBbHeight());
                }
            }
            interactor.getItemInHand(hand).hurtAndBreak(1, interactor, hand.asEquipmentSlot());
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1F, 1F);
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
