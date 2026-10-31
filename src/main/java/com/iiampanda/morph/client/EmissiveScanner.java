package com.iiampanda.morph.client;

import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class EmissiveScanner
{
    private static final Set<String> MARKERS = Set.of("glow", "glowmask", "glowing", "emissive", "emission", "fullbright", "luminous", "eyes", "glowy");

    private EmissiveScanner() {}

    static boolean emissiveName(String path)
    {
        String[] tokens = path.replace(".png", "").split("[_/\\. -]+");
        for(int i = 0; i < tokens.length; i++)
        {
            String t = tokens[i];
            if(MARKERS.contains(t))
            {
                return true;
            }

            if(t.equals("e") && i == tokens.length - 1 && tokens.length > 1)
            {
                return true;
            }
        }
        return false;
    }

    public static void scanAndReport(Minecraft mc)
    {
        try
        {
            Map<String, List<String>> byNamespace = new HashMap<>();
            Set<String> modded = new HashSet<>();
            for(var key : BuiltInRegistries.ENTITY_TYPE.keySet())
            {
                if(!key.getNamespace().equals("minecraft"))
                {
                    modded.add(key.getNamespace());
                }
            }
            for(var entry : mc.getResourceManager().listResources("textures/entity", id -> id.getPath().endsWith(".png") && !id.getNamespace().equals("minecraft")).keySet())
            {
                if(modded.contains(entry.getNamespace()) && emissiveName(entry.getPath()))
                {
                    byNamespace.computeIfAbsent(entry.getNamespace(), k -> new ArrayList<>()).add(entry.getPath().substring("textures/entity/".length()).replace(".png", ""));
                }
            }
            List<String> found = new ArrayList<>();
            for(var key : BuiltInRegistries.ENTITY_TYPE.keySet())
            {
                List<String> textures = byNamespace.get(key.getNamespace());
                if(textures == null)
                {
                    continue;
                }
                Pattern p = Pattern.compile("(^|[/_])" + Pattern.quote(key.getPath()) + "([/_]|$)");
                for(String tex : textures)
                {
                    if(p.matcher(tex).find())
                    {
                        found.add(key.toString());
                        break;
                    }
                }
            }
            if(!found.isEmpty())
            {
                Platform.sendToServer(new Packets.ReportEmissive(found));
            }
        }
        catch(Throwable t)
        {
            com.iiampanda.morph.common.Morph.LOGGER.warn("Could not scan for emissive mob textures", t);
        }
    }
}
