package org.example.gesture;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GestureScreen extends Screen {
    private final Screen parent;
    public GestureScreen(Screen parent) {
        super(Component.literal("Gesture settings"));
        this.parent = parent;
    }
}
