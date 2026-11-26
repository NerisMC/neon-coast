package com.pipsparty.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.pipsparty.registry.ModSounds;

/**
 * Office wall panel. DOOR buttons slam every Security Door within 3 blocks; LIGHT buttons toggle every
 * Hall Light within 4 blocks.
 */
public class ControlButtonBlock extends Block {
    public static final MapCodec<ControlButtonBlock> CODEC = simpleCodec(s -> new ControlButtonBlock(s, true));
    private final boolean door;

    public ControlButtonBlock(Properties settings, boolean door) {
        super(settings);
        this.door = door;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (door) {
            Boolean closing = null;
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof SecurityDoorBlock) {
                    if (closing == null) closing = !s.getValue(SecurityDoorBlock.CLOSED);
                    if (s.getValue(SecurityDoorBlock.CLOSED) != closing && com.pipsparty.shift.ShiftManager.doorsWork(level, pos)) {
                        level.setBlock(p, s.setValue(SecurityDoorBlock.CLOSED, closing), Block.UPDATE_ALL);
                    }
                }
            }
            if (closing == null) {
                player.sendOverlayMessage(Component.literal("No Security Door within 3 blocks of this button"));
            } else if (!com.pipsparty.shift.ShiftManager.doorsWork(level, pos)) {
                level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 0.6f);
            } else {
                level.playSound(null, pos, ModSounds.DOOR_SLAM.get(), SoundSource.BLOCKS, 1.0f, closing ? 1.0f : 1.2f);
            }
        } else {
            Boolean on = null;
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, 4, 4))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof HallLightBlock) {
                    if (on == null) on = !s.getValue(HallLightBlock.LIT) && com.pipsparty.shift.ShiftManager.doorsWork(level, pos);
                    level.setBlock(p, s.setValue(HallLightBlock.LIT, on), Block.UPDATE_ALL);
                }
            }
            if (on == null) player.sendOverlayMessage(Component.literal("No Hall Light within 4 blocks of this button"));
            else level.playSound(null, pos, ModSounds.LIGHT_BUZZ.get(), SoundSource.BLOCKS, on ? 0.8f : 0.3f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }
}
