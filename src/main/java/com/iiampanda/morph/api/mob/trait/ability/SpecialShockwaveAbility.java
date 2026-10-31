package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class SpecialShockwaveAbility extends SpecialAbility<SpecialShockwaveAbility>
{
    public Double radius;
    public Double damage;
    public Double knockback;

    public String effect;
    public Integer effectDuration;
    public Integer effectAmplifier;
    public Boolean onlyPlayers;

    public SpecialShockwaveAbility() { type = "abilitySpecialShockwave"; }

    @Override
    protected boolean activate(ServerPlayer p)
    {
        ServerLevel level = p.level();
        double r = radius == null ? 8D : radius;
        float dmg = (float)(damage == null ? 0D : damage);
        double kb = knockback == null ? 1.5D : knockback;
        MobEffect eff = null;
        if(effect != null && !effect.isEmpty())
        {
            Identifier id = Identifier.tryParse(effect);
            if(id != null)
            {
                eff = BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
            }
        }
        for(int i = 0; i < 40; i++)
        {
            double a = i / 40D * Math.PI * 2D;
            level.sendParticles(ParticleTypes.CLOUD, p.getX() + Math.cos(a) * r * 0.9D, p.getY() + 0.2D, p.getZ() + Math.sin(a) * r * 0.9D, 1, 0D, 0.05D, 0D, 0.02D);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.5D, p.getZ(), 1, 0D, 0D, 0D, 0D);
        for(LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e != p && e.isAlive() && !e.isSpectator()))
        {
            if(t.distanceTo(p) > r || (onlyPlayers != null && onlyPlayers && !(t instanceof Player)))
            {
                continue;
            }
            if(dmg > 0F)
            {
                t.hurt(level.damageSources().playerAttack(p), dmg);
            }
            if(kb > 0D)
            {
                Vec3 away = t.position().subtract(p.position()).multiply(1D, 0D, 1D).normalize();
                double resist = 1D - t.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                t.push(away.x * kb * resist, 0.4D * resist, away.z * kb * resist);
                t.hurtMarked = true;
            }
            if(eff != null)
            {
                t.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(eff), effectDuration == null ? 200 : effectDuration, effectAmplifier == null ? 0 : effectAmplifier));
            }
        }
        return true;
    }
}
