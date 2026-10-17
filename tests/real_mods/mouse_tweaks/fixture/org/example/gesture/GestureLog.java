package org.example.gesture;

public class GestureLog {
    public static String last;
    public static void info(String value) { last = value; }
    public static void debug(String value) { last = value; }
}
