package com.iiampanda.morph.mixin;

import com.iiampanda.morph.api.mob.trait.SwallowBlocksTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntityMixin
{
    /** A block a piston pushes into a morphed Sulfur Cube goes inside it instead of shoving it out of the way. */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void morph$swallow(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity entity, CallbackInfo ci)
    {
        if(level.isClientSide() || entity.isSourcePiston() || entity.getProgress(0F) < 0.5F)
        {
            return;
        }
        ItemStack stack = new ItemStack(entity.getMovedState().getBlock());
        if(!SwallowBlocksTrait.swallowable(stack))
        {
            return;
        }
        Vec3 centre = Vec3.atCenterOf(pos);
        for(ServerPlayer sp : ((net.minecraft.server.level.ServerLevel)level).players())
        {
            SwallowBlocksTrait trait = SwallowBlocksTrait.of(sp);
            if(trait != null && sp.getBoundingBox().contains(centre) && trait.swallow(sp, stack))
            {
                level.removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                ci.cancel();
                return;
            }
        }
    }
}
