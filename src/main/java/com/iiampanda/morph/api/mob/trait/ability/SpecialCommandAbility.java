package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

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
