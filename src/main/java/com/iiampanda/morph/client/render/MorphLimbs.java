package com.iiampanda.morph.client.render;

import com.iiampanda.morph.client.mixin.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class MorphLimbs
{
    private MorphLimbs() {}

    public static ModelPart pick(Collection<ModelPart> roots, boolean right)
    {
        Map<ModelPart, Boolean> seen = new IdentityHashMap<>();
        List<ModelPart> all = new ArrayList<>();
        for(ModelPart root : roots)
        {
            if(root != null)
            {
                root.getAllParts().forEach(p -> {
                    if(seen.put(p, Boolean.TRUE) == null)
                    {
                        all.add(p);
                    }
                });
            }
        }
        ModelPart limb = null;
        float limbScore = Float.MAX_VALUE;
        ModelPart any = null;
        float anyScore = Float.MAX_VALUE;
        for(ModelPart p : all)
        {
            var cubes = ((ModelPartAccessor)(Object)p).morph$cubes();
            if(cubes.isEmpty())
            {
                continue;
            }
            boolean onSide = right ? p.x < -0.5F : p.x > 0.5F;
            if(!onSide)
            {
                continue;
            }
            float h = 0F;
            float w = 0F;
            for(var c : cubes)
            {
                h = Math.max(h, c.maxY - c.minY);
                w = Math.max(w, Math.max(c.maxX - c.minX, c.maxZ - c.minZ));
            }

            float score = p.z * 4F - p.y * 0.01F;
            if(h >= w && h <= 16F && score < limbScore)
            {
                limb = p;
                limbScore = score;
            }
            if(score < anyScore)
            {
                any = p;
                anyScore = score;
            }
        }
        if(limb == null && any == null)
        {

            float best = Float.MAX_VALUE;
            for(ModelPart p : all)
            {
                var cubes = ((ModelPartAccessor)(Object)p).morph$cubes();
                if(cubes.isEmpty())
                {
                    continue;
                }
                float h = 0F;
                float w = 0F;
                for(var c : cubes)
                {
                    h = Math.max(h, c.maxY - c.minY);
                    w = Math.max(w, Math.max(c.maxX - c.minX, c.maxZ - c.minZ));
                }
                float score = p.z * 4F - p.y * 0.01F + Math.abs(p.x) * 0.5F + (h >= w ? 0F : 50F) + (h > 16F ? 100F : 0F);
                if(score < best)
                {
                    best = score;
                    any = p;
                }
            }
        }
        return limb != null ? limb : any;
    }
}
