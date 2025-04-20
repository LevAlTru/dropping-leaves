package levaltru.dropping_leaves.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import levaltru.dropping_leaves.interfaces.IsDroppingLeaves;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(FallingBlockEntity.class)
public abstract class NastyFallingBlockMixin extends Entity implements IsDroppingLeaves {
    public NastyFallingBlockMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Unique
    private static final float MIN = 15f;
    @Unique
    private static final float MAX = 30f;

    @Unique
    private boolean isDroppingLeaves = false;

    @Shadow
    public abstract BlockState getBlockState();

    @Shadow
    public int timeFalling;

    @Override
    public ItemEntity dropItem(ServerWorld serverWorld, ItemConvertible item) {
        if (isDroppingLeaves) {
            List<ItemStack> droppedStacks = Block.getDroppedStacks(getBlockState(), serverWorld, getBlockPos(), null);
            if (!droppedStacks.isEmpty()) {
                for (int i = 0; i < droppedStacks.size() - 1; i++) super.dropItem(serverWorld, droppedStacks.get(i).getItem());
                return super.dropItem(serverWorld, droppedStacks.get(droppedStacks.size() - 1).getItem());
            }
            return null;
        }
        return super.dropItem(serverWorld, item);
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/FallingBlockEntity;discard()V"
            ),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;hasBlockEntity()Z")
            )
    )
    private void droppingLeaves$placeSoundsForLandingLeaves(CallbackInfo ci, @Local(ordinal = 0) BlockPos blockPos) {
        if (isDroppingLeaves && timeFalling > 3f) {
            float v = ((MathHelper.clamp(timeFalling, MIN, MAX) - MIN) / (MAX - MIN)) * 0.8f;
            if (v > random.nextFloat()) getWorld().breakBlock(blockPos, true);
        }
    }

    @Override
    public void setDroppingLeaves(boolean droppingLeaves) {
        isDroppingLeaves = droppingLeaves;
    }

    @Override
    public boolean isDroppingLeaves() {
        return isDroppingLeaves;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void droppingLeaves$addVariableToNbtReading(NbtCompound nbt, CallbackInfo ci) {
        isDroppingLeaves = nbt.getBoolean("droppingLeaves_isDroppingLeaves");
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void droppingLeaves$addVariableToNbtWriting(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("droppingLeaves_isDroppingLeaves", isDroppingLeaves);
    }
}
