package levaltru.dropping_leaves.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import levaltru.dropping_leaves.DroppingLeaves;
import levaltru.dropping_leaves.interfaces.IsDroppingLeaves;
import net.minecraft.block.*;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends Block implements Waterloggable {
    @Shadow
    public abstract void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random);

    @Shadow
    protected abstract boolean shouldDecay(BlockState state);

    public LeavesBlockMixin(Settings settings) {
        super(settings);
    }

    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeavesBlock;shouldDecay(Lnet/minecraft/block/BlockState;)Z"))
    private boolean droppingLeaves$leavesFall$randomTick(LeavesBlock instance, BlockState state, Operation<Boolean> original, @Local(ordinal = 0, argsOnly = true) BlockPos pos, @Local(ordinal = 0, argsOnly = true) ServerWorld world) {
        boolean decay = original.call(instance, state);
        if (!DroppingLeaves.shouldLeavesFall(world)) return decay;
        if (decay) return !fallIfPossible(state, pos, world);    // if block falls, no logic further needed
        return false;
    }

    @Inject(method = "scheduledTick", at = @At("TAIL"))
    private void droppingLeaves$leavesFall$scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!DroppingLeaves.shouldLeavesFall(world)) return;
        if (shouldDecay(state)) {
            if (random.nextFloat() < 0.33f) fallIfPossible(state, pos, world);       // v v v
//            else world.createAndScheduleBlockTick(pos, asBlock(), 1);         // make leaves fall not at once, so it'd look prettier.
            else world.scheduleBlockTick(pos, asBlock(), 1);
        }
    }

    @Unique
    private static boolean fallIfPossible(BlockState state, BlockPos pos, ServerWorld world) {
        if (FallingBlock.canFallThrough(world.getBlockState(pos.down())) && pos.getY() >= world.getBottomY()) {
            FallingBlockEntity fallingBlockEntity = FallingBlockEntity.spawnFromBlock(world, pos, state);
            ((IsDroppingLeaves) fallingBlockEntity).setDroppingLeaves(true);
            return true;
        }
        return false;
    }
}
