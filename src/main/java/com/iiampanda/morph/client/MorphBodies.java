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

/** Keeps the stand-in mob entity of every morphed player in step with the player each tick. */
public final class MorphBodies
{
    private static final Set<String> TICK_FAILED = new HashSet<>();
    private static final java.util.Map<LivingEntity, Boolean> WAS_SWINGING = new java.util.IdentityHashMap<>();


    private static final java.util.Map<LivingEntity, float[]> TEMPO = new java.util.WeakHashMap<>();

    /** Axolotl animations play at one fixed speed; this speeds them up to follow how fast the player really moves. */
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
        // players whose morph packet arrived before they were in the level
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

    /** The dragon's own client tick depends on server side state, so its wing beat and neck trail are driven from the player instead. */
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
        e.setPose(p.getPose() == Pose.SWIMMING || p.getPose() == Pose.FALL_FLYING || p.getPose() == Pose.SPIN_ATTACK ? Pose.STANDING : p.getPose());
        e.setSprinting(p.isSprinting());
        e.setShiftKeyDown(p.isShiftKeyDown());
        e.setSwimming(p.isSwimming());
        e.setInvisible(p.isInvisible());
        e.setSilent(Morph.configServer != null && Morph.configServer.silentMorphs);
        e.setCustomName(p.getDisplayName());
        e.setCustomNameVisible(p != mc.player);
        for(EquipmentSlot slot : EquipmentSlot.values())
        {
            e.setItemSlot(slot, p.getItemBySlot(slot));
        }

        if(e instanceof net.minecraft.world.entity.animal.armadillo.Armadillo armadillo)
        {
            // the player's roll up shows as the armadillo curling into a ball and back out
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

        // rotations and animation state are overwritten after the tick so the mob's own AI-less logic doesn't fight the player
        e.setPos(p.getX(), p.getY(), p.getZ());
        e.xo = p.xo;
        e.yo = p.yo;
        e.zo = p.zo;
        // the dragon model faces the opposite way to every other mob
        float flip = e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon ? 180F : 0F;
        e.setYRot(p.getYRot() + flip);
        e.yRotO = p.yRotO + flip;
        e.setXRot(p.getXRot());
        e.xRotO = p.xRotO;
        e.yBodyRot = p.yBodyRot + flip;
        e.yBodyRotO = p.yBodyRotO + flip;
        e.yHeadRot = p.yHeadRot + flip;
        e.yHeadRotO = p.yHeadRotO + flip;
        // models that animate while moving (dolphin tail, bats, ...) read the stand-in's velocity, taken from where the player actually moved
        e.setDeltaMovement(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);
        // mobs with a dedicated attack animation (golems, ravagers, wardens, hoglins...) play it from entity event 4
        boolean was = WAS_SWINGING.getOrDefault(e, false);
        if(p.swinging && !was)
        {
            try
            {
                e.handleEntityEvent((byte)4);
            }
            catch(Throwable ignored) {}
            if(com.iiampanda.morph.client.render.MorphBlocks.modded(e))
            {
                // other mods hook swing() to start their attack animations
                try
                {
                    e.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                catch(Throwable ignored) {}
            }
        }
        if (WAS_SWINGING.size() > 32) WAS_SWINGING.clear();
        WAS_SWINGING.put(e, p.swinging);
        e.swinging = p.swinging;
        e.swingTime = p.swingTime;
        e.swingingArm = p.swingingArm;
        e.attackAnim = p.attackAnim;
        e.oAttackAnim = p.oAttackAnim;
        e.hurtTime = p.hurtTime;
        e.hurtDuration = p.hurtDuration;
        e.deathTime = p.deathTime;
        if(!ticked)
        {
            e.walkAnimation.update(p.walkAnimation.speed(), 1F);
        }
        if(e instanceof Mob mob)
        {
            mob.setAggressive(p.swinging || p.isUsingItem());
        }
    }
}
