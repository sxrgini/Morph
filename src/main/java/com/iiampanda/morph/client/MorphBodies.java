package com.iiampanda.morph.client;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.common.Morph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public final class MorphBodies
{
    private static final java.util.Map<LivingEntity, Integer> BC_START = new java.util.WeakHashMap<>();
    private static final Set<String> TICK_FAILED = new HashSet<>();
    private static final java.util.Map<LivingEntity, Boolean> WAS_SWINGING = new java.util.IdentityHashMap<>();

    private static final java.util.Map<LivingEntity, float[]> TEMPO = new java.util.WeakHashMap<>();

    public static float tempo(LivingEntity e, float age)
    {
        float[] t = TEMPO.get(e);
        if(t == null)
        {
            return age;
        }
        float dt = age < t[1] ? 0F : Math.min(age - t[1], 4F);
        float factor = Math.max(1F, Math.min(3F, t[2] / 0.1F));
        t[0] += (factor - 1F) * dt;
        t[1] = age;
        return age + t[0];
    }

    private static final java.util.Map<LivingEntity, Boolean> ORIGINAL_PUMPKIN = new java.util.WeakHashMap<>();

    private static final java.util.Map<LivingEntity, double[]> SHULKER_LOCK = new java.util.WeakHashMap<>();

    private static void lockShulker(AbstractClientPlayer p, LivingEntity e)
    {
        double dx = p.getX() - p.xo;
        double dz = p.getZ() - p.zo;
        boolean still = dx * dx + dz * dz < 0.00001D && Math.abs(p.getY() - p.yo) < 0.01D && p.onGround();
        if(!still)
        {
            SHULKER_LOCK.remove(e);
            return;
        }
        double[] lock = SHULKER_LOCK.get(e);
        if(lock == null)
        {
            lock = new double[]{e.getX(), e.getY(), e.getZ(), e.getYRot(), e.getXRot(), e.yBodyRot, e.yHeadRot};
            SHULKER_LOCK.put(e, lock);
        }
        e.setPos(lock[0], lock[1], lock[2]);
        e.xo = lock[0];
        e.yo = lock[1];
        e.zo = lock[2];
        e.xOld = lock[0];
        e.yOld = lock[1];
        e.zOld = lock[2];
        e.setYRot((float)lock[3]);
        e.yRotO = (float)lock[3];
        e.setXRot((float)lock[4]);
        e.xRotO = (float)lock[4];
        e.yBodyRot = (float)lock[5];
        e.yBodyRotO = (float)lock[5];
        e.yHeadRot = (float)lock[6];
        e.yHeadRotO = (float)lock[6];
        e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }

    private MorphBodies() {}

    public static void tick(Minecraft mc)
    {
        if(mc.level == null)
        {
            return;
        }
        for(Player p : mc.level.players())
        {
            if(!(p instanceof AbstractClientPlayer player))
            {
                continue;
            }
            MorphInfo info = MorphApi.getMorphInfo(player);
            sync(mc, player, info.getEntity());
            if(info.isTransitioning())
            {
                sync(mc, player, info.getPrevEntity());
            }
        }

        if(!ClientState.PENDING.isEmpty())
        {
            var it = ClientState.PENDING.entrySet().iterator();
            while(it.hasNext())
            {
                var e = it.next();
                Player p = mc.level.getPlayerByUUID(e.getKey());
                if(p != null)
                {
                    ClientPackets.applySyncMorph(p, e.getValue());
                    it.remove();
                }
            }
        }
    }

    private static void driveDragon(net.minecraft.world.entity.boss.enderdragon.EnderDragon d, AbstractClientPlayer p)
    {
        d.tickCount = p.tickCount;
        d.oFlapTime = d.flapTime;
        net.minecraft.world.phys.Vec3 v = p.getDeltaMovement();
        float f = 0.2F / ((float)v.horizontalDistance() * 10F + 1F);
        f *= (float)Math.pow(2D, v.y);
        d.flapTime += p.onGround() ? 0.1F : f;
        if(d.posPointer < 0)
        {
            for(int i = 0; i < d.positions.length; i++)
            {
                d.positions[i][0] = p.getYRot() + 180F;
                d.positions[i][1] = p.getY();
            }
            d.posPointer = 0;
        }
        d.posPointer = (d.posPointer + 1) % d.positions.length;
        d.positions[d.posPointer][0] = p.getYRot() + 180F;
        d.positions[d.posPointer][1] = p.getY();
    }

    private static void sync(Minecraft mc, AbstractClientPlayer p, LivingEntity e)
    {
        if(e == null)
        {
            return;
        }
        if(e instanceof net.minecraft.world.entity.animal.axolotl.Axolotl)
        {
            float[] tm = TEMPO.computeIfAbsent(e, k -> new float[3]);
            tm[2] = (float)Math.sqrt(Math.pow(p.getX() - p.xo, 2) + Math.pow(p.getY() - p.yo, 2) + Math.pow(p.getZ() - p.zo, 2));
        }
        e.setPos(p.getX(), p.getY(), p.getZ());
        e.xo = p.xo;
        e.yo = p.yo;
        e.zo = p.zo;
        e.xOld = p.xOld;
        e.yOld = p.yOld;
        e.zOld = p.zOld;
        e.setOnGround(p.onGround());
        SitSupport.apply(e, ClientState.SITTING.contains(p.getUUID()));
        if(!SitSupport.ownsPose(e)) e.setPose(p.getPose() == Pose.SWIMMING || p.getPose() == Pose.FALL_FLYING || p.getPose() == Pose.SPIN_ATTACK ? Pose.STANDING : p.getPose());
        e.setSprinting(p.isSprinting());
        e.setShiftKeyDown(p.isShiftKeyDown());
        e.setSwimming(p.isSwimming());
        e.setInvisible(p.isInvisible());
        e.setSilent(Morph.configServer != null && Morph.configServer.silentMorphs);
        e.setCustomName(p.getDisplayName());
        e.setCustomNameVisible(p != mc.player && !com.iiampanda.morph.common.morph.BucketHold.VIEW.contains(p.getUUID()));
        for(EquipmentSlot slot : EquipmentSlot.values())
        {
            e.setItemSlot(slot, p.getItemBySlot(slot));
        }

        if(e instanceof net.minecraft.world.entity.monster.Strider strider)
        {
            strider.setSuffocating(!(p.isInLava() || p.getFluidHeight(net.minecraft.tags.FluidTags.LAVA) > 0D));
        }
        if(e instanceof net.minecraft.world.entity.animal.SnowGolem golem)
        {
            boolean had = ORIGINAL_PUMPKIN.computeIfAbsent(golem, k -> golem.hasPumpkin());
            golem.setPumpkin(had && !ClientState.SHEARED.contains(p.getUUID()));
        }
        if(e instanceof net.minecraft.world.entity.animal.Sheep sheep)
        {
            sheep.setSheared(ClientState.SHEARED.contains(p.getUUID()));
        }
        boolean saddleRide = false;
        for(var t : MorphApi.getMorphInfo(p).traits)
        {
            if(t instanceof com.iiampanda.morph.api.mob.trait.ability.RideableAbility)
            {
                saddleRide = true;
            }
        }
        if(saddleRide && e instanceof net.minecraft.world.entity.Saddleable saddleable)
        {
            boolean wantSaddle = p.getOffhandItem().is(net.minecraft.world.item.Items.SADDLE);
            if(wantSaddle && !saddleable.isSaddled())
            {
                saddleable.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), null);
            }
            else if(!wantSaddle && saddleable.isSaddled() && e == MorphApi.getMorphInfo(p).getEntity())
            {
                MorphApi.getMorphInfo(p).resetEntity();
            }
        }
        if(e instanceof net.minecraft.world.entity.animal.armadillo.Armadillo armadillo)
        {

            var state = armadillo.getState();
            if(com.iiampanda.morph.api.mob.trait.ability.SpecialRollUpAbility.isRolled(p))
            {
                if(state == net.minecraft.world.entity.animal.armadillo.Armadillo.ArmadilloState.IDLE || state == net.minecraft.world.entity.animal.armadillo.Armadillo.ArmadilloState.UNROLLING)
                {
                    armadillo.switchToState(net.minecraft.world.entity.animal.armadillo.Armadillo.ArmadilloState.ROLLING);
                }
            }
            else if(state == net.minecraft.world.entity.animal.armadillo.Armadillo.ArmadilloState.ROLLING || state == net.minecraft.world.entity.animal.armadillo.Armadillo.ArmadilloState.SCARED)
            {
                armadillo.switchToState(net.minecraft.world.entity.animal.armadillo.Armadillo.ArmadilloState.UNROLLING);
            }
            else if(state == net.minecraft.world.entity.animal.armadillo.Armadillo.ArmadilloState.UNROLLING && !armadillo.shouldHideInShell())
            {
                armadillo.switchToState(net.minecraft.world.entity.animal.armadillo.Armadillo.ArmadilloState.IDLE);
            }
        }
        String typeKey = e.getType().toString();
        boolean ticked = false;
        if(e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon)
        {
            driveDragon(dragon, p);
            ticked = true;
        }
        else if(!TICK_FAILED.contains(typeKey))
        {
            try
            {
                e.tickCount = p.tickCount - 1;
                e.tick();
                ticked = true;
            }
            catch(Throwable t)
            {
                TICK_FAILED.add(typeKey);
                Morph.LOGGER.warn("Ticking the stand-in for {} failed, animations will be limited", typeKey, t);
            }
        }
        else
        {
            e.tickCount = p.tickCount;
        }

        e.setPos(p.getX(), p.getY(), p.getZ());
        e.xo = p.xo;
        e.yo = p.yo;
        e.zo = p.zo;

        if(e instanceof net.minecraft.world.entity.animal.Squid squid && squid.tentacleMovement >= 6.2831855F)
        {
            squid.handleEntityEvent((byte)19);
        }

        float flip = e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon ? 180F : 0F;
        e.setYRot(p.getYRot() + flip);
        e.yRotO = p.yRotO + flip;
        e.setXRot(p.getXRot());
        e.xRotO = p.xRotO;
        e.yBodyRot = p.yBodyRot + flip;
        e.yBodyRotO = p.yBodyRotO + flip;
        e.yHeadRot = p.yHeadRot + flip;
        e.yHeadRotO = p.yHeadRotO + flip;
        if(e instanceof net.minecraft.world.entity.monster.Shulker)
        {
            lockShulker(p, e);
        }

        e.setDeltaMovement(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);

        boolean bcSwing = PalCompat.betterCombatAttacking(p);
        boolean swingingNow = p.swinging || bcSwing;
        boolean was = WAS_SWINGING.getOrDefault(e, false);
        if(swingingNow && !was)
        {
            try
            {
                e.handleEntityEvent((byte)4);
            }
            catch(Throwable ignored) {}
            ModAnimations.attack(e);
            if(bcSwing && !p.swinging)
            {
                BC_START.put(e, p.tickCount);
            }
            if(com.iiampanda.morph.client.render.MorphBlocks.modded(e))
            {

                try
                {
                    e.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                catch(Throwable ignored) {}
            }
        }
        if (WAS_SWINGING.size() > 32) WAS_SWINGING.clear();
        WAS_SWINGING.put(e, swingingNow);
        e.swinging = p.swinging;
        e.swingTime = p.swingTime;
        e.swingingArm = p.swingingArm;
        e.attackAnim = p.attackAnim;
        e.oAttackAnim = p.oAttackAnim;
        Integer bcStart = BC_START.get(e);
        if(bcStart != null && !p.swinging)
        {

            int age = p.tickCount - bcStart;
            if(age >= 0 && age < 6)
            {
                e.swinging = true;
                e.swingTime = age;
                e.swingingArm = net.minecraft.world.InteractionHand.MAIN_HAND;
                e.attackAnim = (age + 1) / 6F;
                e.oAttackAnim = age / 6F;
            }
            else
            {
                BC_START.remove(e);
            }
        }
        e.hurtTime = p.hurtTime;
        e.hurtDuration = p.hurtDuration;
        e.deathTime = p.deathTime;
        if(!ticked)
        {
            e.walkAnimation.update(p.walkAnimation.speed(), 1F);
        }
        if(e instanceof Mob mob)
        {
            mob.setAggressive(swingingNow || p.isUsingItem());
        }
    }
}
