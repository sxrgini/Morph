package com.iiampanda.morph.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MorphBlocks
{

    public static final class Block
    {
        public final Vector3f c = new Vector3f();
        public final Vector3f[] a = { new Vector3f(), new Vector3f(), new Vector3f() };
    }

    public final List<Block> blocks = new ArrayList<>();

    public final MorphQuads quads = new MorphQuads();
    public boolean recordQuads;

    public VertexConsumer rec(Object type)
    {
        return recordQuads && !MorphQuads.ignored(type) ? quads.new Rec(type) : null;
    }

    public static boolean modded(net.minecraft.world.entity.Entity e)
    {
        return !"minecraft".equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace());
    }

    public void finishCapture(net.minecraft.world.entity.Entity e)
    {
        if(recordQuads)
        {
            quads.toBlocks(this);
        }
        if(blocks.isEmpty() && e != null)
        {
            Block b = new Block();
            b.c.set(0F, e.getBbHeight() / 2F, 0F);
            b.a[0].set(e.getBbWidth() / 2F, 0F, 0F);
            b.a[1].set(0F, e.getBbHeight() / 2F, 0F);
            b.a[2].set(0F, 0F, e.getBbWidth() / 2F);
            blocks.add(b);
        }
    }

    public void add(Matrix4f m, float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
        Block b = new Block();
        float hx = (maxX - minX) / 32F + 0.0015F;
        float hy = (maxY - minY) / 32F + 0.0015F;
        float hz = (maxZ - minZ) / 32F + 0.0015F;
        m.transformPosition((minX + maxX) / 32F, (minY + maxY) / 32F, (minZ + maxZ) / 32F, b.c);
        m.transformDirection(hx, 0F, 0F, b.a[0]);
        m.transformDirection(0F, hy, 0F, b.a[1]);
        m.transformDirection(0F, 0F, hz, b.a[2]);
        blocks.add(b);
    }

    public void addPart(net.minecraft.client.model.geom.ModelPart part, PoseStack ps)
    {
        if(!part.visible)
        {
            return;
        }
        ps.pushPose();
        part.translateAndRotate(ps);
        if(!part.skipDraw)
        {
            for(net.minecraft.client.model.geom.ModelPart.Cube cube : ((com.iiampanda.morph.client.mixin.ModelPartAccessor)(Object)part).morph$cubes())
            {
                add(ps.last().pose(), cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ);
            }
        }
        for(net.minecraft.client.model.geom.ModelPart child : ((com.iiampanda.morph.client.mixin.ModelPartAccessor)(Object)part).morph$children().values())
        {
            addPart(child, ps);
        }
        ps.popPose();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addModel(net.minecraft.client.model.Model model, Object state, PoseStack ps)
    {
        try
        {
            model.setupAnim(state);
        }
        catch(Throwable ignored) {}
        addPart(model.root(), ps);
    }

    public static net.minecraft.client.renderer.SubmitNodeCollector collector(MorphBlocks sink)
    {
        java.lang.reflect.InvocationHandler handler = (proxy, method, args) ->
        {
            String n = method.getName();
            if(method.getDeclaringClass() == Object.class)
            {
                return switch(n)
                {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> "MorphBlocks.collector";
                };
            }
            if(args != null)
            {
                if(n.equals("submitCustomGeometry") && sink.recordQuads && args.length >= 3 && args[0] instanceof PoseStack cps && args[2] instanceof net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer cg)
                {
                    VertexConsumer rec = sink.rec(args[1]);
                    if(rec != null)
                    {
                        cg.render(cps.last(), rec);
                    }
                    return null;
                }
                if(n.equals("submitModel") && args.length >= 3 && args[0] instanceof net.minecraft.client.model.Model<?> m && args[2] instanceof PoseStack ps)
                {
                    sink.addModel(m, args[1], ps);
                    return null;
                }
                if(n.equals("submitModelPart") && args.length >= 2 && args[0] instanceof net.minecraft.client.model.geom.ModelPart part && args[1] instanceof PoseStack ps)
                {
                    sink.addPart(part, ps);
                    return null;
                }
            }
            Class<?> r = method.getReturnType();
            if(r == void.class)
            {
                return null;
            }
            if(r.isPrimitive())
            {
                return java.lang.reflect.Array.get(java.lang.reflect.Array.newInstance(r, 1), 0);
            }
            return r.isInstance(proxy) ? proxy : null;
        };
        return (net.minecraft.client.renderer.SubmitNodeCollector)java.lang.reflect.Proxy.newProxyInstance(
            net.minecraft.client.renderer.SubmitNodeCollector.class.getClassLoader(), new Class<?>[] { net.minecraft.client.renderer.SubmitNodeCollector.class }, handler);
    }

    public static float sine(float p)
    {
        p = Math.max(0F, Math.min(1F, p));
        return (float)Math.sin(Math.toRadians(-90F + 180F * p)) / 2F + 0.5F;
    }

    private static List<Block> sorted(List<Block> in)
    {
        List<Block> out = new ArrayList<>(in);
        out.sort(Comparator.<Block>comparingInt(b -> -Math.round(b.c.y * 6F)).thenComparingInt(b -> Math.round(b.c.x * 6F)).thenComparingInt(b -> Math.round(b.c.z * 6F)));
        return out;
    }

    private static Block copy(Block b)
    {
        Block n = new Block();
        n.c.set(b.c);
        for(int i = 0; i < 3; i++)
        {
            n.a[i].set(b.a[i]);
        }
        return n;
    }

    private static Vector3f centroid(List<Block> l)
    {
        Vector3f s = new Vector3f();
        for(Block b : l)
        {
            s.add(b.c);
        }
        return l.isEmpty() ? s : s.div(l.size());
    }

    private static final int[][] PERMS = { {0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0} };

    public static List<Block> blend(MorphBlocks from, MorphBlocks to, float t)
    {
        List<Block> a = sorted(from.blocks);
        List<Block> b = sorted(to.blocks);
        List<Block> out = new ArrayList<>();
        if(a.isEmpty() && b.isEmpty())
        {
            return out;
        }
        if(a.isEmpty())
        {
            Block seed = new Block();
            seed.c.set(centroid(b));
            a = List.of(seed);
        }
        if(b.isEmpty())
        {
            Block seed = new Block();
            seed.c.set(centroid(a));
            b = List.of(seed);
        }
        int n = Math.max(a.size(), b.size());
        for(int i = 0; i < n; i++)
        {
            Block x = a.get((int)((long)i * a.size() / n));
            Block y = b.get((int)((long)i * b.size() / n));

            int[] best = PERMS[0];
            float bestScore = -1F;
            for(int[] p : PERMS)
            {
                float score = 0F;
                for(int k = 0; k < 3; k++)
                {
                    score += Math.abs(dirDot(x.a[k], y.a[p[k]]));
                }
                if(score > bestScore)
                {
                    bestScore = score;
                    best = p;
                }
            }
            Block r = new Block();
            x.c.lerp(y.c, t, r.c);
            for(int k = 0; k < 3; k++)
            {
                Vector3f ya = new Vector3f(y.a[best[k]]);
                if(dirDot(x.a[k], ya) < 0F)
                {
                    ya.negate();
                }
                x.a[k].lerp(ya, t, r.a[k]);
            }
            out.add(r);
        }
        return out;
    }

    private static float dirDot(Vector3f p, Vector3f q)
    {
        float lp = p.length();
        float lq = q.length();
        return lp < 1e-5F || lq < 1e-5F ? 0F : p.dot(q) / (lp * lq);
    }

    public static void emit(List<Block> blocks, PoseStack.Pose pose, VertexConsumer vc, int light, float alpha)
    {
        int color = ((int)(Math.max(0F, Math.min(1F, alpha)) * 255F) << 24) | 0xFFFFFF;
        Vector3f p = new Vector3f();
        Vector3f nrm = new Vector3f();
        for(Block b : blocks)
        {
            for(int k = 0; k < 3; k++)
            {
                Vector3f ax = b.a[k];
                float len = ax.length();
                if(len < 1e-4F)
                {
                    continue;
                }
                Vector3f t1 = b.a[(k + 1) % 3];
                Vector3f t2 = b.a[(k + 2) % 3];
                float u1 = Math.min(1F, t1.length() * 32F / 64F);
                float v1 = Math.min(1F, t2.length() * 32F / 64F);
                for(int s = -1; s <= 1; s += 2)
                {
                    nrm.set(ax).mul(s / len);
                    for(int corner = 0; corner < 4; corner++)
                    {
                        float c1 = (corner == 0 || corner == 3) ? -1F : 1F;
                        float c2 = corner < 2 ? -1F : 1F;
                        p.set(b.c).fma(s, ax).fma(c1, t1).fma(c2, t2);
                        vc.addVertex(pose, p.x, p.y, p.z)
                            .setColor(color)
                            .setUv(c1 < 0 ? 0F : u1, c2 < 0 ? 0F : v1)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(pose, nrm.x, nrm.y, nrm.z);
                    }
                }
            }
        }
    }
}
