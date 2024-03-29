package tgw.evolution.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tgw.evolution.EvolutionClient;
import tgw.evolution.blocks.IClimbable;
import tgw.evolution.hooks.LivingHooks;
import tgw.evolution.patches.PatchLivingEntity;
import tgw.evolution.util.math.MathHelper;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public abstract class MixinEntityClient implements EntityAccess {

    @Shadow public Level level;
    @Shadow public float xRotO;
    @Shadow public float yRotO;
    @Shadow @Nullable private Entity vehicle;
    @Shadow private float xRot;
    @Shadow private float yRot;

    @Override
    @Shadow
    public abstract BlockPos blockPosition();

    @Shadow
    public abstract Pose getPose();

    @Shadow
    @Nullable
    public abstract Entity getVehicle();

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract boolean isCrouching();

    @Shadow
    public abstract boolean isInWater();

    @Shadow
    public abstract boolean isOnGround();

    @Shadow
    public abstract boolean isPassenger();

    @Shadow
    public abstract void setXRot(float pXRot);

    @Shadow
    public abstract void setYRot(float pYRot);

    /**
     * @author TheGreatWolf
     * @reason Lock or adjust camera in certain situations
     */
    @SuppressWarnings("ConstantConditions")
    @Overwrite
    public void turn(double yaw, double pitch) {
        if (Minecraft.getInstance().isMultiplayerPaused()) {
            return;
        }
        //Prevent moving camera on certain Special Attacks
        if (this instanceof PatchLivingEntity patch) {
            if (patch.isCameraLocked()) {
                return;
            }
        }
        //noinspection ConstantConditions
        if ((Object) this instanceof LivingEntity living) {
            if (!this.isOnGround() && living.onClimbable() && !(living instanceof Player player && player.getAbilities().flying)) {
                BlockState state = this.level.getBlockState(this.blockPosition());
                Block block = state.getBlock();
                if (block instanceof IClimbable climbable) {
                    float sweepAngle = climbable.getSweepAngle();
                    Direction dir = climbable.getDirection(state);
                    double dPitch = pitch * 0.15;
                    this.xRot += dPitch;
                    double dYaw = yaw * 0.15;
                    this.yRot += dYaw;
                    float partialYaw = this.yRot % 360;
                    this.yRot -= partialYaw;
                    boolean wasNegative = partialYaw < 0;
                    if (wasNegative) {
                        partialYaw += 360;
                    }
                    if (partialYaw >= 180) {
                        partialYaw -= 360;
                    }
                    float newYaw = MathHelper.clampAngle(partialYaw, sweepAngle, dir);
                    if (partialYaw < 0) {
                        partialYaw += 360;
                    }
                    if (newYaw < 0) {
                        newYaw += 360;
                    }
                    if (dir.getAxis() == Direction.Axis.X) {
                        if (partialYaw - newYaw <= -180) {
                            partialYaw += 360;
                        }
                        else if (partialYaw - newYaw >= 180) {
                            newYaw += 360;
                        }
                    }
                    newYaw = (partialYaw + partialYaw + partialYaw + newYaw) / 4;
                    if (dir.getAxis() == Direction.Axis.X) {
                        if (newYaw >= 360) {
                            newYaw -= 360;
                        }
                    }
                    if (wasNegative) {
                        newYaw -= 360;
                    }
                    this.yRot += newYaw;
                    this.xRot = MathHelper.clamp(this.xRot, -90.0F, 90.0F);
                    this.xRotO += dPitch;
                    this.yRotO += dYaw;
                    partialYaw = this.yRotO % 360;
                    this.yRotO -= partialYaw;
                    wasNegative = partialYaw < 0;
                    if (wasNegative) {
                        partialYaw += 360;
                    }
                    if (partialYaw >= 180) {
                        partialYaw -= 360;
                    }
                    newYaw = MathHelper.clampAngle(partialYaw, sweepAngle, dir);
                    if (partialYaw < 0) {
                        partialYaw += 360;
                    }
                    if (newYaw < 0) {
                        newYaw += 360;
                    }
                    if (dir.getAxis() == Direction.Axis.X) {
                        if (partialYaw - newYaw <= -180) {
                            partialYaw += 360;
                        }
                        else if (partialYaw - newYaw >= 180) {
                            newYaw += 360;
                        }
                    }
                    newYaw = (partialYaw + partialYaw + partialYaw + newYaw) / 4;
                    if (dir.getAxis() == Direction.Axis.X) {
                        if (newYaw >= 360) {
                            newYaw -= 360;
                        }
                    }
                    if (wasNegative) {
                        newYaw -= 360;
                    }
                    this.yRotO += newYaw;
                    this.xRotO = MathHelper.clamp(this.xRotO, -90.0F, 90.0F);
                    if (this.vehicle != null) {
                        this.vehicle.onPassengerTurned((Entity) (Object) this);
                    }
                    return;
                }
            }
        }
        float xDelta = LivingHooks.xDelta((Entity) (Object) this, EvolutionClient.getPartialTicks());
        float f = (float) pitch * 0.15F;
        float f1 = (float) yaw * 0.15F;
        this.setXRot(this.getXRot() + f);
        this.setYRot(this.getYRot() + f1);
        this.setXRot(Mth.clamp(this.getXRot(), -90.0F - xDelta, 90.0F - xDelta));
        this.xRotO += f;
        this.yRotO += f1;
        this.xRotO = Mth.clamp(this.xRotO, -90.0F - xDelta, 90.0F - xDelta);
        if (this.vehicle != null) {
            this.vehicle.onPassengerTurned((Entity) (Object) this);
        }
    }
}
