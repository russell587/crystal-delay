package com.example.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stores the crystal delay in milliseconds in config/crystaldelay.json. */
public final class CrystalDelayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("crystaldelay.json");

    public static final int DEFAULT_DELAY_MS = 25;
    public static final int MIN_DELAY_MS = 0;
    public static final int MAX_DELAY_MS = 1000;

    private int delayMs = DEFAULT_DELAY_MS;

    private CrystalDelayConfig() {
    }

    public static CrystalDelayConfig load() {
        if (!Files.exists(PATH)) {
            CrystalDelayConfig config = new CrystalDelayConfig();
            config.save();
            return config;
        }

        try {
            CrystalDelayConfig config = GSON.fromJson(Files.readString(PATH), CrystalDelayConfig.class);
            if (config == null) {
                throw new IOException("Configuration is empty");
            }
            config.delayMs = clamp(config.delayMs);
            config.save();
            return config;
        } catch (Exception exception) {
            CrystalDelayMod.LOGGER.warn("Could not read {}, using defaults", PATH, exception);
            CrystalDelayConfig config = new CrystalDelayConfig();
            config.save();
            return config;
        }
    }

    public int getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(int delayMs) {
        this.delayMs = clamp(delayMs);
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException exception) {
            CrystalDelayMod.LOGGER.warn("Could not save {}", PATH, exception);
        }
    }

    private static int clamp(int delayMs) {
        return Math.max(MIN_DELAY_MS, Math.min(MAX_DELAY_MS, delayMs));
    }
}
