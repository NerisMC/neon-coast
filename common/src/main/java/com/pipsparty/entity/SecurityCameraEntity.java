package com.pipsparty.entity;

import com.pipsparty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A wall-mounted security camera. The Camera Tablet lets you look through it. Its name ("CAM 1A Show Stage")
 * is what the tablet shows. Animatronics moved by a night shift go to the floor spot this camera watches.
 */
public class SecurityCameraEntity extends Entity {
    public SecurityCameraEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved()) return false;
        if (source.getEntity() instanceof Player p) {
            if (!p.hasInfiniteMaterials()) {
                ItemStack drop = new ItemStack(ModItems.SECURITY_CAMERA.get());
                this.spawnAtLocation(level, drop);
            }
            this.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        // cameras don't move
        this.setDeltaMovement(Vec3.ZERO);
    }

    public String label() {
        return this.hasCustomName() ? this.getCustomName().getString() : "CAM";
    }

    /** Where an animatronic stands when it's "on this camera": a few blocks in front, on the floor. */
    public BlockPos roomSpot() {
        Vec3 dir = Vec3.directionFromRotation(0.0f, this.getYRot());
        Level level = this.level();
        for (int dist = 4; dist >= 1; dist--) {
            BlockPos column = BlockPos.containing(this.getX() + dir.x * dist, this.getY(), this.getZ() + dir.z * dist);
            for (int down = 0; down < 8; down++) {
                BlockPos feet = column.below(down);
                if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                        && level.getBlockState(feet.below()).isSolidRender()) {
                    return feet;
                }
            }
        }
        return this.blockPosition();
    }
}
