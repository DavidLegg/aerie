package gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.DynamicsEffect;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class UnitAwareResources {
  private UnitAwareResources() {}

  public static <D> UnitAware<Resource<D>> unitAware(Resource<D> resource, Unit unit, BiFunction<D, Double, D> scaling) {
    return UnitAware.unitAware(resource, unit, extend(scaling, ResourceMonad::map));
  }

  public static <D extends Dynamics<?, D>> UnitAware<CellResource<D>> unitAware(CellResource<D> resource, Unit unit, BiFunction<D, Double, D> scaling) {
    final BiFunction<Expiring<D>, Double, Expiring<D>> extendedScaling = extend(scaling, ExpiringMonad::map);
    return UnitAware.unitAware(resource, unit, (cellResource, scale) -> new CellResource<D>() {
      @Override
      public void emit(final DynamicsEffect<D> effect) {
        // Use an effect in the scaled domain by first scaling the dynamics,
        // then applying the effect, then de-scaling the result back
        cellResource.emit(
            unscaledDynamics -> extendedScaling.apply(effect.apply(
                extendedScaling.apply(unscaledDynamics, scale)), 1 / scale));
      }

      @Override
      public Expiring<D> getDynamics() {
        return extendedScaling.apply(cellResource.getDynamics(), scale);
      }
    });
  }

  public static <A, MA> BiFunction<MA, Double, MA> extend(BiFunction<A, Double, A> scaling, BiFunction<MA, Function<A, A>, MA> map) {
    return (ma, s) -> map.apply(ma, a -> scaling.apply(a, s));
  }

  public static <A> BiFunction<Resource<A>, Double, Resource<A>> extend(BiFunction<A, Double, A> scaling) {
    return extend(scaling, ResourceMonad::map);
  }
}
