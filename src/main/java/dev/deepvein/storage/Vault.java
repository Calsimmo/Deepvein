package dev.deepvein.storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import dev.deepvein.ModBlocks;
import dev.deepvein.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Vault storage network. A Vault Core holds the items; every Vault Cell connected to it adds a page of
 * 45 slots. The screen is the standard six-row chest: five rows of items and a bottom row of buttons.
 */
public class Vault {
    public static final int PAGE = 45;
    public static final int SLOTS = 54;
    public static final int MAX_PAGES = 64;

    public static void open(Player player, CoreBlockEntity core, boolean remote) {
        PageContainer view = new PageContainer(core, core.pages(), remote);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new VaultMenu(id, inventory, view),
                Component.translatable("container.deepvein.vault")));
    }

    public static class CoreBlock extends BaseEntityBlock {
        public CoreBlock(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CoreBlockEntity(pos, state);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                InteractionHand hand, BlockHitResult hit) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof CoreBlockEntity core) {
                open(player, core, false);
            }
            return InteractionResult.CONSUME;
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CoreBlockEntity core) {
                for (ItemStack stack : core.items) {
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
                core.items.clear();
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    public static class CoreBlockEntity extends BlockEntity {
        public final List<ItemStack> items = new ArrayList<>();

        public CoreBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlocks.VAULT_CORE_BE.get(), pos, state);
        }

        public ItemStack get(int index) {
            return index < this.items.size() ? this.items.get(index) : ItemStack.EMPTY;
        }

        public void set(int index, ItemStack stack) {
            while (this.items.size() <= index) {
                this.items.add(ItemStack.EMPTY);
            }
            this.items.set(index, stack);
            this.setChanged();
        }

        /** One page for the core plus one per Vault Cell connected through other cells. */
        public int pages() {
            if (this.level == null) {
                return 1;
            }
            Set<BlockPos> seen = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(this.worldPosition);
            seen.add(this.worldPosition);
            int cells = 0;
            while (!queue.isEmpty() && cells < MAX_PAGES - 1) {
                BlockPos current = queue.poll();
                for (Direction dir : Direction.values()) {
                    BlockPos next = current.relative(dir);
                    if (seen.add(next) && this.level.isLoaded(next)
                            && this.level.getBlockState(next).is(ModBlocks.VAULT_CELL.get())) {
                        cells++;
                        queue.add(next);
                        if (cells >= MAX_PAGES - 1) {
                            break;
                        }
                    }
                }
            }
            return 1 + cells;
        }

        public boolean contains(Item item) {
            for (ItemStack stack : this.items) {
                if (stack.is(item)) {
                    return true;
                }
            }
            return false;
        }

        /** Adds a stack anywhere within the given capacity and returns what did not fit. */
        public ItemStack insert(ItemStack stack, int capacity) {
            for (int i = 0; i < Math.min(capacity, this.items.size()) && !stack.isEmpty(); i++) {
                ItemStack there = this.items.get(i);
                if (!there.isEmpty() && ItemStack.isSameItemSameTags(there, stack)
                        && there.getCount() < there.getMaxStackSize()) {
                    int moved = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                    there.grow(moved);
                    stack.shrink(moved);
                }
            }
            for (int i = 0; i < capacity && !stack.isEmpty(); i++) {
                if (this.get(i).isEmpty()) {
                    this.set(i, stack.copy());
                    stack.setCount(0);
                }
            }
            this.setChanged();
            return stack.isEmpty() ? ItemStack.EMPTY : stack;
        }

        /** Merges partial stacks and orders everything by item id. */
        public void sort() {
            List<ItemStack> merged = new ArrayList<>();
            for (ItemStack stack : this.items) {
                if (stack.isEmpty()) {
                    continue;
                }
                for (ItemStack target : merged) {
                    if (stack.isEmpty()) {
                        break;
                    }
                    if (ItemStack.isSameItemSameTags(target, stack)
                            && target.getCount() < target.getMaxStackSize()) {
                        int moved = Math.min(stack.getCount(), target.getMaxStackSize() - target.getCount());
                        target.grow(moved);
                        stack.shrink(moved);
                    }
                }
                if (!stack.isEmpty()) {
                    merged.add(stack);
                }
            }
            merged.sort(Comparator.comparing(
                    (ItemStack stack) -> String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()))));
            this.items.clear();
            this.items.addAll(merged);
            this.setChanged();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            ListTag list = new ListTag();
            for (int i = 0; i < this.items.size(); i++) {
                ItemStack stack = this.items.get(i);
                if (!stack.isEmpty()) {
                    CompoundTag entry = new CompoundTag();
                    entry.putInt("Slot", i);
                    stack.save(entry);
                    list.add(entry);
                }
            }
            tag.put("Items", list);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            this.items.clear();
            ListTag list = tag.getList("Items", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot < 0 || slot >= MAX_PAGES * PAGE) {
                    continue;
                }
                while (this.items.size() <= slot) {
                    this.items.add(ItemStack.EMPTY);
                }
                this.items.set(slot, ItemStack.of(entry));
            }
        }
    }

    /** One player's view of a vault: the current page plus the button row. */
    public static class PageContainer implements Container {
        public final CoreBlockEntity core;
        public final int pages;
        public final boolean remote;
        public int page;

        public PageContainer(CoreBlockEntity core, int pages, boolean remote) {
            this.core = core;
            this.pages = Math.max(1, pages);
            this.remote = remote;
        }

        private ItemStack button(int column) {
            switch (column) {
                case 0:
                    return new ItemStack(ModItems.VAULT_BUTTON_PREV.get());
                case 1:
                    return new ItemStack(ModItems.VAULT_BUTTON_NEXT.get());
                case 3:
                    return new ItemStack(ModItems.VAULT_BUTTON_SORT.get());
                case 4:
                    return new ItemStack(ModItems.VAULT_BUTTON_DEPOSIT.get());
                case 8:
                    ItemStack label = new ItemStack(ModItems.VAULT_BUTTON_PAGE.get(), Math.min(64, this.page + 1));
                    label.setHoverName(Component.literal("Page " + (this.page + 1) + " / " + this.pages));
                    return label;
                default:
                    return new ItemStack(ModItems.VAULT_BUTTON_BLANK.get());
            }
        }

        private int index(int slot) {
            return this.page * PAGE + slot;
        }

        @Override
        public int getContainerSize() {
            return SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot < PAGE ? this.core.get(this.index(slot)) : this.button(slot - PAGE);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            if (slot >= PAGE) {
                return ItemStack.EMPTY;
            }
            ItemStack there = this.core.get(this.index(slot));
            if (there.isEmpty() || count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = there.split(count);
            if (there.isEmpty()) {
                this.core.set(this.index(slot), ItemStack.EMPTY);
            }
            this.core.setChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot >= PAGE) {
                return ItemStack.EMPTY;
            }
            ItemStack there = this.core.get(this.index(slot));
            this.core.set(this.index(slot), ItemStack.EMPTY);
            return there;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < PAGE) {
                this.core.set(this.index(slot), stack);
            }
        }

        @Override
        public void setChanged() {
            this.core.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            if (this.core.isRemoved()) {
                return false;
            }
            BlockPos pos = this.core.getBlockPos();
            return this.remote
                    || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
        }

        @Override
        public void clearContent() {
        }
    }

    public static class VaultMenu extends ChestMenu {
        private final PageContainer view;

        public VaultMenu(int id, Inventory inventory, PageContainer view) {
            super(MenuType.GENERIC_9x6, id, inventory, view, 6);
            this.view = view;
        }

        @Override
        public void clicked(int slotId, int button, ClickType type, Player player) {
            if (slotId >= PAGE && slotId < SLOTS) {
                if (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE) {
                    this.press(slotId - PAGE, player);
                }
                this.sendAllDataToRemote();
                return;
            }
            super.clicked(slotId, button, type, player);
        }

        private void press(int column, Player player) {
            if (column == 0) {
                this.view.page = Math.max(0, this.view.page - 1);
            } else if (column == 1) {
                this.view.page = Math.min(this.view.pages - 1, this.view.page + 1);
            } else if (column == 3) {
                this.view.core.sort();
            } else if (column == 4) {
                // quick stack: move backpack items that the vault already holds
                Inventory inventory = player.getInventory();
                for (int i = 9; i < 36; i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (!stack.isEmpty() && this.view.core.contains(stack.getItem())) {
                        inventory.setItem(i, this.view.core.insert(stack.copy(), this.view.pages * PAGE));
                    }
                }
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            if (index < 0 || index >= this.slots.size()) {
                return ItemStack.EMPTY;
            }
            Slot slot = this.slots.get(index);
            if (!slot.hasItem() || (index >= PAGE && index < SLOTS)) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            if (index < PAGE) {
                this.moveItemStackTo(stack, SLOTS, this.slots.size(), true);
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            } else {
                slot.set(this.view.core.insert(stack.copy(), this.view.pages * PAGE));
            }
            return ItemStack.EMPTY;
        }
    }
}
