package org.example.publisher.platform;

public final class PublisherPort implements EventPort {
    public com.mojang.datafixers.util.Either<org.example.publisher.api.events.IntEvent, Integer>
            fireValueEvent(net.minecraft.world.entity.player.Player player, int value) {
        var event = new org.example.publisher.api.events.ValueEvent(player, value);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return com.mojang.datafixers.util.Either.left(event);
    }
    public com.mojang.datafixers.util.Either<org.example.publisher.api.events.IntEvent, Integer>
            fireRepairEvent(net.minecraft.world.entity.player.Player player, int value) {
        var event = new org.example.publisher.api.events.RepairEvent(player, value);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return com.mojang.datafixers.util.Either.left(event);
    }
}
