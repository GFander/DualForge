package org.example.publisher.api.events;

public interface IntEvent {
    int getValue();
    void setValue(int value);
    net.minecraft.world.entity.player.Player getPlayer();
}
