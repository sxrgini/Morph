package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.phys.Vec3;

public class SpecialFangsAbility extends SpecialAbility<SpecialFangsAbility>
{
    public Integer count;
    public Double spacing;

    public SpecialFangsAbility() { type = "abilitySpecialFangs"; }

    @Override protected int defaultCooldown() { return 100; }

    @Override
    protected boolean activate(ServerPlayer p)
    {
        ServerLevel level = p.level();
        int n = Math.max(1, count == null ? 12 : count);
        double gap = spacing == null ? 1.4D : spacing;
        Vec3 dir = new Vec3(p.getLookAngle().x, 0D, p.getLookAngle().z).normalize();
        float yaw = (float)Math.atan2(dir.z, dir.x);
        int minY = p.getBlockY() - 4;
        for(int i = 1; i <= n; i++)
        {
            double x = p.getX() + dir.x * gap * i;
            double z = p.getZ() + dir.z * gap * i;
            BlockPos pos = BlockPos.containing(x, p.getY() + 2D, z);
            boolean found = false;
            double y = 0D;
            while(pos.getY() >= minY)
            {
                BlockPos below = pos.below();
                if(level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP))
                {
                    if(!level.isEmptyBlock(pos))
                    {
                        var shape = level.getBlockState(pos).getCollisionShape(level, pos);
                        if(!shape.isEmpty())
                        {
                            y = shape.max(net.minecraft.core.Direction.Axis.Y);
                        }
                    }
                    y += pos.getY();
                    found = true;
                    break;
                }
                pos = below;
            }
            if(found)
            {
                level.addFreshEntity(new EvokerFangs(level, x, y, z, yaw, i * 2, p));
            }
        }
        play(p, SoundEvents.EVOKER_CAST_SPELL, 1F);
        return true;
    }
}
