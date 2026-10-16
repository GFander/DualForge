package org.example.publisher;

public final class Shared {
    public static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("independent-publisher");
    public static java.util.function.BiPredicate<net.minecraft.world.entity.player.Player,
        net.minecraft.world.entity.ExperienceOrb> callback = (player, orb) -> false;
}
