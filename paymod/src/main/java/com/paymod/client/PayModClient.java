package com.paymod.client;

import com.paymod.AmountUtil;
import com.paymod.BalancePayload;
import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PayModClient implements ClientModInitializer {
    private static boolean hudVisible = false;
    private static long balance = 0;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BalancePayload.ID,
                (payload, context) -> balance = payload.balance());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> balance = 0);

        // /ui toggles the money panel
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("ui").executes(ctx -> {
                    hudVisible = !hudVisible;
                    ctx.getSource().sendFeedback(Text.literal("Money UI " + (hudVisible ? "on" : "off")));
                    return Command.SINGLE_SUCCESS;
                })));

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!hudVisible || mc.player == null || mc.options.hudHidden) return;

            TextRenderer tr = mc.textRenderer;
            Text name = Text.literal(mc.player.getName().getString()).formatted(Formatting.WHITE);
            Text money = Text.literal("$ ").formatted(Formatting.GREEN)
                    .append(Text.literal(AmountUtil.format(balance)).formatted(Formatting.WHITE));

            int w = drawContext.getScaledWindowWidth();
            int h = drawContext.getScaledWindowHeight();
            int y = h / 2 - 10;

            drawContext.drawTextWithShadow(tr, name, w - tr.getWidth(name) - 6, y, 0xFFFFFF);
            drawContext.drawTextWithShadow(tr, money, w - tr.getWidth(money) - 6, y + 11, 0xFFFFFF);
        });
    }
}
