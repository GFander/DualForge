import java.lang.reflect.Method;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EmptyRegistrationFixture {
    public static final class BareTarget { }
    public static void main(String[] args) {
        BareTarget target = new BareTarget();
        int customPublic = 0;
        int subscribers = 0;
        for (Method method : target.getClass().getMethods()) {
            if (method.getDeclaringClass() != Object.class) customPublic++;
            if (method.isAnnotationPresent(SubscribeEvent.class)) subscribers++;
        }
        System.out.println("DECLARED_METHODS=" + target.getClass().getDeclaredMethods().length);
        System.out.println("CUSTOM_PUBLIC_METHODS=" + customPublic + " SUBSCRIBERS=" + subscribers);
        IEventBus bus = BusBuilder.builder().startShutdown().useModLauncher().build();
        System.out.println("BUS_CLASS=" + bus.getClass().getName());
        System.out.println("BEFORE_REGISTER");
        bus.register(target);
        System.out.println("AFTER_REGISTER");
    }
}
