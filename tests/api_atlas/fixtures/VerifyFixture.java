package research.fixture;

/** Standalone verifier exercise. No registry construction or game lifecycle is invoked. */
public class VerifyFixture {
    public static void main(String[] args) throws Exception {
        for (String name : new String[]{"AtlasProbe", "Peer", "Untouched"}) {
            Class<?> type = Class.forName("research.fixture.".concat(name), false, VerifyFixture.class.getClassLoader());
            type.getDeclaredMethods();
            type.getDeclaredFields();
        }
        if (Peer.call() != null) throw new AssertionError("expected uninitialized receipt");
        System.out.println("OFFLINE_JVM_VERIFIER_PASS; registry lifecycle NOT RUN");
    }
}
