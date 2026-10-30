package com.paymod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PayMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("paymod");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(BalancePayload.ID, BalancePayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> MoneyManager.load());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> MoneyManager.save());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private void register(CommandDispatcher<ServerCommandSource> d) {
        // /pay <player> <amount>
        d.register(literal("pay")
                .then(argument("player", EntityArgumentType.player())
                        .then(argument("amount", StringArgumentType.word())
                                .executes(this::pay))));

        // /set money <amount>
        d.register(literal("set")
                .then(literal("money")
                        .then(argument("amount", StringArgumentType.word())
                                .executes(this::setMoney))));

        // /setmoney <amount>  (same thing, shorter)
        d.register(literal("setmoney")
                .then(argument("amount", StringArgumentType.word())
                        .executes(this::setMoney)));
    }

    private int setMoney(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        long amount;
        try {
            amount = AmountUtil.parse(StringArgumentType.getString(ctx, "amount"));
        } catch (NumberFormatException e) {
            ctx.getSource().sendError(Text.literal("Invalid amount. Try 1000, 1k, 1m, 2.5b..."));
            return 0;
        }
        if (amount < 0) {
            ctx.getSource().sendError(Text.literal("Amount can't be negative."));
            return 0;
        }
        MoneyManager.set(player.getUuid(), amount);
        sync(player);
        player.sendMessage(Text.literal("Money set to ")
                .append(Text.literal("$").formatted(Formatting.GREEN))
                .append(Text.literal(AmountUtil.format(amount))), false);
        return 1;
    }

    private int pay(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity from = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity to = EntityArgumentType.getPlayer(ctx, "player");

        long amount;
        try {
            amount = AmountUtil.parse(StringArgumentType.getString(ctx, "amount"));
        } catch (NumberFormatException e) {
            ctx.getSource().sendError(Text.literal("Invalid amount. Try 1000, 1k, 1m, 2.5b..."));
            return 0;
        }
        if (amount <= 0) {
            ctx.getSource().sendError(Text.literal("Amount must be more than 0."));
            return 0;
        }
        if (from.getUuid().equals(to.getUuid())) {
            ctx.getSource().sendError(Text.literal("You can't pay yourself."));
            return 0;
        }
        long fromBal = MoneyManager.get(from.getUuid());
        if (fromBal < amount) {
            ctx.getSource().sendError(Text.literal("You don't have enough money."));
            return 0;
        }
        long toBal = MoneyManager.get(to.getUuid());
        long newTo;
        try {
            newTo = Math.addExact(toBal, amount);
        } catch (ArithmeticException e) {
            ctx.getSource().sendError(Text.literal("That player can't hold that much money."));
            return 0;
        }

        MoneyManager.set(from.getUuid(), fromBal - amount);
        MoneyManager.set(to.getUuid(), newTo);
        sync(from);
        sync(to);

        String shown = AmountUtil.format(amount);

        // "You paid <name> $1M"
        from.sendMessage(Text.literal("You paid ")
                .append(Text.literal(to.getName().getString()).formatted(Formatting.YELLOW))
                .append(Text.literal(" "))
                .append(Text.literal("$").formatted(Formatting.GREEN))
                .append(Text.literal(shown)), false);
        from.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.5f);

        // receiver gets a message + ding too
        to.sendMessage(Text.literal(from.getName().getString()).formatted(Formatting.YELLOW)
                .append(Text.literal(" paid you "))
                .append(Text.literal("$").formatted(Formatting.GREEN))
                .append(Text.literal(shown)), false);
        to.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.5f);
        return 1;
    }

    private static void sync(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new BalancePayload(MoneyManager.get(player.getUuid())));
    }
}
