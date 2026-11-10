package dev.deepvein.entity;

import dev.deepvein.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/** Town merchant summoned with the Town Bell. Sells progression items for emeralds. */
public class TinkererEntity extends WanderingTrader {

    public TinkererEntity(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    private static MerchantOffer offer(int emeralds, Item item, int count) {
        return new MerchantOffer(new ItemStack(Items.EMERALD, emeralds), new ItemStack(item, count),
                9999, 2, 0.0F);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        offers.add(offer(3, ModItems.PURITY_POWDER.get(), 4));
        offers.add(offer(6, ModItems.STARDUST.get(), 2));
        offers.add(offer(12, ModItems.MANA_STAR.get(), 1));
        offers.add(offer(24, ModItems.LIFE_CRYSTAL.get(), 1));
        offers.add(offer(16, ModItems.SWIFT_CHARM.get(), 1));
        offers.add(offer(16, ModItems.CLOUD_CHARM.get(), 1));
        offers.add(offer(10, ModItems.GLOOM_LANTERN.get(), 1));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
