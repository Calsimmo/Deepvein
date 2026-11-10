package dev.deepvein;

import dev.deepvein.entity.BossEntity;
import dev.deepvein.entity.MagicBolt;
import dev.deepvein.entity.TinkererEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DeepveinMod.MODID);

    public static final RegistryObject<EntityType<BossEntity.Gloomwarden>> GLOOMWARDEN =
            ENTITIES.register("gloomwarden", () -> EntityType.Builder
                    .<BossEntity.Gloomwarden>of(BossEntity.Gloomwarden::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.9F).clientTrackingRange(10).build("gloomwarden"));

    public static final RegistryObject<EntityType<BossEntity.RiftTyrant>> RIFT_TYRANT =
            ENTITIES.register("rift_tyrant", () -> EntityType.Builder
                    .<BossEntity.RiftTyrant>of(BossEntity.RiftTyrant::new, MobCategory.MONSTER)
                    .sized(1.5F, 4.8F).clientTrackingRange(10).build("rift_tyrant"));

    public static final RegistryObject<EntityType<TinkererEntity>> TINKERER =
            ENTITIES.register("tinkerer", () -> EntityType.Builder
                    .<TinkererEntity>of(TinkererEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("tinkerer"));

    public static final RegistryObject<EntityType<MagicBolt>> MAGIC_BOLT =
            ENTITIES.register("magic_bolt", () -> EntityType.Builder
                    .<MagicBolt>of(MagicBolt::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(10).build("magic_bolt"));

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(GLOOMWARDEN.get(), BossEntity.bossAttributes(300.0D, 10.0D, 0.30D).build());
        event.put(RIFT_TYRANT.get(), BossEntity.bossAttributes(600.0D, 16.0D, 0.34D).build());
        event.put(TINKERER.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D).build());
    }
}
