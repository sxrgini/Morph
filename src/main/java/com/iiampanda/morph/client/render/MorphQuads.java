package com.iiampanda.morph.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The raw quads a renderer sent to the vertex buffer. Used for mobs whose models are not made of vanilla ModelParts
 * (GeckoLib and other custom renderers): the quads are turned into blocks for the morph animation, and the ones that
 * make up an arm are replayed in first person.
 */
public final class MorphQuads
{
    public static final class Quad
    {
        public Object type;
        public final float[] p = new float[12];
        public final float[] uv = new float[8];
        public final int[] col = new int[4];
    }

    public final List<Quad> quads = new ArrayList<>();

    /** Records everything written to it as quads (4 vertices each). */
    public final class Rec implements VertexConsumer
    {
        private final Object type;
        private int n;
        private Quad q;

        public Rec(Object type)
        {
            this.type = type;
        }

        public VertexConsumer addVertex(float x, float y, float z)
        {
            if(q == null || n == 4)
            {
                q = new Quad();
                q.type = type;
                n = 0;
                quads.add(q);
            }
            q.p[n * 3] = x;
            q.p[n * 3 + 1] = y;
            q.p[n * 3 + 2] = z;
            q.col[n] = -1;
            n++;
            return this;
        }

        public VertexConsumer setColor(int r, int g, int b, int a)
        {
            if(q != null && n > 0)
            {
                q.col[n - 1] = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
            }
            return this;
        }

        public VertexConsumer setColor(int argb)
        {
            if(q != null && n > 0)
            {
                q.col[n - 1] = argb;
            }
            return this;
        }

        public VertexConsumer setUv(float u, float v)
        {
            if(q != null && n > 0)
            {
                q.uv[(n - 1) * 2] = u;
                q.uv[(n - 1) * 2 + 1] = v;
            }
            return this;
        }

        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { return this; }
        public VertexConsumer setUv3(float u, float v) { return this; }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
        public VertexConsumer setLineWidth(float w) { return this; }
    }

    /** Turns every vertex around the Y axis (what a renderer does to face the mob the right way, undone). */
    public void rotateY(float degrees)
    {
        double r = Math.toRadians(degrees);
        float c = (float)Math.cos(r), sn = (float)Math.sin(r);
        for(Quad q : quads)
        {
            for(int i = 0; i < 4; i++)
            {
                float x = q.p[i * 3], z = q.p[i * 3 + 2];
                q.p[i * 3] = c * x + sn * z;
                q.p[i * 3 + 2] = -sn * x + c * z;
            }
        }
    }

    /** Swings the arm-like quads on one side forward about the shoulder (mob frame: front is -z, y up). */
    public void swingArm(boolean right, float radians)
    {
        MorphQuads arm = pickArm(right);
        if(arm == null)
        {
            return;
        }
        float[] b = arm.bounds();
        float py = b[4], pz = (b[2] + b[5]) / 2F;
        float c = (float)Math.cos(radians), sn = (float)Math.sin(radians);
        for(Quad q : arm.quads)
        {
            for(int i = 0; i < 4; i++)
            {
                float y = q.p[i * 3 + 1] - py, z = q.p[i * 3 + 2] - pz;
                q.p[i * 3 + 1] = py + y * c - z * sn;
                q.p[i * 3 + 2] = pz + y * sn + z * c;
            }
        }
    }

    /** Render types that are not the mob's body (names, outlines, glint...) */
    public static boolean ignored(Object type)
    {
        String s = String.valueOf(type).toLowerCase(java.util.Locale.ROOT);
        return s.contains("text") || s.contains("line") || s.contains("glint") || s.contains("outline") || s.contains("leash") || s.contains("shadow");
    }

    private float[] bounds()
    {
        float[] b = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
        for(Quad q : quads)
        {
            for(int i = 0; i < 4; i++)
            {
                for(int k = 0; k < 3; k++)
                {
                    b[k] = Math.min(b[k], q.p[i * 3 + k]);
                    b[3 + k] = Math.max(b[3 + k], q.p[i * 3 + k]);
                }
            }
        }
        return b;
    }

    /** Turns the surface of the recorded quads into a shell of small blocks. */
    public void toBlocks(MorphBlocks out)
    {
        if(quads.isEmpty())
        {
            return;
        }
        float[] b = bounds();
        float maxDim = Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2]));
        float cell = Math.max(0.08F, Math.min(0.4F, maxDim / 10F));
        java.util.Set<Long> seen = new java.util.HashSet<>();
        int stride = Math.max(1, quads.size() / 3000);
        for(int qi = 0; qi < quads.size(); qi += stride)
        {
            Quad q = quads.get(qi);
            float e1x = q.p[3] - q.p[0], e1y = q.p[4] - q.p[1], e1z = q.p[5] - q.p[2];
            float e2x = q.p[9] - q.p[0], e2y = q.p[10] - q.p[1], e2z = q.p[11] - q.p[2];
            int n1 = Math.max(1, Math.min(12, (int)Math.ceil(Math.sqrt(e1x * e1x + e1y * e1y + e1z * e1z) / cell)));
            int n2 = Math.max(1, Math.min(12, (int)Math.ceil(Math.sqrt(e2x * e2x + e2y * e2y + e2z * e2z) / cell)));
            for(int i = 0; i <= n1; i++)
            {
                for(int j = 0; j <= n2; j++)
                {
                    float s = i / (float)n1, t = j / (float)n2;
                    float x = q.p[0] + e1x * s + e2x * t, y = q.p[1] + e1y * s + e2y * t, z = q.p[2] + e1z * s + e2z * t;
                    long cx = Math.floorDiv((long)Math.floor((x - b[0]) / cell), 1L), cy = (long)Math.floor((y - b[1]) / cell), cz = (long)Math.floor((z - b[2]) / cell);
                    if(seen.add((cx & 0x1FFFFF) << 42 | (cy & 0x1FFFFF) << 21 | (cz & 0x1FFFFF)))
                    {
                        MorphBlocks.Block blk = new MorphBlocks.Block();
                        blk.c.set(b[0] + (cx + 0.5F) * cell, b[1] + (cy + 0.5F) * cell, b[2] + (cz + 0.5F) * cell);
                        float h = cell / 2F + 0.0015F;
                        blk.a[0].set(h, 0F, 0F);
                        blk.a[1].set(0F, h, 0F);
                        blk.a[2].set(0F, 0F, h);
                        out.blocks.add(blk);
                    }
                }
            }
        }
    }

    /**
     * Picks the quads that look like the arm on one side and re-centres them as a limb hanging from the origin,
     * in 1/16 block "model" units of a vanilla arm (y down, 12 tall at most).
     * @param right the player's right arm (the mob's +x side in its own frame)
     * @return null if nothing arm-like was found
     */
    public MorphQuads pickArm(boolean right)
    {
        if(quads.isEmpty())
        {
            return null;
        }
        float[] b = bounds();
        float height = b[4] - b[1];
        float wmax = right ? b[3] : -b[0];
        MorphQuads out = null;
        if(height > 0F && wmax > 0.05F)
        {
        for(float th : new float[]{ 0.55F, 0.3F })
        {
            out = new MorphQuads();
            for(Quad q : quads)
            {
                float cx = 0F, cy = 0F;
                for(int i = 0; i < 4; i++)
                {
                    cx += q.p[i * 3] / 4F;
                    cy += q.p[i * 3 + 1] / 4F;
                }
                float side = right ? cx : -cx;
                float fy = (cy - b[1]) / height;
                if(side > th * wmax && fy > 0.25F && fy < 0.92F)
                {
                    out.quads.add(q);
                }
            }
            if(out.quads.size() >= 4)
            {
                break;
            }
            out = null;
        }
        }
        if(out == null)
        {
            // nothing sticks out on that side (the model is all down the middle): use the lower part, or the whole mob, so there is always a hand
            out = new MorphQuads();
            for(Quad q : quads)
            {
                float cy = 0F;
                for(int i = 0; i < 4; i++)
                {
                    cy += q.p[i * 3 + 1] / 4F;
                }
                if(height <= 0F || (cy - b[1]) / height < 0.7F)
                {
                    out.quads.add(q);
                }
            }
            if(out.quads.size() < 4)
            {
                out = new MorphQuads();
                out.quads.addAll(quads);
            }
        }
        return out;
    }

    /** Vertex positions of this limb moved so its top inner corner is at the origin, scaled to fit an arm, y down (model space). */
    public void place(boolean right, float x0, float y0, float z0)
    {
        float[] b = bounds();
        float h = b[4] - b[1];
        float w = Math.max(b[3] - b[0], b[5] - b[2]);
        float s = Math.min(1F, Math.min(0.8F / Math.max(h, 0.01F), 0.4F / Math.max(w, 0.01F)));
        float cx = (b[0] + b[3]) / 2F, cz = (b[2] + b[5]) / 2F, top = b[4];
        for(Quad q : quads)
        {
            for(int i = 0; i < 4; i++)
            {
                float x = -(q.p[i * 3] - cx) * s, y = (top - q.p[i * 3 + 1]) * s, z = (q.p[i * 3 + 2] - cz) * s;
                q.p[i * 3] = x0 + x;
                q.p[i * 3 + 1] = y0 + y;
                q.p[i * 3 + 2] = z0 + z;
            }
        }
    }

    /** Draws a quad into a consumer (position in the space the pose is in). */
    public static void emitQuad(Quad q, com.mojang.blaze3d.vertex.PoseStack.Pose pose, VertexConsumer vc, int light)
    {
        Vector3f a = new Vector3f(q.p[3] - q.p[0], q.p[4] - q.p[1], q.p[5] - q.p[2]);
        Vector3f c = new Vector3f(q.p[9] - q.p[0], q.p[10] - q.p[1], q.p[11] - q.p[2]);
        Vector3f nrm = a.cross(c).normalize();
        for(int i = 0; i < 4; i++)
        {
            vc.addVertex(pose, q.p[i * 3], q.p[i * 3 + 1], q.p[i * 3 + 2])
                .setColor(q.col[i])
                .setUv(q.uv[i * 2], q.uv[i * 2 + 1])
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nrm.x, nrm.y, nrm.z);
        }
    }
}
