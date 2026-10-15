package com.example.contextobserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod("neocontextobserver")
public class NeoContextObserver {
    public NeoContextObserver() {
        // An unowned native constructor cannot borrow another mod's Context.
        try {
            Class.forName("io.github.gfander.dualforge.ForgeConstructorContext").getMethod("get").invoke(null);
            throw new AssertionError("Native constructor acquired Forge Context");
        } catch (InvocationTargetException expected) {
            if (!(expected.getCause() instanceof IllegalStateException)
                    || !expected.getCause().getMessage().contains("Forge constructor context rejected"))
                throw new AssertionError(expected);
            System.out.println("[DF007C3] FOREIGN_CONTEXT REJECTED native-constructor");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
        NeoForge.EVENT_BUS.addListener(this::started);
    }

    private void started(ServerStartedEvent event) {
        try {
            Object previous = null;
            ModuleLayer layer = getClass().getModule().getLayer();
            for (String[] entry : new String[][]{{"contextprobe", "com.example.context.ContextProbe"}, {"contextpeer", "com.example.contextpeer.ContextPeer"}}) {
                var container = ModList.get().getModContainerById(entry[0]).orElseThrow();
                Module module = layer.findModule(container.getModInfo().getOwningFile().moduleName()).orElseThrow();
                Class<?> type = Class.forName(module, entry[1]);
                Object view = type.getField("capturedBus").get(null);
                if (view == null || Modifier.isFinal(type.getModifiers()) || type.getConstructors().length != 1
                        || type.getConstructors()[0].getParameterCount() != 0 || type.getModule().getLayer() != layer)
                    throw new AssertionError("Wrong original entry ABI " + entry[0]);
                // Read-only diagnostic inspection of the source-ABI view, not a Forge bus API claim.
                var delegate = view.getClass().getDeclaredField("delegate"); delegate.setAccessible(true);
                var owner = view.getClass().getDeclaredField("owner"); owner.setAccessible(true);
                Object bus = delegate.get(view);
                if (bus != container.getEventBus() || owner.get(view) != container || bus == previous || bus == NeoForge.EVENT_BUS)
                    throw new AssertionError("Wrong owned host Mod Bus " + entry[0]);
                previous = bus;
                System.out.println("[DF007C3] BUS_IDENTITY owner=" + entry[0] + " nativeDelegate=true global=false originalNoArg=true");
            }
            System.out.println("[DF007C3] CONTEXT PASS owners=2 distinct=true");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
