package gov.nasa.jpl.aerie.contrib.streamline.modeling.time_systems;

import gov.nasa.jpl.aerie.contrib.streamline.utils.InvertibleFunction;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: See if there's a more type-safe way to do this with an interface and a wrapped class
//   which could capture it's parent type, perhaps?

public class TimeSystem_v1<T> {
    private final String name;
    private final TimeSystem_v1<?> parent;
    private final InvertibleFunction<T, ?> upConversion;

    private TimeSystem_v1(String name, TimeSystem_v1<?> parent, InvertibleFunction<T, ?> upConversion) {
        this.name = name;
        this.parent = parent;
        this.upConversion = upConversion;
    }

    public <S> Function<T, S> conversionTo(TimeSystem_v1<S> otherSystem) {
        // TODO: this could be done more efficiently, or at least memoized
        Set<TimeSystem_v1<?>> ancestors = ancestors().collect(Collectors.toSet());
        TimeSystem_v1<?> lca = otherSystem.ancestors().filter(ancestors::contains).findFirst().orElseThrow(
                () -> new IllegalArgumentException("Time systems %s and %s are incompatible.".formatted(this, otherSystem))
        );
        Function thisToLCA = ancestors(lca).reduce(Function.identity(), (f, ts) -> f.andThen((Function)ts.upConversion), Function::andThen);
        Function lcaToOther = otherSystem.ancestors(lca).toList().reversed().stream().reduce(Function.identity(), (f, ts) -> f.andThen((Function)ts.upConversion.inverse()), Function::andThen);
        return thisToLCA.andThen(lcaToOther);
        // TODO: finish with a similar reduction on the reverse of otherSystem's ancestors
    }

    private Stream<TimeSystem_v1<?>> ancestors() {
        return Stream.iterate(this, ts -> ts.parent != null, ts -> ts.parent);
    }

    private Stream<TimeSystem_v1<?>> ancestors(TimeSystem_v1<?> stopAt) {
        return Stream.iterate(this, ts -> ts.parent != null && ts != stopAt, ts -> ts.parent);
    }

    public static <T> TimeSystem_v1<T> defineRoot(String name) {
        return new TimeSystem_v1<>(name, null, null);
    }

    @Override
    public String toString() {
        return name;
    }
}
