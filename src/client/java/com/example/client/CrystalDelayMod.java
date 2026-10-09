package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class CrystalDelayMod implements ClientModInitializer {
    public static final String MOD_ID = "crystaldelay";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final long TICK_DURATION_MS = 50L;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Crystal Delay mod initialized.");
    }

    public static long getDelayForNearbyDamagedPlayer(LocalPlayer player) {
        if (player == null || Minecraft.getInstance().level == null) {
            return 0L;
        }

        long delayMs = 0L;
        for (var other : Minecraft.getInstance().level.players()) {
            if (other == null || other == player) {
                continue;
            }

            if (!(other instanceof LivingEntity living)) {
                continue;
            }

            if (player.distanceToSqr(other) > 16.0D * 16.0D) {
                continue;
            }

            int hurtTime = living.getHurtTime();
            if (hurtTime <= 0) {
                continue;
            }

            long candidate = (long) hurtTime * TICK_DURATION_MS;
            if (candidate > delayMs) {
                delayMs = candidate;
            }
        }

        return delayMs;
    }
}
