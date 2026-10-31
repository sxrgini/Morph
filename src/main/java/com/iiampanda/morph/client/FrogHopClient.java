package com.iiampanda.morph.client;

import com.iiampanda.morph.api.mob.trait.ability.SpecialFrogHopAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FrogHopClient
{
    private static int held;

    public static float charge;

    private FrogHopClient() {}

    public static boolean isLocal(Player p)
    {
        return Minecraft.getInstance().player == p;
    }

    public static void reset(Player p)
    {
        if(isLocal(p))
        {
            held = 0;
            charge = 0F;
        }
    }

    static double speedFor(double height)
    {
        double lo = 0.42D, hi = 1.2D;
        for(int i = 0; i < 24; i++)
        {
            double mid = (lo + hi) / 2D;
            if(peak(mid) < height) lo = mid; else hi = mid;
        }
        return hi;
    }

    private static double peak(double v)
    {
        double y = 0D, max = 0D;
        for(int i = 0; i < 80 && v > -3D; i++)
        {
            y += v;
            max = Math.max(max, y);
            v = (v - 0.08D) * 0.98D;
            if(y <= 0D) break;
        }
        return max;
    }

    public static void tick(Player p, SpecialFrogHopAbility hop)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player != p)
        {
            return;
        }
        boolean down = mc.options.keyJump.isDown() && mc.gui.screen() == null;
        if(p.onGround() && !p.isInWater() && down)
        {
            held = Math.min(held + 1, hop.fullCharge());
            charge = held / (float)hop.fullCharge();
        }
        else
        {
            if(held > 0 && p.onGround() && !p.isInWater() && !down)
            {
                double base = 0.42D;
                double v = held <= 2 ? base : base + (speedFor(hop.height()) - base) * (held / (double)hop.fullCharge());
                Vec3 d = p.getDeltaMovement();
                if(p.isSprinting())
                {
                    float yaw = p.getYRot() * ((float)Math.PI / 180F);
                    d = d.add(-Math.sin(yaw) * 0.2D, 0D, Math.cos(yaw) * 0.2D);
                }
                p.setDeltaMovement(d.x, Math.max(d.y, v), d.z);
            }
            held = 0;
            charge = 0F;
        }
    }
}
