package gov.nasa.jpl.aerie.contrib.streamline.core.monads;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Monad transformer for {@link Resource}, M A -> Resource&lt;M A&gt;
 */
public final class ResourceMonadTransformer {
  private ResourceMonadTransformer() {}

  public static <A, DA> Resource<DA> unit(Function<A, Expiring<DA>> mUnit, A a) {
    return lift(mUnit.apply(a));
  }

  public static <A, DA, DB> Resource<DB> bind(
      BiFunction<Expiring<DA>, Function<A, Expiring<DB>>, Expiring<DB>> mBind,
      Resource<DA> ra,
      Function<A, Resource<DB>> f) {
    return () -> mBind.apply(ra.getDynamics(), a -> f.apply(a).getDynamics());
  }

  public static <DA> Resource<DA> lift(Expiring<DA> da) {
    return () -> da;
  }
}
