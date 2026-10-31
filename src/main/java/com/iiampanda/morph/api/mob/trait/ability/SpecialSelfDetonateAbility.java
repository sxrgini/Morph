package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

public class SpecialSelfDetonateAbility extends SpecialAbility<SpecialSelfDetonateAbility>
{

    public Integer fuseTicks;

    public Double radius;

    public Boolean breakBlocks;

    public Boolean killPlayer;

    public transient int fuse;

    public SpecialSelfDetonateAbility() { type = "abilitySpecialSelfDetonate"; }

    private int fuseLength() { return Math.max(1, fuseTicks == null ? 40 : fuseTicks); }

    @Override protected int defaultCooldown() { return fuseLength() + 20; }

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
