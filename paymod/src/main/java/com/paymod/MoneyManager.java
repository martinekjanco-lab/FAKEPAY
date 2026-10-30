package com.paymod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoneyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Long> BALANCES = new HashMap<>();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("paymod_balances.json");
    }

    public static void load() {
        BALANCES.clear();
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            Map<String, Long> raw = GSON.fromJson(r, new TypeToken<Map<String, Long>>() {}.getType());
            if (raw != null) raw.forEach((k, v) -> BALANCES.put(UUID.fromString(k), v));
        } catch (Exception e) {
            PayMod.LOGGER.error("Could not load balances", e);
        }
    }

    public static void save() {
        Map<String, Long> raw = new HashMap<>();
        BALANCES.forEach((k, v) -> raw.put(k.toString(), v));
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(raw, w);
        } catch (Exception e) {
            PayMod.LOGGER.error("Could not save balances", e);
        }
    }

    public static long get(UUID id) {
        return BALANCES.getOrDefault(id, 0L);
    }

    public static void set(UUID id, long amount) {
        BALANCES.put(id, amount);
        save();
    }
}
