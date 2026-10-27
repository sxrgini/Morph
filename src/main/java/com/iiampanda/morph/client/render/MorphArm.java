package com.iiampanda.morph.client.render;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/** First person: draws the morph's own arm in place of the player's, when the mob has one. */
public final class MorphArm
{
    private static final Set<String> BROKEN = new HashSet<>();

    private MorphArm() {}

    /** Depth first search for a part by name (the Warden's arms sit under bone/body). */
    private static ModelPart find(ModelPart part, String name)
    {
        for(java.util.Map.Entry<String, ModelPart> e : ((com.iiampanda.morph.client.mixin.ModelPartAccessor)(Object)part).morph$children().entrySet())
        {
            if(e.getKey().equals(name))
            {
                return e.getValue();
            }
            ModelPart found = find(e.getValue(), name);
            if(found != null)
            {
                return found;
            }
        }
        return null;
    }

    /** A private copy of a model part (cubes are shared, poses are not), so borrowing a leg as an arm never disturbs the body model. */
    private static ModelPart copy(ModelPart p)
    {
        com.iiampanda.morph.client.mixin.ModelPartAccessor acc = (com.iiampanda.morph.client.mixin.ModelPartAccessor)(Object)p;
        java.util.Map<String, ModelPart> kids = new java.util.LinkedHashMap<>();
        for(java.util.Map.Entry<String, ModelPart> e : acc.morph$children().entrySet())
        {
            kids.put(e.getKey(), copy(e.getValue()));
        }
        ModelPart c = new ModelPart(acc.morph$cubes(), kids);
        c.x = p.x;
        c.y = p.y;
        c.z = p.z;
        c.xRot = p.xRot;
        c.yRot = p.yRot;
        c.zRot = p.zRot;
        c.xScale = p.xScale;
        c.yScale = p.yScale;
        c.zScale = p.zScale;
        return c;
    }

    private record Limb(ModelPart part, ResourceLocation texture, MorphQuads quads) {}

    /** Work to do once vanilla has drawn the player's own arm (the static skin goes over it). */
    private static Runnable after;

    public static void finish()
    {
        Runnable r = after;
        after = null;
        if(r != null)
        {
            r.run();
        }
    }

    /** @return true if we drew the arm (the vanilla arm must not be drawn) */
    public static boolean render(PlayerRenderer vanilla, PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, HumanoidArm side)
    {
        after = null;
        MorphInfo info = MorphApi.getMorphInfo(player);
        if(!info.isMorphed() && !info.isTransitioning())
        {
            return false;
        }
        if(!info.isTransitioning())
        {
            LivingEntity entity = info.getEntity();
            Limb limb = entity == null ? null : limbOf(vanilla, entity, side);
            if(limb != null)
            {
                draw(limb, ps, buffers, light);
            }
            return true;
        }

        // same phases as the body: freeze to the static skin, blocks slide into the new arm, thaw
        float t = info.getProgress(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        LivingEntity oldE = info.prevVariant == null ? null : info.getPrevEntity();
        LivingEntity newE = info.isMorphed() ? info.getEntity() : null;
        float middle = Math.max(0F, Math.min(1F, (t - 0.125F) / 0.75F));
        if(middle > 0F && middle < 1F)
        {
            java.util.List<MorphBlocks.Block> blocks = MorphBlocks.blend(blocksOf(vanilla, player, oldE, side), blocksOf(vanilla, player, newE, side), MorphBlocks.sine(middle));
            overlay(blocks, ps, buffers, light, 1F);
            return true;
        }
        LivingEntity e = middle <= 0F ? oldE : newE;
        float alpha = middle <= 0F ? MorphBlocks.sine(t / 0.125F) : 1F - MorphBlocks.sine((t - 0.875F) / 0.125F);
        java.util.List<MorphBlocks.Block> blocks = blocksOf(vanilla, player, e, side).blocks;
        if(e == null)
        {
            after = () -> overlay(blocks, ps, buffers, light, alpha);
            return false; // the player's own arm: vanilla draws it
        }
        Limb limb = limbOf(vanilla, e, side);
        if(limb != null)
        {
            draw(limb, ps, buffers, light);
        }
        overlay(blocks, ps, buffers, light, alpha);
        return true;
    }

    private static void draw(Limb limb, PoseStack ps, MultiBufferSource buffers, int light)
    {
        if(limb.part() != null)
        {
            limb.part().render(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(limb.texture())), light, OverlayTexture.NO_OVERLAY);
            return;
        }
        for(MorphQuads.Quad q : limb.quads().quads)
        {
            MorphQuads.emitQuad(q, ps.last(), buffers.getBuffer((RenderType)q.type), light);
        }
    }

    /** Mobs without vanilla model parts (GeckoLib and the like): the quads on the arm's side of whatever the renderer draws. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Limb quadLimb(LivingEntity entity, EntityRenderer renderer, HumanoidArm side)
    {
        if(renderer == null || !MorphBlocks.modded(entity))
        {
            return null;
        }
        MorphBlocks cap = new MorphBlocks();
        cap.recordQuads = true;
        MorphBlocks.capturing = cap;
        try
        {
            renderer.render(entity, entity.getYRot(), 1F, new PoseStack(), MorphRenderer.DISCARD, 15728880);
        }
        catch(Throwable ignored) {}
        finally
        {
            MorphBlocks.capturing = null;
        }
        cap.quads.rotateY(-(180F - entity.yBodyRot));
        boolean right = side == HumanoidArm.RIGHT;
        MorphQuads arm = cap.quads.pickArm(right);
        if(arm == null)
        {
            return null;
        }
        arm.place(right, right ? -6F / 16F : 6F / 16F, 0F, 0F);
        return new Limb(null, null, arm);
    }

    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("morph", "textures/skin/morphskin.png");

    private static void overlay(java.util.List<MorphBlocks.Block> blocks, PoseStack ps, MultiBufferSource buffers, int light, float alpha)
    {
        if(alpha > 0.003F && !blocks.isEmpty())
        {
            MorphBlocks.emit(blocks, ps.last(), buffers.getBuffer(RenderType.entityTranslucent(SKIN)), light, alpha);
        }
    }

    /** The cubes of the arm a mob (or the player, for null) would hold out, in arm space. */
    private static MorphBlocks blocksOf(PlayerRenderer vanilla, AbstractClientPlayer player, LivingEntity entity, HumanoidArm side)
    {
        MorphBlocks mb = new MorphBlocks();
        Limb limb = limbOf(vanilla, entity, side);
        if(limb != null)
        {
            if(limb.part() != null)
            {
                mb.addTree(limb.part(), new PoseStack());
            }
            else
            {
                limb.quads().toBlocks(mb);
            }
        }
        return mb;
    }

    /** @param entity null = the player's own arm */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Limb limbOf(PlayerRenderer vanilla, LivingEntity entity, HumanoidArm side)
    {
        if(entity == null)
        {
            var pm = vanilla.getModel();
            ModelPart a = side == HumanoidArm.RIGHT ? pm.rightArm : pm.leftArm;
            a.xRot = 0F;
            return new Limb(a, null, null);
        }
        if(BROKEN.contains(entity.getType().toString()))
        {
            return null;
        }
        try
        {
            EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if(MorphBlocks.modded(entity))
            {
                // the mod's own model files know where the arm is
                MorphGeo.GeoArm geo = MorphGeo.find(entity, renderer, side == HumanoidArm.RIGHT);
                if(geo != null)
                {
                    return new Limb(geo.part(), geo.texture(), null);
                }
            }
            if(!(renderer instanceof LivingEntityRenderer living))
            {
                return quadLimb(entity, renderer, side);
            }
            EntityModel model = living.getModel();
            ModelPart arm = null;
            boolean placeAsArm = false;
            if(model instanceof HumanoidModel<?> hm)
            {
                hm.attackTime = 0F;
                hm.crouching = false;
                hm.swimAmount = 0F;
                model.setupAnim(entity, 0F, 0F, 0F, 0F, 0F);
                arm = side == HumanoidArm.RIGHT ? hm.rightArm : hm.leftArm;
            }
            else
            {
                model.setupAnim(entity, 0F, 0F, 0F, 0F, 0F);
                java.util.List<ModelPart> roots = new java.util.ArrayList<>();
                if(model instanceof HierarchicalModel<?> hier)
                {
                    roots.add(hier.root());
                    arm = find(hier.root(), side == HumanoidArm.RIGHT ? "right_arm" : "left_arm");
                }
                if(arm == null)
                {
                    if(model instanceof net.minecraft.client.model.ListModel<?> list)
                    {
                        list.parts().forEach(roots::add);
                    }
                    for(Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass())
                    {
                        for(java.lang.reflect.Field f : c.getDeclaredFields())
                        {
                            if(f.getType() == ModelPart.class)
                            {
                                f.setAccessible(true);
                                roots.add((ModelPart)f.get(model));
                            }
                        }
                    }
                    arm = MorphLimbs.pick(roots, side == HumanoidArm.RIGHT);
                }
                if(arm != null)
                {
                    // sit it where a player's arm would be so the first person camera transform lines up
                    placeAsArm = true;
                }
            }
            if(arm == null)
            {
                return quadLimb(entity, renderer, side); // no model part to borrow
            }
            arm = copy(arm);
            if(placeAsArm)
            {
                arm.setPos(side == HumanoidArm.RIGHT ? -5F : 5F, 2F, 0F);
            }
            arm.xRot = 0F;
            arm.yRot = 0F;
            arm.zRot = 0F;
            return new Limb(arm, living.getTextureLocation(entity), null);
        }
        catch(Throwable t)
        {
            BROKEN.add(entity.getType().toString());
            com.iiampanda.morph.common.Morph.LOGGER.error("Could not draw the first person arm of {}", entity.getType(), t);
            return null;
        }
    }
}
