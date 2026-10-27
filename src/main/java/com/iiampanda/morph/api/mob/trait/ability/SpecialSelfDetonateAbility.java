package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

/** Creeper "Self Detonate": hisses for a moment, then blows up like a creeper and kills the player who used it. */
public class SpecialSelfDetonateAbility extends SpecialAbility<SpecialSelfDetonateAbility>
{
    /** Ticks between right clicking and the explosion (a creeper's fuse is 30; default 40 = 2 seconds). */
    public Integer fuseTicks;
    /** Explosion radius (a creeper uses 3, a charged creeper 6). */
    public Double radius;
    /** Whether the explosion breaks blocks (still respects the mobGriefing game rule). Default true. */
    public Boolean breakBlocks;
    /** Whether the player dies too. Default true. */
    public Boolean killPlayer;

    public transient int fuse;

    public SpecialSelfDetonateAbility() { type = "abilitySpecialSelfDetonate"; }

    private int fuseLength() { return Math.max(1, fuseTicks == null ? 40 : fuseTicks); }

    @Override protected int defaultCooldown() { return fuseLength() + 20; }

    /** Can't be used again while the fuse is burning. */
    @Override public int getCooldown() { return Math.max(super.getCooldown(), fuseLength() + 1); }

    @Override
    protected boolean activate(ServerPlayer p)
    {
        if(fuse > 0)
        {
            return false;
        }
        fuse = fuseLength();
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CREEPER_PRIMED, net.minecraft.sounds.SoundSource.HOSTILE, 1F, 0.5F);
        return true;
    }

    @Override
    public void tick(float strength)
    {
        if(fuse <= 0 || !(player instanceof ServerPlayer p))
        {
            return;
        }
        if(!p.isAlive())
        {
            fuse = 0;
            return;
        }
        if(--fuse > 0)
        {
            if(fuse % 5 == 0)
            {
                p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, p.getX(), p.getY() + 1D, p.getZ(), 4, 0.3D, 0.4D, 0.3D, 0.01D);
            }
            return;
        }
        ServerLevel level = p.level();
        float r = (float)(radius == null ? 3D : radius);
        boolean blocks = breakBlocks == null || breakBlocks;
        level.explode(p, p.getX(), p.getY(0.0625D), p.getZ(), r, blocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        if(killPlayer == null || killPlayer)
        {
            p.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
        }
    }
}
