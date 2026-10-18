package io.github.gfander.dualforge;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarFile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.IModInfo;

/** Native API + explicitly private state-core diagnostics. Public FML lifecycle remains NOT_RUN. */
public final class NeoBridgeContract {
    private static int refusals;
    public static void main(String[] args) throws Exception {
        Path fixture = Path.of(args[0]); var s = NativeApiHarness.load("neo", fixture, Path.of(args[1]));
        Map<String,byte[]> raw = new TreeMap<>();
        try (var jar = new JarFile(fixture.toFile())) { for (var e : jar.stream().filter(e -> e.getName().endsWith(".class")).toList())
            raw.put(e.getName().substring(0, e.getName().length() - 6), jar.getInputStream(e).readAllBytes()); }
        ForgeClientEventPlan plan;
        try (var host = new JarFile(args[2])) {
            plan = ForgeClientEventPlan.validate("gestureinput", "gestureinput", "org/example/gesture/ClientInput", raw, true, n -> {
                try { var e = host.getJarEntry(n + ".class"); return e == null ? null : host.getInputStream(e).readAllBytes(); }
                catch (Exception failure) { throw new IllegalStateException(failure); }
            });
        }
        var owner = new TestContainer(); var foreign = new TestContainer();
        Class<?> progressType = Class.forName(ForgeClientEventBridge.class.getName() + "$Progress");
        var ctor = progressType.getDeclaredConstructors()[0]; ctor.setAccessible(true); Object progress = ctor.newInstance(s.target(), plan);
        Field statesField = ForgeClientEventBridge.class.getDeclaredField("STATES"); statesField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<ModContainer,Object> states = (Map<ModContainer,Object>) statesField.get(null);
        states.put(owner, progress);
        Method register = ForgeClientEventBridge.class.getDeclaredMethod("registerOnBus", ModContainer.class, progressType, IEventBus.class); register.setAccessible(true);
        Method config = ForgeClientEventBridge.class.getDeclaredMethod("installConfig", ModContainer.class, progressType, Object.class, Class.class); config.setAccessible(true);
        Class<?> factoryType = Class.forName(ForgeClientEventPlan.FACTORY.replace('/', '.'));
        int[] calls = {0}; Function<Object,Object> function = parent -> { NativeApiHarness.check(parent == s.screenObject(), "original function parent identity"); calls[0]++; return parent; };
        Object factory = s.type().getMethod("df009c$factory", Function.class).invoke(null, function);
        NativeApiHarness.check(factoryType.isInstance(factory) && factory.getClass().isHidden(), "actual injected metafactory links native SAM");
        NativeApiHarness.check(calls[0] == 0, "factory creation does not create Screen");
        refuse(() -> config.invoke(null, owner, progress, factory, factoryType), "DUPLICATE_OR_UNORDERED_CONFIG");
        refuse(() -> register.invoke(null, foreign, progress, s.bus()), "DUPLICATE_OR_UNORDERED_REGISTRATION");
        register.invoke(null, owner, progress, s.bus());
        refuse(() -> register.invoke(null, owner, progress, s.bus()), "DUPLICATE_OR_UNORDERED_REGISTRATION");
        NativeApiHarness.exercise(s);
        config.invoke(null, owner, progress, factory, factoryType);
        NativeApiHarness.check(owner.getCustomExtension(cast(factoryType)).orElseThrow() == factory, "real ModContainer extension stores native factory");
        NativeApiHarness.check(calls[0] == 0, "extension registration and retrieval remain lazy");
        Object result = factoryType.getMethod("createScreen", ModContainer.class, s.screen()).invoke(factory, owner, s.screenObject());
        NativeApiHarness.check(result == s.screenObject() && calls[0] == 1, "native SAM reaches original Function once");
        factoryType.getMethod("createScreen", ModContainer.class, s.screen()).invoke(factory, foreign, s.screenObject());
        NativeApiHarness.check(calls[0] == 2, "new ModContainer argument not passed to original Screen function");
        refuse(() -> config.invoke(null, owner, progress, factory, factoryType), "DUPLICATE_OR_UNORDERED_CONFIG");
        refuse(() -> ForgeClientEventBridge.begin(s.target(), plan.fingerprint), "ENTRY_CONSTRUCTOR_ONLY");
        refuse(() -> ForgeClientEventBridge.register(s.target(), plan.fingerprint), "ENTRY_CONSTRUCTOR_ONLY");
        refuse(() -> ForgeClientEventBridge.configure(factory, plan.fingerprint), "ENTRY_CONSTRUCTOR_ONLY");
        states.clear();
        System.out.println("NEO CLIENT API CORE PASS assertions=" + NativeApiHarness.assertions + " refusals=" + refusals
            + "; PUBLIC_FML_LIFECYCLE=NOT_RUN; GUI=NOT_RUN; constructor-free Screen; diagnostic factory accessor");
    }
    @SuppressWarnings({"unchecked", "rawtypes"}) private static Class cast(Class<?> value) { return value; }
    private interface Action { void run() throws Exception; }
    private static void refuse(Action action, String fragment) throws Exception {
        try { action.run(); throw new AssertionError("Accepted " + fragment); }
        catch (InvocationTargetException e) { NativeApiHarness.check(e.getCause() instanceof IllegalStateException
            && e.getCause().getMessage().contains(fragment), "core refusal " + fragment); refusals++; }
        catch (IllegalStateException e) { NativeApiHarness.check(e.getMessage().contains(fragment), "public refusal " + fragment); refusals++; }
    }
    private static final class TestContainer extends ModContainer {
        TestContainer() { super((IModInfo) Proxy.newProxyInstance(IModInfo.class.getClassLoader(), new Class[]{IModInfo.class}, (p, m, a) ->
            m.getName().equals("getModId") ? "gestureinput" : null)); }
        @Override public IEventBus getEventBus() { return null; }
    }
}
