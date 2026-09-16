package com.example.crystaldelay;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Queue;

public class CrystalDelayMod implements ClientModInitializer {
    public static final String MOD_ID = "crystaldelay";

    private record DelayedPacket(Packet<?> packet, int executionTick) {}
    private static final Queue<DelayedPacket> QUEUE = new ArrayDeque<>();
    private static int currentTick = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            currentTick++;
            while (!QUEUE.isEmpty() && QUEUE.peek().executionTick <= currentTick) {
                DelayedPacket item = QUEUE.poll();
                if (item != null && client.getNetworkHandler() != null) {
                    client.getNetworkHandler().getConnection().send(item.packet(), null);
                }
            }
        });
    }

    public static void queuePacket(Packet<?> packet, int delay) {
        QUEUE.add(new DelayedPacket(packet, currentTick + delay));
    }

    @Mixin(PlayerInteractEntityC2SPacket.class)
    public interface PacketAccessor {
        @Accessor("entityId")
        int getEntityId();
    }

    @Mixin(ClientConnection.class)
    public static class ConnectionMixin {
        @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
        private void onSend(Packet<?> packet, CallbackInfo ci) {
            if (packet instanceof PlayerInteractEntityC2SPacket interactPacket) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world == null) return;

                int targetId = ((PacketAccessor) interactPacket).getEntityId();
                Entity target = client.world.getEntityById(targetId);

                if (target instanceof EndCrystalEntity) {
                    ci.cancel();
                    CrystalDelayMod.queuePacket(packet, 1);
                }
            }
        }
    }
}
