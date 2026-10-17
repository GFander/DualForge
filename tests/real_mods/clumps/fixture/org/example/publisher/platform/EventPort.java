package org.example.publisher.platform;

public interface EventPort {
    com.mojang.datafixers.util.Either<org.example.publisher.api.events.IntEvent, Integer>
        fireValueEvent(net.minecraft.world.entity.player.Player player, int value);
    com.mojang.datafixers.util.Either<org.example.publisher.api.events.IntEvent, Integer>
        fireRepairEvent(net.minecraft.world.entity.player.Player player, int value);
}
