package com.iiampanda.morph.client;

import com.iiampanda.morph.common.packet.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.List;

public final class AcquisitionEffects
{
    private static final List<Active> ACTIVE = new ArrayList<>();

    private AcquisitionEffects() {}

    private static final class Active
    {
        final Packets.Acquire p;
        int ticks = 20;

        Active(Packets.Acquire p)
        {
            this.p = p;
        }
    }

    public static void start(Packets.Acquire payload)
    {
        ACTIVE.add(new Active(payload));
    }

    public static void tick(Minecraft mc)
    {
        if(mc.level == null || mc.player == null)
        {
            ACTIVE.clear();
            return;
        }
        ACTIVE.removeIf(a ->
        {
            double tx = mc.player.getX();
            double ty = mc.player.getY() + mc.player.getBbHeight() * 0.6D;
            double tz = mc.player.getZ();
            for(int i = 0; i < 4; i++)
            {
                double ox = a.p.x() + (mc.level.random.nextDouble() - 0.5D) * a.p.width();
                double oy = a.p.y() + mc.level.random.nextDouble() * a.p.height();
                double oz = a.p.z() + (mc.level.random.nextDouble() - 0.5D) * a.p.width();
                if(a.p.morph())
                {
                    mc.level.addParticle(ParticleTypes.ENCHANT, tx, ty, tz, ox - tx, oy - ty, oz - tz);
                }
                if(a.p.biomass())
                {
                    mc.level.addParticle(ParticleTypes.PORTAL, tx, ty, tz, ox - tx, oy - ty, oz - tz);
                }
            }
            return --a.ticks <= 0;
        });
    }
}
