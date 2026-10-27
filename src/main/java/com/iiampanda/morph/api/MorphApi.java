package com.iiampanda.morph.api;

import com.iiampanda.morph.api.morph.MorphInfo;
import net.minecraft.world.entity.player.Player;

/**
 * Entry point other mods (and the traits) use to talk to Morph without depending on the implementation.
 */
public final class MorphApi
{
    /** Implemented by the common code (server: real biomass accounting, client: optimistic). */
    public interface Bridge
    {
        /** Try to spend biomass (server side). Returns false if the player could not afford it. */
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
