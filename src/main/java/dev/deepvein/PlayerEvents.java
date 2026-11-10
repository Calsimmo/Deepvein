package dev.deepvein;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerEvents {

    /** Accessories work while they sit in the hotbar or the off hand. */
    public static boolean hasAccessory(Player player, Item item) {
        if (player.getOffhandItem().is(item)) {
            return true;
        }
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void grant(Player player, Item item, MobEffect effect, int amplifier) {
        if (hasAccessory(player, item)) {
            player.addEffect(new MobEffectInstance(effect, 100, amplifier, true, false, true));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 10 == 0) {
            int current = Mana.get(player);
            int max = Mana.max(player);
            if (current < max) {
                int gain = hasAccessory(player, ModItems.MANA_LOCKET.get()) ? 2 : 1;
                Mana.set(player, current + gain);
            }
            Mana.sync(player);
        }
        if (player.tickCount % 20 == 0) {
            grant(player, ModItems.SWIFT_CHARM.get(), MobEffects.MOVEMENT_SPEED, 0);
            grant(player, ModItems.CLOUD_CHARM.get(), MobEffects.JUMP, 1);
            grant(player, ModItems.AEGIS_BAND.get(), MobEffects.DAMAGE_RESISTANCE, 0);
            grant(player, ModItems.EMBER_CHARM.get(), MobEffects.FIRE_RESISTANCE, 0);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        refresh(event.getEntity(), false);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        refresh(event.getEntity(), !event.isEndConquered());
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refresh(event.getEntity(), false);
    }

    private static void refresh(Player player, boolean fullHeal) {
        Mana.applyHealth(player);
        if (fullHeal) {
            player.setHealth(player.getMaxHealth());
        }
        if (player instanceof ServerPlayer sp) {
            Mana.sync(sp);
        }
    }

    /** Monsters killed by a player at night, under open sky, sometimes drop stardust. */
    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide || !(dead instanceof Enemy)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        if (dead.level().isNight() && dead.level().canSeeSky(dead.blockPosition())
                && dead.getRandom().nextFloat() < 0.25F) {
            event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(),
                    new ItemStack(ModItems.STARDUST.get())));
        }
    }
}
