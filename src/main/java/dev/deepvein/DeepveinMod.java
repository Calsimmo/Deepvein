package dev.deepvein;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(DeepveinMod.MODID)
public class DeepveinMod {
    public static final String MODID = "deepvein";

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public DeepveinMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModEntities.ENTITIES.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(ModEntities::attributes);
        MinecraftForge.EVENT_BUS.register(new PlayerEvents());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.registerMessage(0, ManaSync.class, ManaSync::encode, ManaSync::decode, ManaSync::handle);
        });
    }

    public static void sendMana(ServerPlayer player, int mana, int max) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ManaSync(mana, max));
    }

    /** Client-side copy of the local player's mana, filled in by ManaSync packets. */
    public static class ClientMana {
        public static int mana = 20;
        public static int max = 20;
    }

    public static class ManaSync {
        private final int mana;
        private final int max;

        public ManaSync(int mana, int max) {
            this.mana = mana;
            this.max = max;
        }

        public static void encode(ManaSync msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.mana);
            buf.writeInt(msg.max);
        }

        public static ManaSync decode(FriendlyByteBuf buf) {
            return new ManaSync(buf.readInt(), buf.readInt());
        }

        public static void handle(ManaSync msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ClientMana.mana = msg.mana;
                ClientMana.max = msg.max;
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
