package com.example.contract;
import java.io.PrintStream;
public final class StackCheck {
    public static void main(String[] args) {
        PrintStream original = System.out;
        System.setOut(new PrintStream(original) {
            @Override public void println(String text) {
                var frames = StackWalker.getInstance().walk(s -> s.skip(1).toList());
                original.println(text);
                if (!frames.getFirst().getMethodName().equals("callback")) throw new AssertionError("Caller");
                original.println("PASS stack top=" + frames.getFirst().getClassName() + "." + frames.getFirst().getMethodName());
            }
        });
        try { callback(); } finally { System.setOut(original); }
    }
    private static void callback() { System.out.println("CONTRACT CALLBACK"); }
}
