package com.iiampanda.morph.common.mode;

import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.morph.MorphVariant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

public interface MorphMode
{
    String name();

    /** Killing a mob while this mode is active. */
    void onKill(ServerPlayer player, LivingEntity killed);

    boolean canShowSelector(Player player);

    /** May this player start a morph right now (selector or key)? */
    boolean canMorph(Player player);

    /** Whether the player is allowed to morph at all, ignoring whether a morph is currently in progress. */
    boolean canMorphEver(Player player);

    boolean canAcquire(Player player, MorphVariant variant);

    ArrayList<Trait<?>> traitsFor(Player player, MorphVariant variant);

    /** Default mode only: abilities need to be switched on and cost biomass. */
    default boolean abilitiesNeedActivation() { return false; }

    default boolean usesBiomass() { return false; }
}
