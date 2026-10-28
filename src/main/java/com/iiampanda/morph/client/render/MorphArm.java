package com.iiampanda.morph.client.render;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
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

    private record Limb(ModelPart part, Identifier texture, MorphQuads quads) {}

    /** Work to do once vanilla has submitted the player's own arm (the static skin goes over it). */
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
    public static boolean render(LivingEntityRenderer<?, ?, ?> vanilla, PoseStack ps, SubmitNodeCollector collector, int light, HumanoidArm side)
    {
        after = null;
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null)
        {
            return false;
        }
        MorphInfo info = MorphApi.getMorphInfo(mc.player);
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
                draw(limb, ps, collector, light);
            }
            return true;
        }

        // same phases as the body: freeze to the static skin, blocks slide into the new arm, thaw
        float t = info.getProgress(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        LivingEntity oldE = info.prevVariant == null ? null : info.getPrevEntity();
        LivingEntity newE = info.isMorphed() ? info.getEntity() : null;
        float middle = Math.max(0F, Math.min(1F, (t - 0.125F) / 0.75F));
        if(middle > 0F && middle < 1F)
        {
            overlay(MorphBlocks.blend(blocksOf(vanilla, oldE, side), blocksOf(vanilla, newE, side), MorphBlocks.sine(middle)), ps, collector, light, 1F);
            return true;
        }
        LivingEntity e = middle <= 0F ? oldE : newE;
        float alpha = middle <= 0F ? MorphBlocks.sine(t / 0.125F) : 1F - MorphBlocks.sine((t - 0.875F) / 0.125F);
        java.util.List<MorphBlocks.Block> blocks = blocksOf(vanilla, e, side).blocks;
        if(e == null)
        {
            after = () -> overlay(blocks, ps, collector, light, alpha);
            return false; // the player's own arm: vanilla draws it
        }
        Limb limb = limbOf(vanilla, e, side);
        if(limb != null)
        {
            draw(limb, ps, collector, light);
        }
        overlay(blocks, ps, collector, light, alpha);
        return true;
    }

    private static void draw(Limb limb, PoseStack ps, SubmitNodeCollector collector, int light)
    {
        if(limb.part() != null)
        {
            collector.submitModelPart(limb.part(), ps, RenderTypes.entityCutout(limb.texture()), light, OverlayTexture.NO_OVERLAY, null);
            return;
        }
        for(MorphQuads.Quad q : limb.quads().quads)
        {
            collector.submitCustomGeometry(ps, (net.minecraft.client.renderer.rendertype.RenderType)q.type, (pose, vc) -> MorphQuads.emitQuad(q, pose, vc, light));
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
        try
        {
            EntityRenderState state = renderer.createRenderState(entity, 1F);
            state.shadowPieces.clear();
            renderer.submit(state, new PoseStack(), MorphBlocks.collector(cap), new net.minecraft.client.renderer.state.level.CameraRenderState());
        }
        catch(Throwable ignored) {}
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

    private static final Identifier SKIN = Identifier.fromNamespaceAndPath("morph", "textures/skin/morphskin.png");

    private static void overlay(java.util.List<MorphBlocks.Block> blocks, PoseStack ps, SubmitNodeCollector collector, int light, float alpha)
    {
        if(alpha > 0.003F && !blocks.isEmpty())
        {
            collector.submitCustomGeometry(ps, RenderTypes.entityTranslucent(SKIN), (pose, vc) -> MorphBlocks.emit(blocks, pose, vc, light, alpha));
        }
    }

    /** The cubes of the arm a mob (or the player, for null) would hold out, in arm space. */
    private static MorphBlocks blocksOf(LivingEntityRenderer<?, ?, ?> vanilla, LivingEntity entity, HumanoidArm side)
    {
        MorphBlocks mb = new MorphBlocks();
        Limb limb = limbOf(vanilla, entity, side);
        if(limb != null)
        {
            if(limb.part() != null)
            {
                mb.addPart(limb.part(), new PoseStack());
            }
            else
            {
                limb.quads().toBlocks(mb);
            }
        }
        return mb;
    }


    /** The hand of a mob whose model is made of boxes (see MorphBoxes), or null. */
    private static Limb boxLimb(LivingEntity entity, Object model, Object type, HumanoidArm side)
    {
        boolean right = side == HumanoidArm.RIGHT;
        Object box = null;
        HandInfos.Info info = HandInfos.lookup(entity.getType().builtInRegistryHolder().key().identifier().toString(), model.getClass());
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


    /** A hand info for this mob (see HandInfos), built into one part, or null when there is none (or its parts are not in the model). */
    private static ModelPart overrideArm(LivingEntity entity, Object model, java.util.Collection<ModelPart> roots, HumanoidArm side)
    {
        boolean right = side == HumanoidArm.RIGHT;
        HandInfos.Info info = HandInfos.lookup(entity.getType().builtInRegistryHolder().key().identifier().toString(), model.getClass());
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

    /** Parts that are far too big, too small or too far off to pass for an arm are scaled and moved so their top sits at the shoulder. Ordinary limbs are left alone. */
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
        if(!(h < 4F || h > 18F || w > 9F || Math.abs(cx) > 6F || Math.abs(cz) > 6F || b[1] < -6F || b[1] > 6F))
        {
            return;
        }
        float s = 1F;
        if(h > 16F || w > 8F)
        {
            s = Math.min(16F / Math.max(h, 0.01F), 8F / Math.max(w, 0.01F));
        }
        else if(Math.max(h, w) < 4F)
        {
            s = Math.min(2.5F, 4F / Math.max(Math.max(h, w), 0.01F));
        }
        arm.xScale = s;
        arm.yScale = s;
        arm.zScale = s;
        arm.x += -cx * s;
        arm.z += -cz * s;
        arm.y += -2F - b[1] * s;
    }

    /**
     * Last check on every borrowed arm: its box (with its own position and scale) has to sit where a player's arm does, at the shoulder on
     * its own side and not much bigger. Whatever put it elsewhere (an odd pivot, a parent's offset, a very large mob) is undone by moving
     * and shrinking it. Ordinary arms pass untouched.
     */
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
            boolean sane = sideOk && Math.abs(cz) <= 5F && minY >= -5F && minY <= 6F && h <= 20F && w <= 11F;
            if(sane)
            {
                return arm;
            }
            float s = 1F;
            if(h > 16F || w > 8F)
            {
                s = Math.min(16F / Math.max(h, 0.01F), 8F / Math.max(w, 0.01F));
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

    /** Older models keep their parts in java fields rather than a named tree. */
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

    /** @param entity null = the player's own arm */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Limb limbOf(LivingEntityRenderer<?, ?, ?> vanilla, LivingEntity entity, HumanoidArm side)
    {
        if(entity == null)
        {
            if(!(vanilla.getModel() instanceof net.minecraft.client.model.HumanoidModel<?> pm))
            {
                return null;
            }
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
            EntityRenderState state = renderer.createRenderState(entity, 1F);
            if(state instanceof ArmedEntityRenderState armed)
            {
                armed.swingAnimation = 0F;
            }
            if(state instanceof HumanoidRenderState hs)
            {
                hs.isCrouching = false;
                hs.swimAmount = 0F;
            }
            EntityModel model = living.getModel();
            Object boxModel = MorphBoxes.unwrap(model);
            if(boxModel != null)
            {
                // Citadel style box models (Alex's Mobs, Cataclysm...): the hand info or the best arm-like box, read by reflection
                Limb boxed = boxLimb(entity, boxModel, RenderTypes.entityCutout(living.getTextureLocation((LivingEntityRenderState)state)), side);
                if(boxed != null)
                {
                    return boxed;
                }
            }
            ModelPart arm;
            boolean placeAsArm = false;
            model.resetPose();
            model.setupAnim(state);
            if(model instanceof net.minecraft.client.model.HumanoidModel<?> hm)
            {
                arm = side == HumanoidArm.RIGHT ? hm.rightArm : hm.leftArm;
                placeAsArm = true; // modded humanoids (Minoshroom, giants...) keep their arms at odd pivots
            }
            else
            {
                arm = find(model.root(), side == HumanoidArm.RIGHT ? "right_arm" : "left_arm");
                if(arm == null)
                {
                    // no arms: borrow a front leg, wing or fin like the original mod
                    arm = MorphLimbs.pick(java.util.List.of(model.root()), side == HumanoidArm.RIGHT);
                }
                if(arm != null)
                {
                    // sit it where a player's arm would be so the first person camera transform lines up
                    placeAsArm = true;
                }
            }
            ModelPart custom = overrideArm(entity, model, java.util.List.of(model.root()), side);
            if(custom != null)
            {
                return new Limb(settle(custom, side, entity.getType()), living.getTextureLocation((LivingEntityRenderState)state), null);
            }
            if(arm == null)
            {
                return quadLimb(entity, renderer, side); // no model part to borrow
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
            return new Limb(settle(arm, side, entity.getType()), living.getTextureLocation((LivingEntityRenderState)state), null);
        }
        catch(Throwable t)
        {
            BROKEN.add(entity.getType().toString());
            com.iiampanda.morph.common.Morph.LOGGER.error("Could not draw the first person arm of {}", entity.getType(), t);
            return null;
        }
    }
}
