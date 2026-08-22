package com.grgs.craftlevel.entity.projectile;

import com.grgs.craftlevel.damage.ModDamageTypes;
import com.grgs.craftlevel.entity.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Fireball estilo ghast: linha reta, sem desvio, impacto simula explosão creeper
 * com damage type {@code craftlevel:inferno_bolt_damage}.
 */
public class InfernoBoltProjectile extends LargeFireball {

    public static final float CREEPER_POWER = 3.0F;
    public static final int FIRE_SECONDS = 5;
    /** Velocidade um pouco abaixo de flecha carregada. */
    public static final double SPEED = 2.4D;

    private int blastRadius = 2;

    public InfernoBoltProjectile(EntityType<? extends InfernoBoltProjectile> type, Level level) {
        super(type, level);
    }

    public InfernoBoltProjectile(Level level, LivingEntity shooter, Vec3 direction, int blastRadius) {
        this(ModEntities.INFERNO_BOLT.get(), level);
        this.setOwner(shooter);
        this.blastRadius = Math.max(1, blastRadius);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        Vec3 dir = direction.lengthSqr() < 1.0E-6D ? shooter.getLookAngle() : direction.normalize();
        this.setDeltaMovement(dir.scale(SPEED));
        // Sem aceleração contínua (mantém velocidade ~flecha).
        this.xPower = 0.0D;
        this.yPower = 0.0D;
        this.zPower = 0.0D;
    }

    @Override
    public void tick() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            this.setDeltaMovement(motion.normalize().scale(SPEED));
        }
        this.xPower = 0.0D;
        this.yPower = 0.0D;
        this.zPower = 0.0D;
        super.tick();
    }

    public int getBlastRadius() {
        return blastRadius;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide()) {
            return;
        }
        detonate();
        this.discard();
    }

    private void detonate() {
        if (!(this.level() instanceof ServerLevel world)) {
            return;
        }
        Entity owner = this.getOwner();
        ServerPlayer caster = owner instanceof ServerPlayer player ? player : null;

        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        world.playSound(
                null,
                x,
                y,
                z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.0F,
                (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F
        );
        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 8, 0.5D, 0.4D, 0.5D, 0.0D);
        world.sendParticles(ParticleTypes.FLAME, x, y, z, 24, 0.6D, 0.5D, 0.6D, 0.08D);

        float diameter = CREEPER_POWER * 2.0F;
        double radius = blastRadius;
        double radiusSq = radius * radius;
        AABB area = new AABB(x, y, z, x, y, z).inflate(radius);

        for (LivingEntity entity : world.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> isValidTarget(caster, target) && target.distanceToSqr(x, y, z) <= radiusSq
        )) {
            double dist = Math.sqrt(entity.distanceToSqr(x, y, z));
            double distanceFactor = dist / diameter;
            if (distanceFactor > 1.0D) {
                // Ainda dentro do raio da skill, mas além do diameter creeper: usa borda mínima.
                distanceFactor = Math.min(1.0D, dist / Math.max(radius, 0.001D));
            }
            double impact = (1.0D - distanceFactor);
            if (impact <= 0.0D) {
                continue;
            }
            float damage = (float) ((impact * impact + impact) / 2.0D * 7.0D * diameter + 1.0D);
            Entity attacker = caster != null ? caster : owner;
            entity.hurt(ModDamageTypes.infernoBoltDamage(world, attacker), damage);
            entity.setSecondsOnFire(FIRE_SECONDS);
        }
    }

    private static boolean isValidTarget(ServerPlayer caster, LivingEntity target) {
        if (!target.isAlive() || !(target instanceof Enemy)) {
            return false;
        }
        return caster == null || target != caster;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BlastRadius", blastRadius);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BlastRadius")) {
            blastRadius = Math.max(1, tag.getInt("BlastRadius"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
