package org.example.gesture;

public enum GestureButton {
    PRIMARY, SECONDARY;
    public static GestureButton of(int value) {
        return switch (value) { case 0 -> PRIMARY; case 1 -> SECONDARY; default -> null; };
    }
}
