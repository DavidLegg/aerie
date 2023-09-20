package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.Cell;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.commutingEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.noncommutingEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.unit;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static org.junit.jupiter.api.Assertions.*;

class CellRefV2Test {
  @Nested
  @ExtendWith(MerlinExtension.class)
  @TestInstance(Lifecycle.PER_CLASS)
  class NonCommutingEffects {
    private final CellRef<DynamicsEffect<Discrete<Integer>>, Cell<Discrete<Integer>>> cell =
        allocate(unit(42), noncommutingEffects());

    @Test
    void gets_initial_value_if_no_effects_are_emitted() {
      assertEquals(42, discreteCellValue(cell));
    }

    @Test
    void applies_singleton_effect() {
      int initialValue = discreteCellValue(cell);
      cell.emit(effect(n -> 3 * n));
      assertEquals(3 * initialValue, discreteCellValue(cell));
    }

    @Test
    void applies_sequential_effects_in_order() {
      int initialValue = discreteCellValue(cell);
      cell.emit(effect(n -> 3 * n));
      cell.emit(effect(n -> n + 1));
      assertEquals(3 * initialValue + 1, discreteCellValue(cell));
    }

    @Test
    void throws_exception_when_concurrent_effects_are_applied() {
      spawn(() -> cell.emit(effect(n -> 3 * n)));
      spawn(() -> cell.emit(effect(n -> 3 * n)));
      delay(ZERO);
      assertInstanceOf(ErrorCatching.Failure.class, cell.get().dynamics);
    }
  }

  @Nested
  @ExtendWith(MerlinExtension.class)
  @TestInstance(Lifecycle.PER_CLASS)
  class CommutingEffects {
    private final CellRef<DynamicsEffect<Discrete<Integer>>, Cell<Discrete<Integer>>> cell =
        allocate(unit(42), commutingEffects());

    @Test
    void gets_initial_value_if_no_effects_are_emitted() {
      assertEquals(42, discreteCellValue(cell));
    }

    @Test
    void applies_singleton_effect() {
      int initialValue = discreteCellValue(cell);
      cell.emit(effect(n -> 3 * n));
      assertEquals(3 * initialValue, discreteCellValue(cell));
    }

    @Test
    void applies_sequential_effects_in_order() {
      int initialValue = discreteCellValue(cell);
      cell.emit(effect(n -> 3 * n));
      cell.emit(effect(n -> n + 1));
      assertEquals(3 * initialValue + 1, discreteCellValue(cell));
    }

    @Test
    void applies_concurrent_effects_in_any_order() {
      int initialValue = discreteCellValue(cell);
      // These effects do not in fact commute,
      // but the point of the commutingEffects is that it *doesn't* check.
      spawn(() -> cell.emit(effect(n -> 3 * n)));
      spawn(() -> cell.emit(effect(n -> n + 1)));
      delay(ZERO);
      int result = discreteCellValue(cell);
      assertTrue(result == 3*initialValue + 1 || result == 3 * (initialValue + 1));
    }
  }

  @Nested
  @ExtendWith(MerlinExtension.class)
  @TestInstance(Lifecycle.PER_CLASS)
  class AutoEffects {
    private final CellRef<DynamicsEffect<Discrete<Integer>>, Cell<Discrete<Integer>>> cell =
        allocate(unit(42), autoEffects());

    @Test
    void gets_initial_value_if_no_effects_are_emitted() {
      assertEquals(42, discreteCellValue(cell));
    }

    @Test
    void applies_singleton_effect() {
      int initialValue = discreteCellValue(cell);
      cell.emit(effect(n -> 3 * n));
      assertEquals(3 * initialValue, discreteCellValue(cell));
    }

    @Test
    void applies_sequential_effects_in_order() {
      int initialValue = discreteCellValue(cell);
      cell.emit(effect(n -> 3 * n));
      cell.emit(effect(n -> n + 1));
      assertEquals(3 * initialValue + 1, discreteCellValue(cell));
    }

    @Test
    void applies_commuting_concurrent_effects() {
      int initialValue = discreteCellValue(cell);
      // These effects do not in fact commute,
      // but the point of the commutingEffects is that it *doesn't* check.
      spawn(() -> cell.emit(effect(n -> 3 * n)));
      spawn(() -> cell.emit(effect(n -> 4 * n)));
      delay(ZERO);
      int result = discreteCellValue(cell);
      assertEquals(12 * initialValue, result);
    }

    @Test
    void throws_exception_when_non_commuting_concurrent_effects_are_applied() {
      spawn(() -> cell.emit(effect(n -> 3 * n)));
      spawn(() -> cell.emit(effect(n -> n + 1)));
      delay(ZERO);
      assertInstanceOf(ErrorCatching.Failure.class, cell.get().dynamics);
    }
  }

  private static <T> T discreteCellValue(CellRef<DynamicsEffect<Discrete<T>>, Cell<Discrete<T>>> cell) {
    return cell.get().dynamics.getOrThrow().data().extract();
  }
}
