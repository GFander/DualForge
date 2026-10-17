package org.example.gesture;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ScreenEvent;

@Mod("gestureinput")
public class ClientInput {
    public ClientInput() {
        GestureLogic.prepare();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.registerConfigScreen(GestureScreen::new);
    }
    @SubscribeEvent
    public void pressed(ScreenEvent.MouseButtonPressed.Pre event) {
        GestureLog.debug("press " + event.getButton());
        GestureButton button = GestureButton.of(event.getButton());
        if (button != null) {
            if (GestureLogic.press(event.getScreen(), event.getMouseX(), event.getMouseY(), button)) event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public void released(ScreenEvent.MouseButtonReleased.Pre event) {
        GestureLog.debug("release " + event.getButton());
        GestureButton button = GestureButton.of(event.getButton());
        if (button != null) {
            if (GestureLogic.release(event.getScreen(), event.getMouseX(), event.getMouseY(), button)) event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public void dragged(ScreenEvent.MouseDragged.Pre event) {
        GestureLog.debug("drag " + event.getMouseButton() + " " + event.getDragX() + " " + event.getDragY());
        GestureButton button = GestureButton.of(event.getMouseButton());
        if (button != null) {
            if (GestureLogic.drag(event.getScreen(), event.getMouseX(), event.getMouseY(), button)) event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public void scrolled(ScreenEvent.MouseScrolled.Post event) {
        GestureLog.debug("wheel " + event.getDeltaY());
        GestureLogic.scroll(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getDeltaY());
    }
}
