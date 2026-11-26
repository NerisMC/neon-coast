package com.neoncoast.weapon;

import com.neoncoast.crime.CrimeHooks;
import com.neoncoast.npc.CitizenEntity;
import com.neoncoast.npc.PoliceOfficerEntity;
import com.neoncoast.vehicle.CarEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Hitscan shooting shared by players and armed police. */
public final class Ballistics {
    private Ballistics() {
    }

    /**
     * Fires one shot (all pellets) from the shooter's eyes along {@code aim}.
     * Handles damage (headshots do 1.6x), glass breaking, impact dust, tracers, muzzle flash, sound,
     * scaring citizens and reporting crimes.
     */
    public static void fire(ServerLevel level, LivingEntity shooter, GunStats stats, Vec3 aim, float extraSpread) {
        RandomSource r = shooter.getRandom();
        Vec3 eye = shooter.getEyePosition();
        Vec3 look = aim.normalize();
        Map<LivingEntity, Float> damage = new LinkedHashMap<>();
        boolean headshot = false;
        float spread = stats.spread() + extraSpread;

        for (int p = 0; p < stats.pellets(); p++) {
            Vec3 dir = look.add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize();
            Vec3 end = eye.add(dir.scale(stats.range()));
            BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
            AABB sweep = shooter.getBoundingBox().expandTowards(dir.scale(stats.range())).inflate(1.0);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(shooter, eye, stop, sweep,
                    e -> e.isPickable() && !e.isSpectator() && e != shooter && e != shooter.getVehicle() && !shooter.hasPassenger(e),
                    eye.distanceToSqr(stop));
            if (entityHit != null) {
                stop = entityHit.getLocation();
                Entity target = entityHit.getEntity();
                if (target instanceof CarEntity car) {
                    car.damageCar(stats.damage() * 0.5f);
                    if (car.isPolice() && shooter instanceof ServerPlayer sp) CrimeHooks.crime(sp, 1, "Shot at a police car");
                    sparks(level, stop);
                } else if (target instanceof LivingEntity living) {
                    float dmg = stats.damage();
                    if (stop.y > living.getEyeY() - 0.22) {
                        dmg *= 1.6f;
                        headshot = true;
                    }
                    damage.merge(living, dmg, Float::sum);
                    level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, stop.x, stop.y, stop.z, 2, 0.1, 0.1, 0.1, 0.1);
                } else {
                    target.hurtServer(level, source(level, shooter), stats.damage());
                }
            } else if (blockHit.getType() == HitResult.Type.BLOCK) {
                impact(level, blockHit);
            }
            tracer(level, eye.add(0, -0.12, 0).add(look.scale(0.8)), stop);
        }

        DamageSource src = source(level, shooter);
        for (Map.Entry<LivingEntity, Float> e : damage.entrySet()) {
            LivingEntity victim = e.getKey();
            victim.invulnerableTime = 0;
            victim.hurtServer(level, src, e.getValue());
            if (shooter instanceof ServerPlayer sp && !sp.isCreative()) {
                if (victim instanceof PoliceOfficerEntity) CrimeHooks.crime(sp, 2, "Shot a police officer");
                else if (victim instanceof CitizenEntity) CrimeHooks.witnessedCrime(sp, 1, "Shooting");
            }
        }
        if (headshot && shooter instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(net.minecraft.network.chat.Component.literal("HEADSHOT")
                    .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
        }

        // muzzle flash + bang (loud: you hear gunfights from far away)
        Vec3 muzzle = eye.add(0, -0.15, 0).add(look.scale(0.9));
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.02, 0.02, 0.02, 0.005);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.04, 0.04, 0.04, 0.01);
        level.playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(), stats.sound().get(), SoundSource.PLAYERS,
                4.0f, 0.92f + r.nextFloat() * 0.16f);

        CitizenEntity.scareNear(level, shooter.position(), 32.0, shooter);
        if (shooter instanceof ServerPlayer sp && !sp.isCreative()) CrimeHooks.shotsFired(sp);
    }

    private static DamageSource source(ServerLevel level, LivingEntity shooter) {
        return shooter instanceof Player p ? level.damageSources().playerAttack(p) : level.damageSources().mobAttack(shooter);
    }

    private static void impact(ServerLevel level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Vec3 at = hit.getLocation();
        if (state.getSoundType() == SoundType.GLASS && state.getDestroySpeed(level, pos) >= 0 && state.getDestroySpeed(level, pos) < 1.0f) {
            level.destroyBlock(pos, false); // windows shatter
            return;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 6, 0.05, 0.05, 0.05, 0.15);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.01);
    }

    private static void sparks(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 5, 0.05, 0.05, 0.05, 0.3);
    }

    private static void tracer(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 2.0) return;
        Vec3 step = d.scale(1.0 / len);
        int n = (int) Math.min(24, len / 2.0);
        for (int i = 1; i <= n; i++) {
            Vec3 p = from.add(step.scale(i * len / (n + 1)));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
