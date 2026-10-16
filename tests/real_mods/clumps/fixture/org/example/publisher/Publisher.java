package org.example.publisher;

@net.minecraftforge.fml.common.Mod("otherpublisher")
public final class Publisher {
    public Publisher() {
        Shared.callback = (player, orb) -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
            new net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp(player, orb));
    }
}
