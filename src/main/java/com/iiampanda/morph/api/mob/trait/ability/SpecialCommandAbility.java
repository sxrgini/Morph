package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs commands as the player (with operator permission, no output). This lets pack makers build any special move from
 * vanilla commands or other mods' commands, e.g. "summon minecraft:lightning_bolt ^ ^ ^5". Executed at the player, so ^ ^ ^ coordinates work.
 */
public class SpecialCommandAbility extends SpecialAbility<SpecialCommandAbility>
{
    public List<String> commands = new ArrayList<>();

    public SpecialCommandAbility() { type = "abilitySpecialCommand"; }

    @Override
    protected boolean activate(ServerPlayer p)
    {
        if(commands == null || commands.isEmpty())
        {
            return false;
        }
        var server = p.server;
        var source = p.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        for(String cmd : commands)
        {
            if(cmd != null && !cmd.isBlank())
            {
                server.getCommands().performPrefixedCommand(source, cmd.startsWith("/") ? cmd.substring(1) : cmd);
            }
        }
        return true;
    }
}
