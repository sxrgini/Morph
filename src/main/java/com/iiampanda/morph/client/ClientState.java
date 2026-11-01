package com.iiampanda.morph.client;

import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.common.save.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientState
{
    public static final PlayerData DATA = new PlayerData();
    public static String mode = "classic";
    public static boolean canSelect;
    public static boolean canMorph;
    public static boolean biomassUnlocked;
    public static boolean abilitiesUnlocked;

    public static final Map<UUID, Packets.SyncMorph> PENDING = new HashMap<>();

    public static final java.util.Set<UUID> SITTING = new java.util.HashSet<>();
    public static final java.util.Set<UUID> SHEARED = new java.util.HashSet<>();

    private static final Map<String, LivingEntity> GUI_ENTITIES = new HashMap<>();

    private ClientState() {}

    public static void reset()
    {
        DATA.read(new net.minecraft.nbt.CompoundTag());
        mode = "classic";
        canSelect = canMorph = biomassUnlocked = abilitiesUnlocked = false;
        PENDING.clear();
        SITTING.clear();
        GUI_ENTITIES.clear();
    }

    public static boolean oneMorph()
    {
        return mode.equals("one_morph");
    }

    public static boolean usesBiomass()
    {
        return mode.equals("default");
    }

    public static LivingEntity guiEntity(MorphVariant variant)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.level == null)
        {
            return null;
        }
        return GUI_ENTITIES.computeIfAbsent(variant.key(), k -> variant.createEntity(mc.level, null));
    }

    public static void clearGuiEntities()
    {
        GUI_ENTITIES.clear();
    }
}
