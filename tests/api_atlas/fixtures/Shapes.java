package research.scan;

import java.lang.annotation.*;
import java.util.*;

@Retention(RetentionPolicy.RUNTIME)
@interface Mark {
    Class<?> value();
    double number() default Double.POSITIVE_INFINITY;
}

@Mark(Map.Entry.class)
public sealed class Shapes<T extends Number> permits Shapes.Child {
    public static final float NEGATIVE_INFINITY = Float.NEGATIVE_INFINITY;
    protected Map<String, List<T[]>> generic;
    private String internal;
    public Number bridgeTarget() throws IllegalStateException { return null; }
    public static final class Child extends Shapes<Integer> { }
    public record Pair(@Mark(String.class) String value, int number) { }
}
