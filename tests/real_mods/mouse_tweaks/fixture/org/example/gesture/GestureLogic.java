package org.example.gesture;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class GestureLogic {
    public static GestureSettings settings;
    private static Minecraft client;
    private static boolean ready;
    public static final int[] calls = new int[4];
    public static final double[] data = new double[4];
    public static Object lastScreen;
    public static boolean cancel;
    public static void prepare() {
        GestureLog.info("prepare");
        if (ready) return;
        client = Minecraft.getInstance();
        settings = new GestureSettings(client.gameDirectory.getAbsolutePath() + File.separator + "config" + File.separator + "gesture.cfg");
        settings.read();
        GestureLog.info("ready");
        ready = true;
    }
    private static boolean record(int kind, Screen screen, double x, double y, double value) {
        calls[kind]++; lastScreen = screen; data[0] = x; data[1] = y; data[2] = value; return cancel;
    }
    public static boolean press(Screen s, double x, double y, GestureButton b) { return record(0, s, x, y, b.ordinal()); }
    public static boolean release(Screen s, double x, double y, GestureButton b) { return record(1, s, x, y, b.ordinal()); }
    public static boolean drag(Screen s, double x, double y, GestureButton b) { return record(2, s, x, y, b.ordinal()); }
    public static boolean scroll(Screen s, double x, double y, double delta) { return record(3, s, x, y, delta); }
}
