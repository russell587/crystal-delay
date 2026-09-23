package com.example.client.mixin;

import com.example.client.CrystalDelayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Applies a wall-clock delay to every second end-crystal placement. */
@Mixin(MultiPlayerGameMode.class)
public class CrystalPlacementMixin {
    private static final ScheduledExecutorService DELAY_EXECUTOR = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "crystal-delay");
                thread.setDaemon(true);
                return thread;
            });

    private static int crystalAttempts;
    private static boolean replayingDelayedPlacement;

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void delayEverySecondCrystal(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (replayingDelayedPlacement || !player.getItemInHand(hand).is(Items.END_CRYSTAL)) {
            return;
        }

        crystalAttempts++;
        if ((crystalAttempts & 1) == 1) {
            return;
        }

        MultiPlayerGameMode interactionManager = (MultiPlayerGameMode) (Object) this;
        cir.setReturnValue(InteractionResult.SUCCESS);
        DELAY_EXECUTOR.schedule(() -> Minecraft.getInstance().execute(() -> {
            replayingDelayedPlacement = true;
            try {
                interactionManager.useItemOn(player, hand, hitResult);
            } catch (RuntimeException exception) {
                CrystalDelayMod.LOGGER.warn("Could not replay delayed crystal placement", exception);
            } finally {
                replayingDelayedPlacement = false;
            }
        }), CrystalDelayMod.SECOND_CRYSTAL_DELAY_MS, TimeUnit.MILLISECONDS);
    }
}
