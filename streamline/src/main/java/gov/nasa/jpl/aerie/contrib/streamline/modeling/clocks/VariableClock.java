package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Map;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.$int;
import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.duration;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public record VariableClock(Duration extract, int multiplier) implements Dynamics<Duration, VariableClock> {
  @Override
  public VariableClock step(final Duration t) {
    return new VariableClock(extract.plus(t.times(multiplier)), multiplier);
  }

  public static VariableClock runningStopwatch() {
    return runningStopwatch(ZERO);
  }

  public static VariableClock runningStopwatch(Duration time) {
    return new VariableClock(time, 1);
  }

  public static VariableClock pausedStopwatch() {
    return pausedStopwatch(ZERO);
  }

  public static VariableClock pausedStopwatch(Duration time) {
    return new VariableClock(time, 0);
  }

  public static VariableClock runningTimer(Duration timeRemaining) {
    return new VariableClock(timeRemaining, -1);
  }

  public static VariableClock pausedTimer(Duration timeRemaining) {
    // Identical to pausedStopwatch (could be condensed to just "paused", perhaps?)
    return new VariableClock(timeRemaining, 0);
  }

  public static ValueMapper<VariableClock> valueMapper() {
    return new ValueMapper<VariableClock>() {
      @Override
      public ValueSchema getValueSchema() {
        return ValueSchema.ofStruct(Map.of(
                "time", ValueSchema.DURATION,
                "multiplier", ValueSchema.INT
        ));
      }

      @Override
      public Result<VariableClock, String> deserializeValue(SerializedValue serializedValue) {
        try {
          var serializedMap = serializedValue.asMap().orElseThrow();
          var timeResult = duration().deserializeValue(serializedMap.get("time")).getSuccessOrThrow();
          var multiplierResult = $int().deserializeValue(serializedMap.get("multiplier")).getSuccessOrThrow();
          return Result.success(new VariableClock(timeResult, multiplierResult));
        } catch (Throwable e) {
          return Result.failure("Failed to deserialize VariableClock: " + e.getMessage());
        }
      }

      @Override
      public SerializedValue serializeValue(VariableClock value) {
        return null;
      }
    };
  }
}
