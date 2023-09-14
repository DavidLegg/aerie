package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.junit.MerlinExtension;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.*;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MINUTE;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Nested
@ExtendWith(MerlinExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class DiscreteEffectsTest {
  public DiscreteEffectsTest(final Registrar registrar) {
    Resources.init();
  }

  private final CellResource<Discrete<Integer>> settable = cellResource(discrete(42));

  @Test
  void set_effect_changes_to_new_value() {
    set(settable, 123);
    assertEquals(123, currentValue(settable));
  }

  @Test
  void conflicting_concurrent_set_effects_throw_exception() {
    spawn(() -> set(settable, 123));
    spawn(() -> set(settable, 456));
    delay(ZERO);
    assertThrows(UnsupportedOperationException.class, settable::getDynamics);
  }

  @Test
  void agreeing_concurrent_set_effects_set_new_value() {
    spawn(() -> set(settable, 789));
    spawn(() -> set(settable, 789));
    delay(ZERO);
    assertEquals(789, currentValue(settable));
  }

  private final CellResource<Discrete<Boolean>> flag = cellResource(discrete(false));

  @Test
  void flag_set_makes_value_true() {
    set(flag);
    assertTrue(currentValue(flag));
  }

  @Test
  void flag_unset_makes_value_false() {
    unset(flag);
    assertFalse(currentValue(flag));
  }

  @Test
  void flag_toggle_changes_value() {
    set(flag);
    toggle(flag);
    assertFalse(currentValue(flag));

    toggle(flag);
    assertTrue(currentValue(flag));
  }

  private final CellResource<Discrete<Integer>> counter = cellResource(discrete(0));

  @Test
  void increment_increases_value_by_1() {
    int initialValue = currentValue(counter);
    increment(counter);
    assertEquals(initialValue + 1, currentValue(counter));
  }

  @Test
  void increment_by_n_increases_value_by_n() {
    int initialValue = currentValue(counter);
    increment(counter, 3);
    assertEquals(initialValue + 3, currentValue(counter));
  }

  @Test
  void decrement_decreases_value_by_1() {
    int initialValue = currentValue(counter);
    decrement(counter);
    assertEquals(initialValue - 1, currentValue(counter));
  }

  @Test
  void decrement_by_n_decreases_value_by_n() {
    int initialValue = currentValue(counter);
    decrement(counter, 3);
    assertEquals(initialValue - 3, currentValue(counter));
  }

  private final CellResource<Discrete<Double>> consumable = cellResource(discrete(10.0));

  @Test
  void consume_decreases_value_by_amount() {
    double initialValue = currentValue(consumable);
    consume(consumable, 3.14);
    assertEquals(initialValue - 3.14, currentValue(consumable));
  }

  @Test
  void restore_increases_value_by_amount() {
    double initialValue = currentValue(consumable);
    restore(consumable, 3.14);
    assertEquals(initialValue + 3.14, currentValue(consumable));
  }

  @Test
  void consume_and_restore_effects_commute() {
    double initialValue = currentValue(consumable);
    spawn(() -> consume(consumable, 2.7));
    spawn(() -> restore(consumable, 5.6));
    delay(ZERO);
    assertEquals(initialValue - 2.7 + 5.6, currentValue(consumable));
  }

  private final CellResource<Discrete<Double>> nonconsumable = cellResource(discrete(10.0));

  @Test
  void using_decreases_value_while_action_is_running() {
    double initialValue = currentValue(nonconsumable);
    using(nonconsumable, 3.14, () -> {
      assertEquals(initialValue - 3.14, currentValue(nonconsumable));
    });
    assertEquals(initialValue, currentValue(nonconsumable));
  }

  @Test
  void using_runs_synchronously() {
    Duration start = currentTime();
    using(nonconsumable, 3.14, () -> {
      assertEquals(start, currentTime());
      delay(MINUTE);
    });
    assertEquals(start.plus(MINUTE), currentTime());
  }

  @Test
  void tasks_in_parallel_with_using_observe_decreased_value() {
    double initialValue = currentValue(nonconsumable);
    spawn(() -> using(nonconsumable, 3.14, () -> {
      delay(MINUTE);
    }));
    // Allow one tick for effects to be observable from child task
    delay(ZERO);
    assertEquals(initialValue - 3.14, currentValue(nonconsumable));
    delay(30, SECONDS);
    assertEquals(initialValue - 3.14, currentValue(nonconsumable));
    delay(30, SECONDS);
    // Allow one tick for effects to be observable from child task
    delay(ZERO);
    assertEquals(initialValue, currentValue(nonconsumable));
  }

  // TODO: Unit-aware effects - to be tested once the unit-aware framework itself is tested
}
