package com.paymod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BalancePayload(long balance) implements CustomPayload {
    public static final Id<BalancePayload> ID = new Id<>(Identifier.of("paymod", "balance"));
    public static final PacketCodec<RegistryByteBuf, BalancePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_LONG, BalancePayload::balance, BalancePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
