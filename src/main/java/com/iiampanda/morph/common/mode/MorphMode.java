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

    void onKill(ServerPlayer player, LivingEntity killed);

    boolean canShowSelector(Player player);

    boolean canMorph(Player player);

    boolean canMorphEver(Player player);

    boolean canAcquire(Player player, MorphVariant variant);

    ArrayList<Trait<?>> traitsFor(Player player, MorphVariant variant);

    default boolean abilitiesNeedActivation() { return false; }

    default boolean usesBiomass() { return false; }
}
