package com.iiampanda.morph.common.mode;

import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.morph.MorphVariant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/** Morphs are only handed out and applied by commands. */
public class CommandMode extends BaseMode
{
    private final boolean allowSelection;

    public CommandMode(boolean allowSelection)
    {
        this.allowSelection = allowSelection;
    }

    @Override public String name() { return allowSelection ? "command_allow_selection" : "command_only"; }

    @Override public void onKill(ServerPlayer player, LivingEntity killed) {}

    @Override public boolean canShowSelector(Player player) { return allowSelection && selectorAllowed(player); }

    @Override public boolean canMorph(Player player) { return allowSelection && allowed(player) && notMidMorph(player); }

    @Override public boolean canMorphEver(Player player) { return allowSelection && allowed(player); }

    @Override public boolean canAcquire(Player player, MorphVariant variant) { return false; }

    @Override
    public ArrayList<Trait<?>> traitsFor(Player player, MorphVariant variant)
    {
        return profileTraits(player, variant, false);
    }
}
