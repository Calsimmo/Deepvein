package dev.deepvein.entity;

import dev.deepvein.ModItems;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared behaviour for both bosses: boss bar, enrage at half health, timed special attack. */
public abstract class BossEntity extends Monster {
    private final ServerBossEvent bossEvent;
    private int attackTimer;
    protected boolean enraged;

    protected BossEntity(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), color, BossEvent.BossBarOverlay.PROGRESS);
        this.xpReward = 150;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder bossAttributes(double health, double damage, double speed) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    protected abstract int attackInterval();

    protected abstract void specialAttack(LivingEntity target);

    protected abstract void onEnrage();

    protected abstract ItemStack trophy();

    protected void shootBolt(LivingEntity target, float damage, int kind, float inaccuracy) {
        MagicBolt bolt = new MagicBolt(this.level(), this, damage, kind);
        bolt.setPos(this.getX(), this.getEyeY() - 0.3D, this.getZ());
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5D) - bolt.getY();
        double dz = target.getZ() - this.getZ();
        bolt.shoot(dx, dy, dz, 1.3F, inaccuracy);
        this.level().addFreshEntity(bolt);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (!this.enraged && this.getHealth() < this.getMaxHealth() / 2.0F) {
            this.enraged = true;
            this.onEnrage();
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.attackTimer++;
            if (this.attackTimer >= this.attackInterval()) {
                this.attackTimer = 0;
                this.specialAttack(target);
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(this.trophy());
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    /** First boss: a hulking guardian that throws ember bolts and calls zombies when hurt. */
    public static class Gloomwarden extends BossEntity {
        public Gloomwarden(EntityType<? extends Monster> type, Level level) {
            super(type, level, BossEvent.BossBarColor.RED);
        }

        @Override
        protected int attackInterval() {
            return this.enraged ? 50 : 80;
        }

        @Override
        protected void specialAttack(LivingEntity target) {
            for (int i = 0; i < 3; i++) {
                this.shootBolt(target, 6.0F, MagicBolt.EMBER, 8.0F);
            }
            this.playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.6F);
        }

        @Override
        protected void onEnrage() {
            this.playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.7F);
            for (int i = 0; i < 4; i++) {
                Zombie zombie = EntityType.ZOMBIE.create(this.level());
                if (zombie != null) {
                    zombie.moveTo(this.getX() + this.random.nextInt(7) - 3, this.getY(),
                            this.getZ() + this.random.nextInt(7) - 3, this.random.nextFloat() * 360.0F, 0.0F);
                    this.level().addFreshEntity(zombie);
                }
            }
        }

        @Override
        protected ItemStack trophy() {
            return new ItemStack(ModItems.WARDEN_CORE.get(), 6 + this.random.nextInt(3));
        }
    }

    /** Second boss: teleports around its target and fires spreads of void bolts. */
    public static class RiftTyrant extends BossEntity {
        private int blinkTimer;

        public RiftTyrant(EntityType<? extends Monster> type, Level level) {
            super(type, level, BossEvent.BossBarColor.PURPLE);
        }

        @Override
        protected int attackInterval() {
            return this.enraged ? 35 : 60;
        }

        @Override
        protected void specialAttack(LivingEntity target) {
            int bolts = this.enraged ? 5 : 3;
            for (int i = 0; i < bolts; i++) {
                this.shootBolt(target, 9.0F, MagicBolt.VOID, 10.0F);
            }
            this.playSound(SoundEvents.SHULKER_SHOOT, 2.0F, 0.6F);
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                this.blinkTimer++;
                if (this.blinkTimer >= (this.enraged ? 90 : 150)) {
                    this.blinkTimer = 0;
                    double angle = this.random.nextDouble() * Math.PI * 2.0D;
                    this.teleportTo(target.getX() + Math.cos(angle) * 5.0D, target.getY() + 0.5D,
                            target.getZ() + Math.sin(angle) * 5.0D);
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 2.0F, 0.6F);
                }
            }
        }

        @Override
        protected void onEnrage() {
            this.playSound(SoundEvents.WITHER_SPAWN, 2.0F, 1.2F);
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
        }

        @Override
        protected ItemStack trophy() {
            return new ItemStack(ModItems.TYRANT_HEART.get(), 6 + this.random.nextInt(3));
        }
    }
}
