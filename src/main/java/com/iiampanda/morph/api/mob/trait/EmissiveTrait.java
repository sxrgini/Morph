package com.iiampanda.morph.api.mob.trait;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class EmissiveTrait extends Trait<EmissiveTrait>
{
    public Integer lightLevel;

    private transient BlockPos lightPos;
    private transient Level lightLevelWorld;

    public EmissiveTrait() { type = "traitEmissive"; }

    private BlockState lightState(Level level, BlockPos pos)
    {
        int l = Math.max(1, Math.min(15, lightLevel == null ? 10 : lightLevel));
        return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, l).setValue(LightBlock.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override
    public void tick(float strength)
    {
        if(player.level().isClientSide())
        {
            return;
        }
        if(strength < 1F || player.isSpectator() || player.isInvisible())
        {
            clear();
            return;
        }
        Level level = player.level();
        BlockPos pos = BlockPos.containing(player.getX(), player.getY() + player.getBbHeight() * 0.6D, player.getZ());
        if(pos.equals(lightPos) && level == lightLevelWorld)
        {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        if(existing.isAir() || existing.is(Blocks.LIGHT) || existing.is(Blocks.WATER))
        {
            clear();
            level.setBlock(pos, lightState(level, pos), 18);
            lightPos = pos;
            lightLevelWorld = level;
        }
    }

    private void clear()
    {
        if(lightPos != null && lightLevelWorld != null && lightLevelWorld.getBlockState(lightPos).is(Blocks.LIGHT))
        {
            BlockState s = lightLevelWorld.getBlockState(lightPos);
            lightLevelWorld.setBlock(lightPos, s.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 18);
        }
        lightPos = null;
        lightLevelWorld = null;
    }

    @Override
    public void removeHooks()
    {
        super.removeHooks();
        clear();
    }
}
