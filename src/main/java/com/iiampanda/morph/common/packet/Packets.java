package com.iiampanda.morph.common.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** All of Morph's network payloads. */
public final class Packets
{
    private Packets() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path)
    {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("morph", path));
    }

    private static void writeOptTag(FriendlyByteBuf buf, Optional<CompoundTag> tag)
    {
        buf.writeBoolean(tag.isPresent());
        tag.ifPresent(buf::writeNbt);
    }

    private static Optional<CompoundTag> readOptTag(FriendlyByteBuf buf)
    {
        return buf.readBoolean() ? Optional.ofNullable((CompoundTag)buf.readNbt()) : Optional.empty();
    }

    //================ SERVER -> CLIENT ================
    /** A player's morph state changed (or we started tracking them). */
    public record SyncMorph(UUID player, Optional<CompoundTag> variant, String traitsJson, int duration, boolean animate, List<String> enabledAbilities) implements CustomPacketPayload
    {
        public static final Type<SyncMorph> TYPE = typeOf("sync_morph");
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncMorph> CODEC = CustomPacketPayload.codec((v, buf) ->
        {
            buf.writeUUID(v.player);
            writeOptTag(buf, v.variant);
            buf.writeUtf(v.traitsJson, 262144);
            buf.writeVarInt(v.duration);
            buf.writeBoolean(v.animate);
            buf.writeVarInt(v.enabledAbilities.size());
            v.enabledAbilities.forEach(buf::writeUtf);
        }, buf ->
        {
            UUID id = buf.readUUID();
            Optional<CompoundTag> variant = readOptTag(buf);
            String traits = buf.readUtf(262144);
            int duration = buf.readVarInt();
            boolean animate = buf.readBoolean();
            int n = buf.readVarInt();
            List<String> list = new ArrayList<>();
            for(int i = 0; i < n; i++)
            {
                list.add(buf.readUtf());
            }
            return new SyncMorph(id, variant, traits, duration, animate, list);
        });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The receiving player's own morph list, biomass and upgrades, plus what the mode lets them do. */
    public record SyncData(CompoundTag data, String upgradesJson, String mode, boolean canSelect, boolean canMorph, boolean biomassUnlocked, boolean abilitiesUnlocked) implements CustomPacketPayload
    {
        public static final Type<SyncData> TYPE = typeOf("sync_data");
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncData> CODEC = CustomPacketPayload.codec((v, buf) ->
        {
            buf.writeNbt(v.data);
            buf.writeUtf(v.upgradesJson, 1048576);
            buf.writeUtf(v.mode);
            buf.writeBoolean(v.canSelect);
            buf.writeBoolean(v.canMorph);
            buf.writeBoolean(v.biomassUnlocked);
            buf.writeBoolean(v.abilitiesUnlocked);
        }, buf -> new SyncData((CompoundTag)buf.readNbt(), buf.readUtf(1048576), buf.readUtf(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncBiomass(double biomass) implements CustomPacketPayload
    {
        public static final Type<SyncBiomass> TYPE = typeOf("sync_biomass");
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncBiomass> CODEC = CustomPacketPayload.codec((v, buf) -> buf.writeDouble(v.biomass), buf -> new SyncBiomass(buf.readDouble()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Plays the "absorbing" effect from a dead mob to the player. */
    public record Acquire(double x, double y, double z, float width, float height, boolean morph, boolean biomass) implements CustomPacketPayload
    {
        public static final Type<Acquire> TYPE = typeOf("acquire");
        public static final StreamCodec<RegistryFriendlyByteBuf, Acquire> CODEC = CustomPacketPayload.codec((v, buf) ->
        {
            buf.writeDouble(v.x);
            buf.writeDouble(v.y);
            buf.writeDouble(v.z);
            buf.writeFloat(v.width);
            buf.writeFloat(v.height);
            buf.writeBoolean(v.morph);
            buf.writeBoolean(v.biomass);
        }, buf -> new Acquire(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    //================ CLIENT -> SERVER ================
    /** Empty = demorph. */
    public record RequestMorph(Optional<CompoundTag> variant) implements CustomPacketPayload
    {
        public static final Type<RequestMorph> TYPE = typeOf("request_morph");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestMorph> CODEC = CustomPacketPayload.codec((v, buf) -> writeOptTag(buf, v.variant), buf -> new RequestMorph(readOptTag(buf)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SetFavourite(CompoundTag variant, boolean favourite) implements CustomPacketPayload
    {
        public static final Type<SetFavourite> TYPE = typeOf("set_favourite");
        public static final StreamCodec<RegistryFriendlyByteBuf, SetFavourite> CODEC = CustomPacketPayload.codec((v, buf) ->
        {
            buf.writeNbt(v.variant);
            buf.writeBoolean(v.favourite);
        }, buf -> new SetFavourite((CompoundTag)buf.readNbt(), buf.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ForgetMorph(CompoundTag variant) implements CustomPacketPayload
    {
        public static final Type<ForgetMorph> TYPE = typeOf("forget_morph");
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgetMorph> CODEC = CustomPacketPayload.codec((v, buf) -> buf.writeNbt(v.variant), buf -> new ForgetMorph((CompoundTag)buf.readNbt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ToggleAbility(String abilityType) implements CustomPacketPayload
    {
        public static final Type<ToggleAbility> TYPE = typeOf("toggle_ability");
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleAbility> CODEC = CustomPacketPayload.codec((v, buf) -> buf.writeUtf(v.abilityType), buf -> new ToggleAbility(buf.readUtf()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PurchaseUpgrade(String id) implements CustomPacketPayload
    {
        public static final Type<PurchaseUpgrade> TYPE = typeOf("purchase_upgrade");
        public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseUpgrade> CODEC = CustomPacketPayload.codec((v, buf) -> buf.writeUtf(v.id), buf -> new PurchaseUpgrade(buf.readUtf()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UseSpecial() implements CustomPacketPayload
    {
        public static final Type<UseSpecial> TYPE = typeOf("use_special");
        public static final StreamCodec<RegistryFriendlyByteBuf, UseSpecial> CODEC = StreamCodec.unit(new UseSpecial());

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The client found glow / emissive textures for these modded mobs (see EmissiveScanner). Only operators (or the singleplayer owner) are listened to. */
    public record ReportEmissive(List<String> entities) implements CustomPacketPayload
    {
        public static final Type<ReportEmissive> TYPE = typeOf("report_emissive");
        public static final StreamCodec<RegistryFriendlyByteBuf, ReportEmissive> CODEC = CustomPacketPayload.codec((v, buf) ->
        {
            buf.writeVarInt(v.entities.size());
            v.entities.forEach(buf::writeUtf);
        }, buf ->
        {
            int n = Math.min(buf.readVarInt(), 4096);
            List<String> list = new ArrayList<>();
            for(int i = 0; i < n; i++)
            {
                list.add(buf.readUtf(256));
            }
            return new ReportEmissive(list);
        });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The player asks to sit down or stand up (only a morph that can sit does anything). */
    public record SetSit(boolean sitting) implements CustomPacketPayload
    {
        public static final Type<SetSit> TYPE = typeOf("set_sit");
        public static final StreamCodec<RegistryFriendlyByteBuf, SetSit> CODEC = CustomPacketPayload.codec((v, buf) -> buf.writeBoolean(v.sitting), buf -> new SetSit(buf.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** A morphed player sat down or stood up. */
    public record SyncSit(UUID player, boolean sitting) implements CustomPacketPayload
    {
        public static final Type<SyncSit> TYPE = typeOf("sync_sit");
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncSit> CODEC = CustomPacketPayload.codec((v, buf) ->
        {
            buf.writeUUID(v.player);
            buf.writeBoolean(v.sitting);
        }, buf -> new SyncSit(buf.readUUID(), buf.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The loader module implements this to register the payload types (and, for client bound ones, their handlers). */
    public interface Registrar
    {
        <T extends CustomPacketPayload> void s2c(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec);

        <T extends CustomPacketPayload> void c2s(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec);
    }

    /** Implemented on the client to receive the S2C payloads; the loader module forwards to it. */
    public interface ClientHandler
    {
        void handle(CustomPacketPayload payload);
    }

    public static ClientHandler clientHandler = payload -> {};

    public static void registerAll(Registrar r)
    {
        r.s2c(SyncMorph.TYPE, SyncMorph.CODEC);
        r.s2c(SyncData.TYPE, SyncData.CODEC);
        r.s2c(SyncBiomass.TYPE, SyncBiomass.CODEC);
        r.s2c(Acquire.TYPE, Acquire.CODEC);
        r.c2s(RequestMorph.TYPE, RequestMorph.CODEC);
        r.c2s(SetFavourite.TYPE, SetFavourite.CODEC);
        r.c2s(ForgetMorph.TYPE, ForgetMorph.CODEC);
        r.c2s(ToggleAbility.TYPE, ToggleAbility.CODEC);
        r.c2s(PurchaseUpgrade.TYPE, PurchaseUpgrade.CODEC);
        r.c2s(UseSpecial.TYPE, UseSpecial.CODEC);
        r.c2s(ReportEmissive.TYPE, ReportEmissive.CODEC);
        r.c2s(SetSit.TYPE, SetSit.CODEC);
        r.s2c(SyncSit.TYPE, SyncSit.CODEC);
    }
}
