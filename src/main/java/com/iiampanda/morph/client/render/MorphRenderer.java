package com.iiampanda.morph.client.render;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.common.Morph;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public final class MorphRenderer
{
    private static boolean reentry;
    private static final Set<String> BROKEN = new HashSet<>();

    private MorphRenderer() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean render(PlayerRenderer vanilla, AbstractClientPlayer player, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light)
    {
        if(reentry || com.iiampanda.morph.client.PalCompat.isFirstPersonPass(null))
        {
            return false;
        }
        MorphInfo info = MorphApi.getMorphInfo(player);
        if(!info.isMorphed() && !info.isTransitioning())
        {
            return false;
        }
        float t = info.getProgress(partial);
        if(!info.isTransitioning())
        {
            return drawEntity(vanilla, info.getEntity(), player, yaw, partial, ps, buffers, light, 1F);
        }

        LivingEntity oldE = info.prevVariant == null ? null : info.getPrevEntity();
        LivingEntity newE = info.isMorphed() ? info.getEntity() : null;
        float middle = Math.max(0F, Math.min(1F, (t - 0.125F) / 0.75F));
        if(middle <= 0F)
        {
            drawEntity(vanilla, oldE, player, yaw, partial, ps, buffers, light, 1F);
            drawSkin(capture(vanilla, oldE, player, yaw, partial, light).blocks, ps, buffers, light, MorphBlocks.sine(t / 0.125F));
        }
        else if(middle >= 1F)
        {
            drawEntity(vanilla, newE, player, yaw, partial, ps, buffers, light, 1F);
            drawSkin(capture(vanilla, newE, player, yaw, partial, light).blocks, ps, buffers, light, 1F - MorphBlocks.sine((t - 0.875F) / 0.125F));
        }
        else
        {
            MorphBlocks from = capture(vanilla, oldE, player, yaw, partial, light);
            MorphBlocks to = capture(vanilla, newE, player, yaw, partial, light);
            drawSkin(MorphBlocks.blend(from, to, MorphBlocks.sine(middle)), ps, buffers, light, 1F);
        }
        return true;
    }

    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("morph", "textures/skin/morphskin.png");

    private static void drawSkin(java.util.List<MorphBlocks.Block> blocks, PoseStack ps, MultiBufferSource buffers, int light, float alpha)
    {
        if(alpha > 0.003F && !blocks.isEmpty())
        {
            MorphBlocks.emit(blocks, ps.last(), buffers.getBuffer(RenderType.entityTranslucent(SKIN)), light, alpha);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static MorphBlocks capture(PlayerRenderer vanilla, LivingEntity entity, AbstractClientPlayer player, float yaw, float partial, int light)
    {
        MorphBlocks result = new MorphBlocks();
        boolean playerBody = entity == null || BROKEN.contains(entity.getType().toString());
        result.recordQuads = !playerBody && MorphBlocks.modded(entity);
        MorphBlocks.capturing = result;
        boolean wasReentry = reentry;
        try
        {
            PoseStack scratch = new PoseStack();
            if(!playerBody)
            {
                EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
                if(renderer != null)
                {
                    renderer.render(entity, yaw, partial, scratch, DISCARD, light);
                    return result;
                }
            }
            reentry = true;
            vanilla.render(player, yaw, partial, scratch, DISCARD, light);
        }
        catch(Throwable t)
        {

        }
        finally
        {
            reentry = wasReentry;
            MorphBlocks.capturing = null;
            result.finishCapture(playerBody ? null : entity);
        }
        return result;
    }

    private static final VertexConsumer SINK = new VertexConsumer()
    {
        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    };

    static final MultiBufferSource DISCARD = type ->
    {
        MorphBlocks c = MorphBlocks.capturing;
        VertexConsumer r = c == null ? null : c.rec(type);
        return r != null ? r : SINK;
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean replaySwing(EntityRenderer renderer, LivingEntity entity, AbstractClientPlayer player, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light)
    {
        MorphBlocks cap = new MorphBlocks();
        cap.recordQuads = true;
        MorphBlocks.capturing = cap;
        try
        {
            renderer.render(entity, yaw, partial, new PoseStack(), DISCARD, light);
        }
        catch(Throwable t)
        {
            return false;
        }
        finally
        {
            MorphBlocks.capturing = null;
        }
        if(!cap.blocks.isEmpty() || cap.quads.quads.size() < 8)
        {
            return false;
        }
        float body = net.minecraft.util.Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
        float swing = (float)Math.toRadians(100D) * net.minecraft.util.Mth.sin(player.getAttackAnim(partial) * (float)Math.PI);
        cap.quads.rotateY(-(180F - body));
        cap.quads.swingArm(player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT, swing);
        cap.quads.rotateY(180F - body);
        for(MorphQuads.Quad q : cap.quads.quads)
        {
            MorphQuads.emitQuad(q, ps.last(), buffers.getBuffer((RenderType)q.type), light);
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean drawEntity(PlayerRenderer vanilla, LivingEntity entity, AbstractClientPlayer player, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light, float scale)
    {
        boolean playerBody = entity == null || BROKEN.contains(entity.getType().toString());
        if(!playerBody)
        {
            EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if(renderer == null)
            {
                BROKEN.add(entity.getType().toString());
                playerBody = true;
            }
            else
            {
                ps.pushPose();
                try
                {
                    ps.scale(scale, scale, scale);
                    if(player.swinging && MorphBlocks.modded(entity) && replaySwing(renderer, entity, player, yaw, partial, ps, buffers, light))
                    {
                        return true;
                    }
                    renderer.render(entity, yaw, partial, ps, buffers, light);
                    return true;
                }
                catch(Throwable t)
                {
                    BROKEN.add(entity.getType().toString());
                    Morph.LOGGER.error("Could not render the morph {}; showing the player instead", entity.getType(), t);
                    playerBody = true;
                }
                finally
                {
                    ps.popPose();
                }
            }
        }
        if(playerBody)
        {
            ps.pushPose();
            ps.scale(scale, scale, scale);
            reentry = true;
            try
            {
                vanilla.render(player, yaw, partial, ps, buffers, light);
            }
            finally
            {
                reentry = false;
                ps.popPose();
            }
            return true;
        }
        return false;
    }
}
