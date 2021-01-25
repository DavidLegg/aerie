package gov.nasa.jpl.aerie.merlin.framework.resources.discrete;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.CompoundCondition;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.aerie.merlin.timeline.History;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public abstract class DiscreteResource<T> {
  private DiscreteResource() {}

  protected abstract T getDynamics(final CellGetter getter);

  private interface CellGetter {
    <CellType> CellType get(CellRef<?, CellType> ref);
  }


  public static <CellType, T>
  DiscreteResource<T> atom(final CellRef<?, CellType> ref, final Function<CellType, T> property) {
    Objects.requireNonNull(ref);
    Objects.requireNonNull(property);

    return new DiscreteResource<>() {
      @Override
      protected T getDynamics(final CellGetter getter) {
        return property.apply(getter.get(ref));
      }
    };
  }

  public static <T, S>
  DiscreteResource<S> mapped(final DiscreteResource<T> resource, final Function<T, S> transform) {
    Objects.requireNonNull(resource);
    Objects.requireNonNull(transform);

    return new DiscreteResource<>() {
      @Override
      protected S getDynamics(final CellGetter getter) {
        return transform.apply(resource.getDynamics(getter));
      }
    };
  }


  public final T getDynamics() {
    return this.getDynamics(new CellGetter() {
      @Override
      public <CellType> CellType get(final CellRef<?, CellType> ref) {
        return ref.get();
      }
    });
  }

  public final T getDynamicsAt(final History<?> now) {
    Objects.requireNonNull(now);

    return this.getDynamics(new CellGetter() {
      @Override
      public <CellType> CellType get(final CellRef<?, CellType> ref) {
        return ref.getAt(now);
      }
    });
  }


  public T ask() {
    return this.getDynamics();
  }

  public <S> DiscreteResource<S> map(final Function<T, S> transform) {
    return DiscreteResource.mapped(this, transform);
  }

  public CompoundCondition<?> isOneOf(final Set<T> values, final ValueMapper<T> mapper) {
    return CompoundCondition.atom(
        new DiscreteResourceSolver<>(mapper),
        this,
        Set.copyOf(values));
  }
}
