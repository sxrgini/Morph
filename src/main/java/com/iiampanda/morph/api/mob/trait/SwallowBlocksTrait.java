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

    public static final java.util.Map<java.util.UUID, String> SYNCED = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<java.util.UUID, Double> LAST_Y = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<java.util.UUID, Double> FALL_VY = new java.util.concurrent.ConcurrentHashMap<>();

    private transient String appliedItem = "";
    private transient String announcedItem = "";
    private transient java.util.List<net.minecraft.world.entity.SulfurCubeArchetype> applied = java.util.List.of();

    public static ItemStack bodyStack(Player p)
    {
        if(p.level().isClientSide())
        {
            String id = SYNCED.get(p.getUUID());
            if(id == null)
            {
                return p.getItemBySlot(EquipmentSlot.BODY);
            }
            net.minecraft.resources.Identifier key = net.minecraft.resources.Identifier.tryParse(id);
            ItemStack stack = key == null || id.isEmpty() ? ItemStack.EMPTY : new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key));
            return stack;
        }
        return p.getItemBySlot(EquipmentSlot.BODY);
    }

    public static boolean hasBlock(Player p)
    {
        return of(p) != null && !bodyStack(p).isEmpty();
    }

    private static java.util.List<net.minecraft.world.entity.SulfurCubeArchetype> archetypes(Player p, ItemStack stack)
    {
        if(stack.isEmpty())
        {
            return java.util.List.of();
        }
        return p.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.SULFUR_CUBE_ARCHETYPE).stream().filter(a -> stack.is(a.items())).toList();
    }

    private void applyArchetypes(ItemStack stack)
    {
        for(var a : applied)
        {
            for(var mod : a.attributeModifiers())
            {
                var inst = player.getAttribute(mod.attribute());
                if(inst != null)
                {
                    inst.removeModifier(mod.modifier());
                }
            }
        }
        applied = archetypes(player, stack);
        for(var a : applied)
        {
            for(var mod : a.attributeModifiers())
            {
                var inst = player.getAttribute(mod.attribute());
                if(inst != null)
                {
                    inst.addOrUpdateTransientModifier(mod.modifier());
                }
            }
        }
    }

    private static String idOf(ItemStack stack)
    {
        return stack.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private void broadcast(ServerPlayer sp, String id)
    {
        com.iiampanda.morph.common.packet.Packets.SyncSwallowed payload = new com.iiampanda.morph.common.packet.Packets.SyncSwallowed(sp.getUUID(), id);
        com.iiampanda.morph.platform.Platform.sendToPlayer(sp, payload);
        com.iiampanda.morph.platform.Platform.sendToTracking(sp, payload);
        announcedItem = id;
    }

    @Override
    public void tick(float strength)
    {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.BODY);
        String id = idOf(stack);
        boolean buoyant = false;
        for(var a : applied)
        {
            buoyant |= a.buoyant();
        }
        if(buoyant && !stack.isEmpty() && player.isInWater() && !player.isShiftKeyDown())
        {
            player.setDeltaMovement(player.getDeltaMovement().add(0D, 0.03D, 0D));
        }
        if(player.level().isClientSide() || !(player instanceof ServerPlayer sp) || strength < 1F)
        {
            return;
        }
        if(!id.equals(appliedItem))
        {
            applyArchetypes(stack);
            appliedItem = id;
        }
        if(!id.equals(announcedItem) || sp.tickCount % 40 == 0)
        {
            broadcast(sp, id);
        }
        if(stack.isEmpty() || sp.isSpectator())
        {
            return;
        }
        double bounce = sp.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BOUNCINESS);
        net.minecraft.world.phys.AABB box = sp.getBoundingBox();
        if(bounce > 0D)
        {
            for(ServerPlayer q : sp.level().getEntitiesOfClass(ServerPlayer.class, box.inflate(0.05D, 0D, 0.05D).expandTowards(0D, 2.5D, 0D), e -> e != sp && !e.isSpectator()))
            {
                double last = LAST_Y.getOrDefault(q.getUUID(), q.getY());
                double vy = q.getY() - last;
                LAST_Y.put(q.getUUID(), q.getY());
                if(!q.onGround())
                {
                    if(vy < 0D)
                    {
                        q.resetFallDistance();
                        FALL_VY.put(q.getUUID(), vy);
                    }
                }
                else if(q.getY() <= box.maxY + 0.1D)
                {
                    Double prev = FALL_VY.remove(q.getUUID());
                    if(prev != null && prev < -0.15D && !q.isShiftKeyDown())
                    {
                        double up = Math.min(1.3D, -prev * Math.max(0.5D, bounce));
                        q.setDeltaMovement(q.getDeltaMovement().x, up, q.getDeltaMovement().z);
                        q.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(q));
                        q.resetFallDistance();
                        for(var a : applied)
                        {
                            sp.level().playSound(null, q.getX(), q.getY(), q.getZ(), a.soundSettings().hitSound(), SoundSource.PLAYERS, 1F, 1F);
                            break;
                        }
                    }
                }
            }
        }
        if(sp.tickCount % 10 == 0)
        {
            for(var a : applied)
            {
                if(a.contactDamage().isPresent())
                {
                    var dmg = a.contactDamage().get();
                    for(net.minecraft.world.entity.LivingEntity victim : sp.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box.inflate(0.1D), e -> e != sp && e.isAlive() && !(e instanceof Player pl && (pl.isCreative() || pl.isSpectator()))))
                    {
                        victim.hurtServer(sp.level(), new net.minecraft.world.damagesource.DamageSource(dmg.damageType(), dmg.attributeToSource() ? sp : null), dmg.amount().sample(sp.getRandom()));
                    }
                }
            }
        }
    }

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
        applyArchetypes(ItemStack.EMPTY);
        appliedItem = "";
        if(player instanceof ServerPlayer spp)
        {
            broadcast(spp, "");
        }
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
