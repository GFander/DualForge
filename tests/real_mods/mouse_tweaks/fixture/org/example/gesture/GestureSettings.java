package org.example.gesture;

public class GestureSettings {
    public final String path;
    public int reads;
    public GestureSettings(String path) { this.path = path; }
    public void read() { reads++; }
}
