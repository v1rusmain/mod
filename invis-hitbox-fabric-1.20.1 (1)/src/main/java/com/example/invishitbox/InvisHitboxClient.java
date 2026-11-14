package com.example.invishitbox;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class InvisHitboxClient implements ClientModInitializer {

    private static boolean enabled = true;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        // Клавиша H — включить/выключить подсветку (можно поменять в настройках управления)
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.invishitbox.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.categories.invishitbox"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.translatable(enabled ? "invishitbox.enabled" : "invishitbox.disabled"),
                            true // показать над хотбаром
                    );
                }
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(InvisHitboxClient::renderHitboxes);
    }

    private static void renderHitboxes(WorldRenderContext context) {
        if (!enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrixStack();
        if (consumers == null || matrices == null) return;

        Vec3d cam = context.camera().getPos();
        float tickDelta = context.tickDelta();
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;      // себя не подсвечиваем
            if (player.isSpectator()) continue;     // наблюдатели не нужны
            if (!player.isInvisible()) continue;    // только невидимые

            // Плавная интерполяция позиции между тиками, чтобы рамка не дёргалась
            double dx = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()) - player.getX();
            double dy = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()) - player.getY();
            double dz = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()) - player.getZ();

            Box box = player.getBoundingBox().offset(dx - cam.x, dy - cam.y, dz - cam.z);

            // Красная рамка хитбокса
            WorldRenderer.drawBox(matrices, lines, box, 1.0f, 0.15f, 0.15f, 1.0f);
        }
    }
}
