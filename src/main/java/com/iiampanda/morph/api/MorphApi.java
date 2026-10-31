package com.iiampanda.morph.api;

import com.iiampanda.morph.api.morph.MorphInfo;
import net.minecraft.world.entity.player.Player;

public final class MorphApi
{

    public interface Bridge
    {

        boolean consumeBiomass(Player player, double amount);

        boolean hasBiomass(Player player, double amount);
    }

    private static Bridge bridge = new Bridge()
    {
        @Override public boolean consumeBiomass(Player player, double amount) { return true; }
        @Override public boolean hasBiomass(Player player, double amount) { return true; }
    };

    private MorphApi() {}

    public static void setBridge(Bridge b) { bridge = b; }

    public static Bridge bridge() { return bridge; }

    public static MorphInfo getMorphInfo(Player player)
    {
        return ((MorphInfoHolder)player).morph$getMorphInfo();
    }

    public static boolean isMorphed(Player player)
    {
        return getMorphInfo(player).isMorphed();
    }
}
