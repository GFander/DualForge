package org.example.publisher.api.events;

public final class RepairEvent extends net.minecraftforge.eventbus.api.Event implements IntEvent {
    private final net.minecraft.world.entity.player.Player owner;
    private int amount;

    public RepairEvent(net.minecraft.world.entity.player.Player owner, int amount) {
        this.owner = owner;
        this.amount = amount;
    }
    public int getValue() { return amount; }
    public void setValue(int value) { amount = value; }
    public net.minecraft.world.entity.player.Player getPlayer() { return owner; }
}
