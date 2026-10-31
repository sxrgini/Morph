package com.iiampanda.morph.client.render;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.common.Morph;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MorphRenderer
{
    private record Source(AbstractClientPlayer player, float partial) {}

    private static boolean reentry;
    private static final Set<String> BROKEN = new HashSet<>();
    private static final Map<AvatarRenderState, Source> SOURCES = new WeakHashMap<>();

    private MorphRenderer() {}

    public static void remember(Avatar entity, AvatarRenderState state, float partial)
    {
        if(entity instanceof AbstractClientPlayer player)
        {
            SOURCES.put(state, new Source(player, partial));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean submit(LivingEntityRenderer vanilla, LivingEntityRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera)
    {
        if(reentry || com.iiampanda.morph.client.PalCompat.isFirstPersonPass(state) || !(state instanceof AvatarRenderState avatar))
        {
            return false;
        }
        Source src = SOURCES.get(avatar);
        if(src == null)
        {
            return false;
        }
        AbstractClientPlayer player = src.player();
        float partial = src.partial();
        MorphInfo info = MorphApi.getMorphInfo(player);
        if(!info.isMorphed() && !info.isTransitioning())
        {
            return false;
        }
        float t = info.getProgress(partial);
        if(!info.isTransitioning())
        {
            return drawEntity(vanilla, state, info.getEntity(), partial, ps, collector, camera, 1F);
        }

        LivingEntity oldE = info.prevVariant == null ? null : info.getPrevEntity();
        LivingEntity newE = info.isMorphed() ? info.getEntity() : null;
        float middle = Math.max(0F, Math.min(1F, (t - 0.125F) / 0.75F));
        if(middle <= 0F)
        {
            drawEntity(vanilla, state, oldE, partial, ps, collector, camera, 1F);
            drawSkin(capture(vanilla, state, oldE, partial, camera).blocks, ps, collector, MorphBlocks.sine(t / 0.125F), state.lightCoords);
        }
        else if(middle >= 1F)
        {
            drawEntity(vanilla, state, newE, partial, ps, collector, camera, 1F);
            drawSkin(capture(vanilla, state, newE, partial, camera).blocks, ps, collector, 1F - MorphBlocks.sine((t - 0.875F) / 0.125F), state.lightCoords);
        }
        else
        {
            MorphBlocks from = capture(vanilla, state, oldE, partial, camera);
            MorphBlocks to = capture(vanilla, state, newE, partial, camera);
            drawSkin(MorphBlocks.blend(from, to, MorphBlocks.sine(middle)), ps, collector, 1F, state.lightCoords);
        }
        return true;
    }

    private static final Identifier SKIN = Identifier.fromNamespaceAndPath("morph", "textures/skin/morphskin.png");

    private static void drawSkin(java.util.List<MorphBlocks.Block> blocks, PoseStack ps, SubmitNodeCollector collector, float alpha, int light)
    {
        if(alpha > 0.003F && !blocks.isEmpty())
        {
            collector.submitCustomGeometry(ps, net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SKIN), (pose, vc) -> MorphBlocks.emit(blocks, pose, vc, light, alpha));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static MorphBlocks capture(LivingEntityRenderer vanilla, LivingEntityRenderState playerState, LivingEntity entity, float partial, CameraRenderState camera)
    {
        MorphBlocks result = new MorphBlocks();
        boolean playerBody = entity == null || BROKEN.contains(entity.getType().toString());
        result.recordQuads = !playerBody && MorphBlocks.modded(entity);
        boolean wasReentry = reentry;
        try
        {
            PoseStack scratch = new PoseStack();
            SubmitNodeCollector sink = MorphBlocks.collector(result);
            if(!playerBody)
            {
                EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
                if(renderer != null)
                {
                    EntityRenderState state = renderer.createRenderState(entity, partial);
                    state.shadowPieces.clear();
                    renderer.submit(state, scratch, sink, camera);
                    return result;
                }
            }
            reentry = true;
            vanilla.submit(playerState, scratch, sink, camera);
        }
        catch(Throwable t)
        {

        }
        finally
        {
            reentry = wasReentry;
            result.finishCapture(playerBody ? null : entity);
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean replaySwing(EntityRenderer renderer, LivingEntity entity, LivingEntityRenderState playerState, float partial, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera)
    {
        Source src = playerState instanceof AvatarRenderState av ? SOURCES.get(av) : null;
        if(src == null || !src.player().swinging || !MorphBlocks.modded(entity))
        {
            return false;
        }
        MorphBlocks cap = new MorphBlocks();
        cap.recordQuads = true;
        try
        {
            EntityRenderState state = renderer.createRenderState(entity, partial);
            state.shadowPieces.clear();
            renderer.submit(state, new PoseStack(), MorphBlocks.collector(cap), camera);
        }
        catch(Throwable t)
        {
            return false;
        }
        if(!cap.blocks.isEmpty() || cap.quads.quads.size() < 8)
        {
            return false;
        }
        float body = net.minecraft.util.Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
        float swing = (float)Math.toRadians(100D) * net.minecraft.util.Mth.sin(src.player().getAttackAnim(partial) * (float)Math.PI);
        cap.quads.rotateY(-(180F - body));
        cap.quads.swingArm(src.player().getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT, swing);
        cap.quads.rotateY(180F - body);
        int light = playerState.lightCoords;
        for(MorphQuads.Quad q : cap.quads.quads)
        {
            collector.submitCustomGeometry(ps, (net.minecraft.client.renderer.rendertype.RenderType)q.type, (pose, vc) -> MorphQuads.emitQuad(q, pose, vc, light));
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean drawEntity(LivingEntityRenderer vanilla, LivingEntityRenderState playerState, LivingEntity entity, float partial, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera, float scale)
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
                    if(replaySwing(renderer, entity, playerState, partial, ps, collector, camera))
                    {
                        return true;
                    }
                    EntityRenderState state = renderer.createRenderState(entity, partial);
                    state.shadowPieces.clear();
                    renderer.submit(state, ps, collector, camera);
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
                vanilla.submit(playerState, ps, collector, camera);
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
