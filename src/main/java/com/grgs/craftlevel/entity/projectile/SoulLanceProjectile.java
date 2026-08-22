package com.grgs.craftlevel.entity.projectile;

import com.grgs.craftlevel.damage.ModDamageTypes;
import com.grgs.craftlevel.entity.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Tridente sem gravidade: dano soul_lance_damage + raios no alvo.
 */
public class SoulLanceProjectile extends ThrownTrident {

    public static final float BASE_TRIDENT_DAMAGE = 8.0F;
    public static final double SPEED = 2.5D;

    private float damageMultiplier = 2.0F;
    private int lightningCount = 1;

    public SoulLanceProjectile(EntityType<? extends SoulLanceProjectile> type, Level level) {
        super(type, level);
    }

    public SoulLanceProjectile(
            Level level,
            LivingEntity shooter,
            Vec3 direction,
            float damageMultiplier,
            int lightningCount
    ) {
        this(ModEntities.SOUL_LANCE.get(), level);
        this.setOwner(shooter);
        this.damageMultiplier = Math.max(1.0F, damageMultiplier);
        this.lightningCount = Math.max(1, lightningCount);
        this.setNoGravity(true);
        this.pickup = Pickup.DISALLOWED;
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        Vec3 dir = direction.lengthSqr() < 1.0E-6D ? shooter.getLookAngle() : direction.normalize();
        this.shoot(dir.x, dir.y, dir.z, (float) SPEED, 0.0F);
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        super.tick();
        if (!this.level().isClientSide() && this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (!(this.level() instanceof ServerLevel world)) {
            this.discard();
            return;
        }
        Entity owner = this.getOwner();
        if (target instanceof LivingEntity living && living.isAlive()) {
            float damage = BASE_TRIDENT_DAMAGE * damageMultiplier;
            living.hurt(ModDamageTypes.soulLanceDamage(world, owner != null ? owner : this), damage);
            strikeLightning(world, living, owner);
        }
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            onHitEntity((EntityHitResult) result);
            return;
        }
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    private void strikeLightning(ServerLevel world, LivingEntity target, Entity owner) {
        for (int i = 0; i < lightningCount; i++) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
            if (bolt == null) {
                continue;
            }
            double ox = (i == 0) ? 0.0D : (world.random.nextDouble() - 0.5D) * 0.8D;
            double oz = (i == 0) ? 0.0D : (world.random.nextDouble() - 0.5D) * 0.8D;
            bolt.moveTo(target.getX() + ox, target.getY(), target.getZ() + oz);
            bolt.setVisualOnly(true);
            if (owner instanceof ServerPlayer player) {
                bolt.setCause(player);
            }
            world.addFreshEntity(bolt);
        }
    }

    @Override
    public ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("DamageMultiplier", damageMultiplier);
        tag.putInt("LightningCount", lightningCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DamageMultiplier")) {
            damageMultiplier = Math.max(1.0F, tag.getFloat("DamageMultiplier"));
        }
        if (tag.contains("LightningCount")) {
            lightningCount = Math.max(1, tag.getInt("LightningCount"));
        }
        this.setNoGravity(true);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
