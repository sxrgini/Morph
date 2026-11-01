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

public final class MorphArm
{
    private static final java.util.Map<String, Integer> BROKEN = new java.util.HashMap<>();

    private MorphArm() {}

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

    private static final java.util.Set<String> HAND_LOGGED = new java.util.HashSet<>();

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
            if(HAND_LOGGED.add(String.valueOf(entity == null ? null : entity.getType()) + side))
            {
                com.iiampanda.morph.common.Morph.LOGGER.info("[MorphHud] hand {} {} -> {}", entity == null ? null : entity.getType(), side, limb == null ? "nothing to draw" : (limb.part() != null ? "model part" : "quads"));
            }
            if(limb != null)
            {
                draw(limb, ps, buffers, light);
            }
            return true;
        }

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
            return false;
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

    private static Limb boxLimb(LivingEntity entity, Object model, Object type, HumanoidArm side)
    {
        boolean right = side == HumanoidArm.RIGHT;
        Object box = null;
        HandInfos.Info info = HandInfos.lookup(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), model.getClass());
        if(info != null)
        {
            for(HandInfos.Entry e : info.side(right))
            {
                box = MorphBoxes.boxField(model, e.fieldName());
                if(box == null)
                {
                    box = MorphBoxes.boxField(model, e.partName());
                }
                if(box != null)
                {
                    break;
                }
            }
        }
        if(box == null)
        {
            box = MorphBoxes.pick(model, right);
        }
        if(box == null)
        {
            return null;
        }
        MorphBoxes.rest(model);
        MorphQuads quads = MorphBoxes.capture(box, type);
        if(quads == null)
        {
            return null;
        }
        quads.place(right, right ? -6F / 16F : 6F / 16F, 0F, 0F);
        return new Limb(null, null, quads);
    }

    private static ModelPart overrideArm(LivingEntity entity, Object model, java.util.Collection<ModelPart> roots, HumanoidArm side)
    {
        boolean right = side == HumanoidArm.RIGHT;
        HandInfos.Info info = HandInfos.lookup(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), model.getClass());
        if(info == null)
        {
            return null;
        }
        java.util.Map<String, ModelPart> kids = new java.util.LinkedHashMap<>();
        for(HandInfos.Entry entry : info.side(right))
        {
            ModelPart found = resolve(model, roots, entry);
            if(found == null)
            {
                continue;
            }
            ModelPart part = copy(found);
            part.setPos(right ? -5F : 5F, 2F, 0F);
            part.xRot = 0F;
            part.yRot = 0F;
            part.zRot = 0F;
            if(entry.autoFit())
            {
                fit(part);
            }
            for(HandInfos.Corrector c : entry.correctors())
            {
                float v = c.rotate() ? (float)Math.toRadians(c.amount()) : c.amount() * 16F;
                switch(c.axis())
                {
                    case 'x' -> { if(c.rotate()) part.xRot += v; else part.x += v; }
                    case 'y' -> { if(c.rotate()) part.yRot += v; else part.y += v; }
                    default -> { if(c.rotate()) part.zRot += v; else part.z += v; }
                }
            }
            kids.put("hand" + kids.size(), part);
        }
        if(kids.isEmpty())
        {
            return null;
        }
        if(kids.size() == 1)
        {
            return kids.values().iterator().next();
        }
        return new ModelPart(java.util.List.of(), kids);
    }

    private static ModelPart resolve(Object model, java.util.Collection<ModelPart> roots, HandInfos.Entry entry)
    {
        ModelPart found = null;
        if(entry.fieldName() != null)
        {
            found = FIELDPART(model, entry.fieldName());
        }
        for(String name : new String[] {entry.partName(), entry.fieldName()})
        {
            if(found != null || name == null)
            {
                continue;
            }
            ModelPart cur = null;
            for(String seg : name.split("/"))
            {
                ModelPart next = null;
                for(ModelPart root : cur != null ? java.util.List.of(cur) : roots)
                {
                    next = find(root, seg);
                    if(next != null)
                    {
                        break;
                    }
                }
                cur = next;
                if(cur == null)
                {
                    break;
                }
            }
            found = cur;
        }
        return found;
    }

    private static float[] extent(ModelPart p, float ox, float oy, float oz)
    {
        com.iiampanda.morph.client.mixin.ModelPartAccessor acc = (com.iiampanda.morph.client.mixin.ModelPartAccessor)(Object)p;
        float[] b = null;
        for(var c : acc.morph$cubes())
        {
            float[] cb = { c.minX + ox, c.minY + oy, c.minZ + oz, c.maxX + ox, c.maxY + oy, c.maxZ + oz };
            b = union(b, cb);
        }
        for(ModelPart child : acc.morph$children().values())
        {
            b = union(b, extent(child, ox + child.x, oy + child.y, oz + child.z));
        }
        return b;
    }

    private static float[] union(float[] a, float[] b)
    {
        if(a == null || b == null)
        {
            return a != null ? a : b;
        }
        return new float[] { Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[3], b[3]), Math.max(a[4], b[4]), Math.max(a[5], b[5]) };
    }

    private static void fit(ModelPart arm)
    {
        float[] b = extent(arm, 0F, 0F, 0F);
        if(b == null)
        {
            return;
        }
        float h = b[4] - b[1];
        float w = Math.max(b[3] - b[0], b[5] - b[2]);
        float cx = (b[0] + b[3]) / 2F;
        float cz = (b[2] + b[5]) / 2F;
        if(!(h < 4F || Math.max(h, w) < 8F || w < 3F || h > 18F || w > 9F || Math.abs(cx) > 6F || Math.abs(cz) > 6F || b[1] < -6F || b[1] > 6F))
        {
            return;
        }
        float s = 1F;
        if(h > 16F || w > 8F)
        {
            s = Math.min(16F / Math.max(h, 0.01F), 8F / Math.max(w, 0.01F));
        }
        else if(Math.max(h, w) < 8F)
        {
            s = Math.min(6F, 8F / Math.max(Math.max(h, w), 0.01F));
        }
        float t = w * s < 3F ? Math.min(4F, 3F / Math.max(w * s, 0.01F)) : 1F;
        arm.xScale = s * t;
        arm.yScale = s;
        arm.zScale = s * t;
        arm.x += -cx * s * t;
        arm.z += -cz * s * t;
        arm.y += -2F - b[1] * s;
    }

    private static final Set<String> LOGGED = new HashSet<>();

    private static ModelPart settle(ModelPart arm, HumanoidArm side, Object what)
    {
        try
        {
            float[] b = extent(arm, 0F, 0F, 0F);
            if(b == null)
            {
                return arm;
            }
            boolean right = side == HumanoidArm.RIGHT;
            float sx = arm.xScale, sy = arm.yScale, sz = arm.zScale;
            float minX = arm.x + b[0] * sx, maxX = arm.x + b[3] * sx;
            float minY = arm.y + b[1] * sy, maxY = arm.y + b[4] * sy;
            float minZ = arm.z + b[2] * sz, maxZ = arm.z + b[5] * sz;
            float cx = (minX + maxX) / 2F, cz = (minZ + maxZ) / 2F;
            float h = maxY - minY, w = Math.max(maxX - minX, maxZ - minZ);
            boolean sideOk = right ? cx < -2F && cx > -13F : cx > 2F && cx < 13F;
            if(LOGGED.add(what + (right ? "#R" : "#L")))
            {
                com.iiampanda.morph.common.Morph.LOGGER.info("First person arm of {}: x {} to {}, y {} to {}, z {} to {}", what, minX, maxX, minY, maxY, minZ, maxZ);
            }
            boolean sane = sideOk && Math.abs(cz) <= 5F && minY >= -5F && minY <= 6F && h <= 20F && w <= 11F && Math.max(h, w) >= 8F;
            if(sane)
            {
                return arm;
            }
            float s = 1F;
            if(h > 16F || w > 8F)
            {
                s = Math.min(16F / Math.max(h, 0.01F), 8F / Math.max(w, 0.01F));
            }
            else if(Math.max(h, w) < 8F)
            {
                s = Math.min(6F, 8F / Math.max(Math.max(h, w), 0.01F));
            }
            arm.xScale = sx * s;
            arm.yScale = sy * s;
            arm.zScale = sz * s;
            float nx = arm.x + b[0] * arm.xScale, nX = arm.x + b[3] * arm.xScale;
            float nz = arm.z + b[2] * arm.zScale, nZ = arm.z + b[5] * arm.zScale;
            float ny = arm.y + b[1] * arm.yScale;
            arm.x += (right ? -6F : 6F) - (nx + nX) / 2F;
            arm.z += -(nz + nZ) / 2F;
            arm.y += 0F - ny;
            com.iiampanda.morph.common.Morph.LOGGER.info("Moved the first person arm of {} back to the shoulder (it was at x {} y {} z {}, {} x {} high)", what, cx, minY, cz, w, h);
        }
        catch(Throwable ignored) {}
        return arm;
    }

    private static ModelPart FIELDPART(Object model, String name)
    {
        for(Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass())
        {
            try
            {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                if(f.getType() == ModelPart.class)
                {
                    f.setAccessible(true);
                    return (ModelPart)f.get(model);
                }
            }
            catch(Throwable ignored) {}
        }
        return null;
    }

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
        if(BROKEN.getOrDefault(entity.getType().toString(), 0) >= 200)
        {
            return null;
        }
        try
        {
            EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if(MorphBlocks.modded(entity))
            {

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
            Object boxModel = MorphBoxes.unwrap(model);
            if(boxModel != null)
            {

                Limb boxed = boxLimb(entity, boxModel, RenderType.entityCutoutNoCull(living.getTextureLocation(entity)), side);
                if(boxed != null)
                {
                    return boxed;
                }
            }
            ModelPart arm = null;
            boolean placeAsArm = false;
            if(model instanceof HumanoidModel<?> hm)
            {
                hm.attackTime = 0F;
                hm.crouching = false;
                hm.swimAmount = 0F;
                model.setupAnim(entity, 0F, 0F, 0F, 0F, 0F);
                arm = side == HumanoidArm.RIGHT ? hm.rightArm : hm.leftArm;
                placeAsArm = true;
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

                    placeAsArm = true;
                }
            }
            java.util.List<ModelPart> handRoots = new java.util.ArrayList<>();
            if(model instanceof HierarchicalModel<?> hier2)
            {
                handRoots.add(hier2.root());
            }
            if(model instanceof net.minecraft.client.model.ListModel<?> list2)
            {
                list2.parts().forEach(handRoots::add);
            }
            ModelPart custom = overrideArm(entity, model, handRoots, side);
            if(custom != null)
            {
                return new Limb(settle(custom, side, entity.getType()), living.getTextureLocation(entity), null);
            }
            if(arm == null)
            {
                return quadLimb(entity, renderer, side);
            }
            arm = copy(arm);
            if(placeAsArm)
            {
                arm.setPos(side == HumanoidArm.RIGHT ? -5F : 5F, 2F, 0F);
                fit(arm);
            }
            arm.xRot = 0F;
            arm.yRot = 0F;
            arm.zRot = 0F;
            return new Limb(settle(arm, side, entity.getType()), living.getTextureLocation(entity), null);
        }
        catch(Throwable t)
        {
            BROKEN.merge(entity.getType().toString(), 1, Integer::sum);
            if(BROKEN.get(entity.getType().toString()) == 1) com.iiampanda.morph.common.Morph.LOGGER.error("Could not draw the first person arm of {}", entity.getType(), t);
            return null;
        }
    }
}
