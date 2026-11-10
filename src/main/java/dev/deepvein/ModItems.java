package dev.deepvein;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import dev.deepvein.entity.MagicBolt;
import dev.deepvein.storage.Vault;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DeepveinMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DeepveinMod.MODID);

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> described(String name) {
        return ITEMS.register(name, () -> new DescItem(new Item.Properties().stacksTo(1)));
    }

    private static RegistryObject<Item> blockItem(RegistryObject<Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    // Blocks
    public static final RegistryObject<Item> EMBERITE_ORE = blockItem(ModBlocks.EMBERITE_ORE);
    public static final RegistryObject<Item> GLOOMSILVER_ORE = blockItem(ModBlocks.GLOOMSILVER_ORE);
    public static final RegistryObject<Item> AETHERIUM_ORE = blockItem(ModBlocks.AETHERIUM_ORE);
    public static final RegistryObject<Item> LIFE_CRYSTAL_BLOCK = blockItem(ModBlocks.LIFE_CRYSTAL_BLOCK);
    public static final RegistryObject<Item> BLIGHTGRASS = blockItem(ModBlocks.BLIGHTGRASS);
    public static final RegistryObject<Item> BLIGHTSTONE = blockItem(ModBlocks.BLIGHTSTONE);

    public static final RegistryObject<Item> VAULT_CORE = blockItem(ModBlocks.VAULT_CORE);
    public static final RegistryObject<Item> VAULT_CELL = blockItem(ModBlocks.VAULT_CELL);

    // Vault screen buttons (never obtainable)
    public static final RegistryObject<Item> VAULT_BUTTON_PREV = simple("vault_button_prev");
    public static final RegistryObject<Item> VAULT_BUTTON_NEXT = simple("vault_button_next");
    public static final RegistryObject<Item> VAULT_BUTTON_SORT = simple("vault_button_sort");
    public static final RegistryObject<Item> VAULT_BUTTON_DEPOSIT = simple("vault_button_deposit");
    public static final RegistryObject<Item> VAULT_BUTTON_PAGE = simple("vault_button_page");
    public static final RegistryObject<Item> VAULT_BUTTON_BLANK = simple("vault_button_blank");

    // Materials
    public static final RegistryObject<Item> RAW_EMBERITE = simple("raw_emberite");
    public static final RegistryObject<Item> RAW_GLOOMSILVER = simple("raw_gloomsilver");
    public static final RegistryObject<Item> RAW_AETHERIUM = simple("raw_aetherium");
    public static final RegistryObject<Item> EMBERITE_INGOT = simple("emberite_ingot");
    public static final RegistryObject<Item> GLOOMSILVER_INGOT = simple("gloomsilver_ingot");
    public static final RegistryObject<Item> AETHERIUM_INGOT = simple("aetherium_ingot");
    public static final RegistryObject<Item> STARDUST = simple("stardust");
    public static final RegistryObject<Item> BLIGHT_SPORE = simple("blight_spore");
    public static final RegistryObject<Item> WARDEN_CORE = simple("warden_core");
    public static final RegistryObject<Item> TYRANT_HEART = simple("tyrant_heart");

    // Projectile icons (not obtainable in survival)
    public static final RegistryObject<Item> EMBER_BOLT = simple("ember_bolt");
    public static final RegistryObject<Item> FROST_BOLT = simple("frost_bolt");
    public static final RegistryObject<Item> VOID_BOLT = simple("void_bolt");

    // Tiers
    public static final Tier EMBERITE_TIER = new ForgeTier(2, 400, 6.5F, 2.5F, 14,
            BlockTags.NEEDS_IRON_TOOL, () -> Ingredient.of(EMBERITE_INGOT.get()));
    public static final Tier GLOOMSILVER_TIER = new ForgeTier(3, 1200, 8.5F, 4.0F, 16,
            BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(GLOOMSILVER_INGOT.get()));
    public static final Tier AETHERIUM_TIER = new ForgeTier(4, 2400, 10.0F, 6.0F, 20,
            BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(AETHERIUM_INGOT.get()));

    private static void gear(String name, Tier tier, ArmorMaterial material) {
        ITEMS.register(name + "_sword", () -> new SwordItem(tier, 3, -2.4F, new Item.Properties()));
        ITEMS.register(name + "_pickaxe", () -> new PickaxeItem(tier, 1, -2.8F, new Item.Properties()));
        ITEMS.register(name + "_helmet",
                () -> new ArmorItem(material, ArmorItem.Type.HELMET, new Item.Properties()));
        ITEMS.register(name + "_chestplate",
                () -> new ArmorItem(material, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        ITEMS.register(name + "_leggings",
                () -> new ArmorItem(material, ArmorItem.Type.LEGGINGS, new Item.Properties()));
        ITEMS.register(name + "_boots",
                () -> new ArmorItem(material, ArmorItem.Type.BOOTS, new Item.Properties()));
    }

    static {
        gear("emberite", EMBERITE_TIER, ModArmor.EMBERITE);
        gear("gloomsilver", GLOOMSILVER_TIER, ModArmor.GLOOMSILVER);
        gear("aetherium", AETHERIUM_TIER, ModArmor.AETHERIUM);
    }

    // Magic weapons: (kind, mana cost, cooldown ticks, damage)
    public static final RegistryObject<Item> EMBER_WAND = ITEMS.register("ember_wand",
            () -> new MagicWeaponItem(MagicWeaponItem.EMBER, 4, 8, 6.0F));
    public static final RegistryObject<Item> FROST_STAFF = ITEMS.register("frost_staff",
            () -> new MagicWeaponItem(MagicWeaponItem.FROST, 6, 10, 10.0F));
    public static final RegistryObject<Item> STORM_TOME = ITEMS.register("storm_tome",
            () -> new MagicWeaponItem(MagicWeaponItem.STORM, 16, 30, 14.0F));
    public static final RegistryObject<Item> VOID_SCEPTER = ITEMS.register("void_scepter",
            () -> new MagicWeaponItem(MagicWeaponItem.VOID, 12, 12, 13.0F));

    // Permanent upgrades
    public static final RegistryObject<Item> LIFE_CRYSTAL = ITEMS.register("life_crystal",
            () -> new UpgradeItem(Mana.LIFE, Mana.MAX_LIFE));
    public static final RegistryObject<Item> MANA_STAR = ITEMS.register("mana_star",
            () -> new UpgradeItem(Mana.STARS, Mana.MAX_STARS));

    // Accessories (active in the hotbar or off hand)
    public static final RegistryObject<Item> SWIFT_CHARM = described("swift_charm");
    public static final RegistryObject<Item> CLOUD_CHARM = described("cloud_charm");
    public static final RegistryObject<Item> AEGIS_BAND = described("aegis_band");
    public static final RegistryObject<Item> EMBER_CHARM = described("ember_charm");
    public static final RegistryObject<Item> MANA_LOCKET = described("mana_locket");

    public static final RegistryObject<Item> LODESTONE_CHARM = described("lodestone_charm");
    public static final RegistryObject<Item> MINERS_LENS = described("miners_lens");

    // Quality of life
    public static final RegistryObject<Item> VAULT_REMOTE = ITEMS.register("vault_remote",
            () -> new VaultRemoteItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RECALL_MIRROR = ITEMS.register("recall_mirror",
            () -> new RecallMirrorItem(new Item.Properties().stacksTo(1)));

    // Summons and utility
    public static final RegistryObject<Item> GLOOM_LANTERN = ITEMS.register("gloom_lantern",
            () -> new SummonItem(ModEntities.GLOOMWARDEN, true, SoundEvents.WITHER_SPAWN));
    public static final RegistryObject<Item> RIFT_IDOL = ITEMS.register("rift_idol",
            () -> new SummonItem(ModEntities.RIFT_TYRANT, false, SoundEvents.END_PORTAL_SPAWN));
    public static final RegistryObject<Item> TOWN_BELL = ITEMS.register("town_bell",
            () -> new SummonItem(ModEntities.TINKERER, false, SoundEvents.BELL_BLOCK));
    public static final RegistryObject<Item> PURITY_POWDER = ITEMS.register("purity_powder",
            () -> new PurityPowderItem(new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.deepvein"))
                    .icon(() -> new ItemStack(EMBERITE_INGOT.get()))
                    .displayItems((params, output) ->
                            ITEMS.getEntries().forEach(entry -> {
                                String path = entry.getId().getPath();
                                if (!path.startsWith("vault_button") && !path.endsWith("_bolt")) {
                                    output.accept(entry.get());
                                }
                            }))
                    .build());

    // ---------------------------------------------------------------- armor

    public enum ModArmor implements ArmorMaterial {
        EMBERITE("emberite", 18, new int[] {2, 6, 5, 2}, 14, 0.5F, () -> Ingredient.of(EMBERITE_INGOT.get())),
        GLOOMSILVER("gloomsilver", 30, new int[] {3, 8, 6, 3}, 16, 2.0F,
                () -> Ingredient.of(GLOOMSILVER_INGOT.get())),
        AETHERIUM("aetherium", 40, new int[] {4, 9, 7, 4}, 20, 3.5F,
                () -> Ingredient.of(AETHERIUM_INGOT.get()));

        private static final int[] BASE_DURABILITY = {11, 16, 15, 13};
        private final String id;
        private final int durability;
        private final int[] defense;
        private final int enchant;
        private final float toughness;
        private final Supplier<Ingredient> repair;

        ModArmor(String id, int durability, int[] defense, int enchant, float toughness,
                Supplier<Ingredient> repair) {
            this.id = id;
            this.durability = durability;
            this.defense = defense;
            this.enchant = enchant;
            this.toughness = toughness;
            this.repair = repair;
        }

        private static int index(ArmorItem.Type type) {
            switch (type) {
                case HELMET:
                    return 0;
                case CHESTPLATE:
                    return 1;
                case LEGGINGS:
                    return 2;
                default:
                    return 3;
            }
        }

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return BASE_DURABILITY[index(type)] * this.durability;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return this.defense[index(type)];
        }

        @Override
        public int getEnchantmentValue() {
            return this.enchant;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_IRON;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return this.repair.get();
        }

        @Override
        public String getName() {
            return DeepveinMod.MODID + ":" + this.id;
        }

        @Override
        public float getToughness() {
            return this.toughness;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    }

    // ---------------------------------------------------------------- item classes

    /** Item with a grey description line read from "<item key>.desc" in the language file. */
    public static class DescItem extends Item {
        public DescItem(Item.Properties props) {
            super(props);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                TooltipFlag flag) {
            tooltip.add(Component.translatable(this.getDescriptionId() + ".desc")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static class MagicWeaponItem extends DescItem {
        public static final int EMBER = 0;
        public static final int FROST = 1;
        public static final int VOID = 2;
        public static final int STORM = 3;

        private final int kind;
        private final int cost;
        private final int cooldown;
        private final float damage;

        public MagicWeaponItem(int kind, int cost, int cooldown, float damage) {
            super(new Item.Properties().stacksTo(1));
            this.kind = kind;
            this.cost = cost;
            this.cooldown = cooldown;
            this.damage = damage;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                return InteractionResultHolder.success(stack);
            }
            if (!Mana.tryConsume(player, this.cost)) {
                player.displayClientMessage(Component.translatable("message.deepvein.no_mana"), true);
                return InteractionResultHolder.fail(stack);
            }
            if (this.kind == STORM) {
                Vec3 spot = player.pick(40.0D, 0.0F, false).getLocation();
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(spot.x, spot.y, spot.z);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                AABB area = new AABB(spot, spot).inflate(3.5D);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
                    if (victim != player) {
                        victim.hurt(level.damageSources().indirectMagic(player, player), this.damage);
                    }
                }
            } else {
                int bolts = this.kind == VOID ? 3 : 1;
                for (int i = 0; i < bolts; i++) {
                    MagicBolt bolt = new MagicBolt(level, player, this.damage, this.kind);
                    bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F,
                            bolts > 1 ? 6.0F : 1.0F);
                    level.addFreshEntity(bolt);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS,
                        0.6F, 1.4F);
            }
            player.getCooldowns().addCooldown(this, this.cooldown);
            return InteractionResultHolder.success(stack);
        }
    }

    /** Life Crystal (+1 heart) and Mana Star (+20 max mana), each with a cap. */
    public static class UpgradeItem extends DescItem {
        private final String key;
        private final int cap;

        public UpgradeItem(String key, int cap) {
            super(new Item.Properties());
            this.key = key;
            this.cap = cap;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                return InteractionResultHolder.success(stack);
            }
            int used = Mana.data(player).getInt(this.key);
            if (used >= this.cap) {
                player.displayClientMessage(Component.translatable("message.deepvein.maxed"), true);
                return InteractionResultHolder.fail(stack);
            }
            Mana.data(player).putInt(this.key, used + 1);
            if (this.key.equals(Mana.LIFE)) {
                Mana.applyHealth(player);
                player.heal(2.0F);
            } else {
                Mana.set(player, Mana.max(player));
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                    0.8F, 1.2F);
            return InteractionResultHolder.success(stack);
        }
    }

    /** Spawns a boss or the town merchant a few blocks in front of the player. */
    public static class SummonItem extends DescItem {
        private final Supplier<? extends EntityType<? extends Mob>> type;
        private final boolean nightOnly;
        private final SoundEvent sound;

        public SummonItem(Supplier<? extends EntityType<? extends Mob>> type, boolean nightOnly,
                SoundEvent sound) {
            super(new Item.Properties().stacksTo(16));
            this.type = type;
            this.nightOnly = nightOnly;
            this.sound = sound;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                return InteractionResultHolder.success(stack);
            }
            if (this.nightOnly && level.isDay()) {
                player.displayClientMessage(Component.translatable("message.deepvein.night_only"), true);
                return InteractionResultHolder.fail(stack);
            }
            Mob mob = this.type.get().create(level);
            if (mob == null) {
                return InteractionResultHolder.fail(stack);
            }
            Vec3 look = player.getLookAngle();
            double len = Math.max(0.001D, Math.sqrt(look.x * look.x + look.z * look.z));
            mob.moveTo(player.getX() + look.x / len * 5.0D, player.getY() + 0.5D,
                    player.getZ() + look.z / len * 5.0D, player.getYRot() + 180.0F, 0.0F);
            level.addFreshEntity(mob);
            level.playSound(null, player.blockPosition(), this.sound, SoundSource.HOSTILE, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        }
    }

    /** Opens a linked Vault Core from anywhere in the same dimension while its chunk is loaded. */
    public static class VaultRemoteItem extends DescItem {
        public VaultRemoteItem(Item.Properties props) {
            super(props);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            if (!(level.getBlockEntity(pos) instanceof Vault.CoreBlockEntity)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                CompoundTag tag = context.getItemInHand().getOrCreateTag();
                tag.putLong("VaultPos", pos.asLong());
                tag.putString("VaultDim", level.dimension().location().toString());
                Player player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.deepvein.remote_linked"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                return InteractionResultHolder.success(stack);
            }
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("VaultPos")) {
                player.displayClientMessage(Component.translatable("message.deepvein.remote_unlinked"), true);
                return InteractionResultHolder.fail(stack);
            }
            BlockPos pos = BlockPos.of(tag.getLong("VaultPos"));
            boolean sameDimension = level.dimension().location().toString().equals(tag.getString("VaultDim"));
            if (sameDimension && level.isLoaded(pos)
                    && level.getBlockEntity(pos) instanceof Vault.CoreBlockEntity core) {
                Vault.open(player, core, true);
                return InteractionResultHolder.success(stack);
            }
            player.displayClientMessage(Component.translatable("message.deepvein.remote_out_of_range"), true);
            return InteractionResultHolder.fail(stack);
        }
    }

    /** Teleports the player to their bed or respawn anchor, or to world spawn if they have none. */
    public static class RecallMirrorItem extends DescItem {
        public RecallMirrorItem(Item.Properties props) {
            super(props);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
                return InteractionResultHolder.success(stack);
            }
            BlockPos pos = sp.getRespawnPosition();
            ServerLevel target = sp.server.getLevel(sp.getRespawnDimension());
            if (pos == null || target == null) {
                target = sp.server.overworld();
                pos = target.getSharedSpawnPos();
            }
            sp.teleportTo(target, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    sp.getYRot(), sp.getXRot());
            target.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            sp.getCooldowns().addCooldown(this, 200);
            return InteractionResultHolder.success(stack);
        }
    }

    /** Turns blight back into grass and stone in a 5x5x5 area. */
    public static class PurityPowderItem extends DescItem {
        public PurityPowderItem(Item.Properties props) {
            super(props);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockPos centre = context.getClickedPos();
            boolean changed = false;
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, -2, -2), centre.offset(2, 2, 2))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(ModBlocks.BLIGHTGRASS.get())) {
                    level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
                    changed = true;
                } else if (state.is(ModBlocks.BLIGHTSTONE.get())) {
                    level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                    changed = true;
                }
            }
            if (!changed) {
                return InteractionResult.PASS;
            }
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            level.playSound(null, centre, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
    }
}
