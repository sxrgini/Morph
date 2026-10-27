package com.iiampanda.morph.api.mob.trait.ability;

import com.iiampanda.morph.api.mob.trait.IEventBusRequired;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;

/** Armadillo "Roll Up": for a few seconds all incoming damage is cut (by half by default). */
public class SpecialRollUpAbility extends SpecialAbility<SpecialRollUpAbility> implements IEventBusRequired
{
    /** How long you stay rolled up, in ticks (default 100 = 5 seconds). */
    public Integer duration;
    /** Fraction of incoming damage that is blocked while rolled up (default 0.5). */
    public Double damageReduction;

    public transient long rolledUntil;

    /** Slowness this strong is never a vanilla effect, so it marks "rolled up" on every client. */
    public static final int MARKER_LEVEL = 9;

    public static boolean isRolled(Player p)
    {
        MobEffectInstance e = p.getEffect(MobEffects.SLOWNESS);
        return e != null && e.getAmplifier() >= MARKER_LEVEL;
    }

    public SpecialRollUpAbility() { type = "abilitySpecialRollUp"; }

    @Override protected int defaultCooldown() { return 300; }

    @Override
    protected boolean activate(ServerPlayer p)
    {
        int ticks = Math.max(1, duration == null ? 100 : duration);
        rolledUntil = p.level().getGameTime() + ticks;
        // an invisible, very strong Slowness pins you in place, and being a normal effect it is also what other clients see
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, MARKER_LEVEL, true, false, false));
        play(p, SoundEvents.ARMADILLO_ROLL, 1F);
        return true;
    }

    @Override
    public float modifyDamage(LivingEntity victim, DamageSource source, float amount)
    {
        if(victim == player && enabled && victim.level().getGameTime() < rolledUntil)
        {
            double cut = Math.max(0D, Math.min(1D, damageReduction == null ? 0.5D : damageReduction));
            return (float)(amount * (1D - cut));
        }
        return amount;
    }
}
