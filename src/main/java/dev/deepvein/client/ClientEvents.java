package dev.deepvein.client;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.deepvein.DeepveinMod;
import dev.deepvein.ModEntities;
import dev.deepvein.entity.BossEntity;
import dev.deepvein.entity.MagicBolt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeepveinMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.<BossEntity>registerEntityRenderer(ModEntities.GLOOMWARDEN.get(),
                ctx -> new BossRenderer(ctx, "gloomwarden", 2.0F));
        event.<BossEntity>registerEntityRenderer(ModEntities.RIFT_TYRANT.get(),
                ctx -> new BossRenderer(ctx, "rift_tyrant", 2.5F));
        event.<WanderingTrader>registerEntityRenderer(ModEntities.TINKERER.get(),
                ctx -> new WanderingTraderRenderer(ctx));
        event.<MagicBolt>registerEntityRenderer(ModEntities.MAGIC_BOLT.get(),
                ctx -> new ThrownItemRenderer<MagicBolt>(ctx));
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("mana", (gui, graphics, partialTick, width, height) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) {
                return;
            }
            int mana = DeepveinMod.ClientMana.mana;
            int max = Math.max(1, DeepveinMod.ClientMana.max);
            int barWidth = 80;
            int x = width - barWidth - 8;
            int y = 8;
            int filled = Math.round(barWidth * Math.min(1.0F, mana / (float) max));
            graphics.fill(x - 1, y - 1, x + barWidth + 1, y + 7, 0xAA000000);
            graphics.fill(x, y, x + filled, y + 6, 0xFF4A7BFF);
            graphics.drawString(mc.font, "Mana " + mana + "/" + max, x, y + 10, 0xFF9FC0FF);
        });
    }

    /** Draws a boss with the standard humanoid model, scaled up, using its own texture. */
    public static class BossRenderer extends MobRenderer<BossEntity, HumanoidModel<BossEntity>> {
        private final ResourceLocation texture;
        private final float scale;

        public BossRenderer(EntityRendererProvider.Context ctx, String name, float scale) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F * scale);
            this.texture = new ResourceLocation(DeepveinMod.MODID, "textures/entity/" + name + ".png");
            this.scale = scale;
        }

        @Override
        public ResourceLocation getTextureLocation(BossEntity entity) {
            return this.texture;
        }

        @Override
        protected void scale(BossEntity entity, PoseStack pose, float partialTick) {
            pose.scale(this.scale, this.scale, this.scale);
        }
    }
}
