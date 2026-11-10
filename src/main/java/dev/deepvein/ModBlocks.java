package dev.deepvein;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DeepveinMod.MODID);

    public static final RegistryObject<Block> EMBERITE_ORE = BLOCKS.register("emberite_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_ORE), UniformInt.of(1, 3)));
    public static final RegistryObject<Block> GLOOMSILVER_ORE = BLOCKS.register("gloomsilver_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE), UniformInt.of(2, 5)));
    public static final RegistryObject<Block> AETHERIUM_ORE = BLOCKS.register("aetherium_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE), UniformInt.of(4, 8)));
    public static final RegistryObject<Block> LIFE_CRYSTAL_BLOCK = BLOCKS.register("life_crystal_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE).lightLevel(state -> 9)));
    public static final RegistryObject<Block> BLIGHTGRASS = BLOCKS.register("blightgrass",
            () -> new BlightBlock(BlockBehaviour.Properties.copy(Blocks.DIRT).randomTicks()));
    public static final RegistryObject<Block> BLIGHTSTONE = BLOCKS.register("blightstone",
            () -> new BlightBlock(BlockBehaviour.Properties.copy(Blocks.STONE).randomTicks()));

    /** A block that slowly converts neighbouring grass, dirt and stone into blight. */
    public static class BlightBlock extends Block {
        public BlightBlock(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) != 0) {
                return;
            }
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (!level.isLoaded(target)) {
                return;
            }
            BlockState there = level.getBlockState(target);
            if (there.is(Blocks.GRASS_BLOCK) || there.is(Blocks.DIRT)) {
                level.setBlockAndUpdate(target, BLIGHTGRASS.get().defaultBlockState());
            } else if (there.is(Blocks.STONE) || there.is(Blocks.DEEPSLATE)) {
                level.setBlockAndUpdate(target, BLIGHTSTONE.get().defaultBlockState());
            }
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (!level.isClientSide && entity instanceof Player player && !player.isCreative()) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
            }
            super.stepOn(level, pos, state, entity);
        }
    }
}
