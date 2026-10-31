package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SpecialSonicBoomAbility extends SpecialAbility<SpecialSonicBoomAbility>
{
    public Double range;
    public Double damage;
    public Double knockback;

    public SpecialSonicBoomAbility() { type = "abilitySpecialSonicBoom"; }

    @Override protected int defaultCooldown() { return 160; }

    @Override
    protected boolean activate(ServerPlayer p)
    {
        ServerLevel level = p.serverLevel();
        double r = range == null ? 20D : range;
        float dmg = (float)(damage == null ? 10D : damage);
        double kb = knockback == null ? 2.5D : knockback;
        Vec3 start = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        Vec3 end = start.add(dir.scale(r));
        play(p, SoundEvents.WARDEN_SONIC_CHARGE, 1F);
        play(p, SoundEvents.WARDEN_SONIC_BOOM, 1F);
        for(int i = 1; i < (int)r; i++)
        {
            Vec3 pt = start.add(dir.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, pt.x, pt.y - 0.2D, pt.z, 1, 0D, 0D, 0D, 0D);
        }
        AABB box = new AABB(start, end).inflate(1.5D);
        for(LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator()))
        {
            if(target.getBoundingBox().inflate(0.6D).clip(start, end).isPresent())
            {
                target.hurt(level.damageSources().sonicBoom(p), dmg);
                double resist = 1D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                target.push(dir.x * kb * resist, dir.y * 0.5D * resist, dir.z * kb * resist);
            }
        }
        return true;
    }
}
