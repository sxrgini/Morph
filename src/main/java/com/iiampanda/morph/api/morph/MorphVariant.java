package com.iiampanda.morph.api.morph;

import com.iiampanda.morph.api.MorphEntityAccess;
import com.iiampanda.morph.util.NbtUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * One acquirable "form": an entity type, the NBT that makes it look/behave like that variant (cat type, sheep colour, horse markings...)
 * and whether it is the baby version. Two variants are equal when type, filtered NBT and baby flag are all equal.
 */
public final class MorphVariant
{
    public final Identifier type;
    public final CompoundTag nbt;
    public final boolean baby;

    private String key;

    public MorphVariant(Identifier type, CompoundTag nbt, boolean baby)
    {
        this.type = type;
        this.nbt = nbt == null ? new CompoundTag() : nbt;
        this.baby = baby;
    }

    public MorphVariant asBaby(boolean baby)
    {
        return new MorphVariant(type, nbt.copy(), baby);
    }

    public String key()
    {
        if(key == null)
        {
            key = type + (baby ? "|baby|" : "|adult|") + nbt;
        }
        return key;
    }

    @Nullable
    public EntityType<?> entityType()
    {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(type).orElse(null);
    }

    /**
     * Builds the (never world-added) entity that stands in for the player: used for rendering, for sizing and for tick-driven things such as sounds.
     */
    @Nullable
    private static final java.util.concurrent.atomic.AtomicInteger FAKE_IDS = new java.util.concurrent.atomic.AtomicInteger(-1000);

    public LivingEntity createEntity(Level level, @Nullable UUID playerId)
    {
        EntityType<?> entityType = entityType();
        if(entityType == null)
        {
            return null;
        }
        try
        {
            Entity entity = entityType.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if(!(entity instanceof LivingEntity living))
            {
                return null;
            }
            if(level.isClientSide())
            {
                // client levels hand out no entity ids, and 26.x throws when an entity without one is asked for it (rendering, hashing, ticking)
                living.setId(FAKE_IDS.getAndDecrement());
            }
            if(playerId != null)
            {
                ((MorphEntityAccess)living).morph$setMorphPlayerId(playerId);
            }
            if(!nbt.isEmpty())
            {
                CompoundTag full = NbtUtil.save(living);
                NbtUtil.putAll(full, nbt);
                NbtUtil.load(living, full);
            }
            if(living instanceof Mob mob)
            {
                mob.setBaby(baby);
                mob.setNoAi(true);
                mob.setPersistenceRequired();
            }
            living.setPermanentlyInvulnerable(true);
            return living;
        }
        catch(Throwable t)
        {
            com.iiampanda.morph.common.Morph.LOGGER.error("Could not create the stand-in entity for {}", type, t);
            return null;
        }
    }

    public MutableComponent displayName()
    {
        EntityType<?> t = entityType();
        MutableComponent name = t == null ? Component.literal(type.toString()) : t.getDescription().copy();
        if(baby)
        {
            name = Component.translatable("morph.variant.baby", name);
        }
        return name;
    }

    /** Short text telling variants of one mob apart ("Tabby", "Baby", "Color 3"). */
    public String variantLabel()
    {
        java.util.List<String> parts = new java.util.ArrayList<>();
        java.util.List<String> keys = new java.util.ArrayList<>(nbt.keySet());
        java.util.Collections.sort(keys);
        for(String key : keys)
        {
            net.minecraft.nbt.Tag tag = nbt.get(key);
            if(tag == null)
            {
                continue;
            }
            if(tag instanceof net.minecraft.nbt.StringTag st)
            {
                String v = st.value();
                int colon = v.lastIndexOf(':');
                v = v.substring(colon + 1).replace('/', ' ').replace('_', ' ');
                parts.add(titleCase(v));
            }
            else if(tag instanceof net.minecraft.nbt.NumericTag nt)
            {
                if(key.toLowerCase(java.util.Locale.ROOT).contains("color") && nt.intValue() >= 0 && nt.intValue() < 16)
                {
                    parts.add(titleCase(net.minecraft.world.item.DyeColor.byId(nt.intValue()).getName().replace('_', ' ')));
                }
                else
                {
                    parts.add(titleCase(key) + " " + nt.intValue());
                }
            }
        }
        if(baby)
        {
            parts.add("Baby");
        }
        return parts.isEmpty() ? "Default" : String.join(", ", parts);
    }

    private static String titleCase(String s)
    {
        StringBuilder sb = new StringBuilder();
        for(String w : s.split(" "))
        {
            if(!w.isEmpty())
            {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    public CompoundTag write(CompoundTag tag)
    {
        tag.putString("type", type.toString());
        tag.putBoolean("baby", baby);
        tag.put("nbt", nbt.copy());
        return tag;
    }

    public CompoundTag write()
    {
        return write(new CompoundTag());
    }

    @Nullable
    public static MorphVariant read(CompoundTag tag)
    {
        Identifier type = Identifier.tryParse(tag.getStringOr("type", ""));
        if(type == null)
        {
            return null;
        }
        return new MorphVariant(type, tag.getCompoundOrEmpty("nbt").copy(), tag.getBooleanOr("baby", false));
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof MorphVariant v && v.key().equals(key());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(key());
    }

    @Override
    public String toString()
    {
        return key();
    }
}
