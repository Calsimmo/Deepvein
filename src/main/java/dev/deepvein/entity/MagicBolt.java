package dev.deepvein.entity;

import dev.deepvein.ModEntities;
import dev.deepvein.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Straight-flying magic projectile. kind: 0 = ember, 1 = frost, 2 = void. */
public class MagicBolt extends ThrowableItemProjectile {
    public static final int EMBER = 0;
    public static final int FROST = 1;
    public static final int VOID = 2;

    private float damage = 6.0F;
    private int kind = EMBER;

    public MagicBolt(EntityType<? extends MagicBolt> type, Level level) {
        super(type, level);
    }

    public MagicBolt(Level level, LivingEntity owner, float damage, int kind) {
        super(ModEntities.MAGIC_BOLT.get(), owner, level);
        this.damage = damage;
        this.kind = kind;
        Item icon = kind == FROST ? ModItems.FROST_BOLT.get()
                : kind == VOID ? ModItems.VOID_BOLT.get() : ModItems.EMBER_BOLT.get();
        this.setItem(new ItemStack(icon));
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.EMBER_BOLT.get();
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > 80) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (this.level().isClientSide) {
            return;
        }
        target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.damage);
        if (target instanceof LivingEntity living) {
            if (this.kind == EMBER) {
                living.setSecondsOnFire(4);
            } else if (this.kind == FROST) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            } else {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}
