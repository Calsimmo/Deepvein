package dev.deepvein;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Per-player progression data: mana, mana stars used and life crystals used. */
public class Mana {
    public static final String MANA = "dv_mana";
    public static final String STARS = "dv_stars";
    public static final String LIFE = "dv_life";
    public static final int MAX_STARS = 9;
    public static final int MAX_LIFE = 10;
    private static final UUID LIFE_UUID = UUID.fromString("5d7c2a44-91f3-4b7e-a9b6-2f6f6f0d1c11");

    /** Data that survives death (Forge copies this tag to the respawned player). */
    public static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static int max(Player player) {
        return 20 + 20 * Math.min(MAX_STARS, data(player).getInt(STARS));
    }

    public static int get(Player player) {
        CompoundTag tag = data(player);
        int max = max(player);
        if (!tag.contains(MANA)) {
            return max;
        }
        return Math.min(max, tag.getInt(MANA));
    }

    public static void set(Player player, int value) {
        data(player).putInt(MANA, Math.max(0, Math.min(max(player), value)));
    }

    public static boolean tryConsume(Player player, int cost) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        int current = get(player);
        if (current < cost) {
            return false;
        }
        set(player, current - cost);
        if (player instanceof ServerPlayer sp) {
            sync(sp);
        }
        return true;
    }

    public static void sync(ServerPlayer player) {
        DeepveinMod.sendMana(player, get(player), max(player));
    }

    /** Re-applies the bonus hearts from life crystals. */
    public static void applyHealth(Player player) {
        AttributeInstance inst = player.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) {
            return;
        }
        inst.removeModifier(LIFE_UUID);
        int used = Math.min(MAX_LIFE, data(player).getInt(LIFE));
        if (used > 0) {
            inst.addPermanentModifier(new AttributeModifier(LIFE_UUID, "Deepvein life crystals",
                    used * 2.0D, AttributeModifier.Operation.ADDITION));
        }
    }
}
