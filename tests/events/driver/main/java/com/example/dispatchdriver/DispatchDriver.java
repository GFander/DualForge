package com.example.dispatchdriver;

import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Native test driver: only public Event construction and the real global post API. */
@Mod("dispatchdriver")
public final class DispatchDriver {
    private final AtomicBoolean posted = new AtomicBoolean();
    public DispatchDriver() { NeoForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void onStarted(ServerStartedEvent started) throws ReflectiveOperationException {
        if (!posted.compareAndSet(false, true)) throw new IllegalStateException("Duplicate ServerStartedEvent");
        String module = LoadingModList.get().getModFileById("dispatchprobe").getFile().getSecureJar().name();
        var layer = FMLLoader.getGameLayer();
        var loader = layer.findLoader(module);
        Class<?> type = Class.forName("com.example.dispatchcheck.DispatchEvent", true, loader);
        if (type.getClassLoader() != loader || !type.getModule().getName().equals(module)) throw new IllegalStateException("Wrong event origin");
        Event event = (Event) type.getConstructor().newInstance();
        System.out.println("[DF004] DRIVER READY phase=ServerStartedEvent module=" + module + " event=" + type.getName());
        String runId = UUID.randomUUID().toString();
        System.out.println("[DF004] DRIVER RUN=" + runId);
        PrintStream original = System.out;
        // Scheme C observes the real call stack at the unchanged println point.
        PrintStream observer = new PrintStream(original) {
            @Override public void println(String text) {
                var frames = StackWalker.getInstance(StackWalker.Option.SHOW_HIDDEN_FRAMES).walk(s -> s.skip(1).toList());
                original.println(text);
                var top = frames.getFirst();
                if (top.getClassName().equals("com.example.dispatchcheck.DispatchProbe") && top.getMethodName().equals("onDispatch")) {
                    original.println("[DF004] CALLBACK STACK RUN=" + runId + " TOP=" + top.getClassName() + "." + top.getMethodName());
                    for (var frame : frames) original.println("[DF004] FRAME " + frame.getClassName() + "." + frame.getMethodName());
                }
            }
        };
        System.out.println("[DF004] POST BEGIN");
        System.setOut(observer);
        try {
            if (NeoForge.EVENT_BUS.post(event) != event) throw new IllegalStateException("Wrong post return");
        } finally {
            System.setOut(original);
        }
        System.out.println("[DF004] POST RETURNED");
    }
}
