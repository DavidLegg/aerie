package gov.nasa.jpl.aerie.contrib.streamline.modeling.piecewise;

import java.util.Objects;
import java.util.function.Supplier;

public class Lazy<T> {
    private Supplier<T> supplier;
    private T value;

    private Lazy(T value) {
        supplier = null;
        this.value = Objects.requireNonNull(value);
    }

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> Lazy<T> eager(T value) {
        return new Lazy<>(value);
    }

    public T get() {
        if (value == null) {
            value = Objects.requireNonNull(supplier.get());
            // Release the reference to supplier to reduce memory footprint.
            supplier = null;
        }
        return value;
    }
}
