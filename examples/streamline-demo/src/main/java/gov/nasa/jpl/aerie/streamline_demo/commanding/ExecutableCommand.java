package gov.nasa.jpl.aerie.streamline_demo.commanding;

import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.when;

public interface ExecutableCommand {
    /**
     * Returns a condition, optionally considering the state of the SequenceEngine
     * and/or the rest of the model, which specifies when this command may be dispatched.
     * This method assumes this command is the next to be dispatched.
     */
    Condition readyForDispatch(SequenceEngine engine);

    // TODO - should this be the command activity instead?
    void run(SequenceEngine engine);


    static ExecutableCommand of(
            Function<SequenceEngine, Condition> readyForDispatch,
            Consumer<SequenceEngine> action) {
        return new ExecutableCommand() {

            @Override
            public Condition readyForDispatch(SequenceEngine engine) {
                return readyForDispatch.apply(engine);
            }

            @Override
            public void run(SequenceEngine engine) {
                action.accept(engine);
            }
        };
    }

    static Function<SequenceEngine, Condition> absoluteTime(Duration dispatchTime) {
        return engine -> (positive, atEarliest, atLatest) -> {
            var relativeDispatchTime = dispatchTime.minus(currentTime());
            if (positive) {
                return Optional.of(Duration.max(relativeDispatchTime))
                        .filter(atLatest::noShorterThan);
            } else {
                return Optional.of(atEarliest).filter(relativeDispatchTime::longerThan);
            }
        };
    }

    static Function<SequenceEngine, Condition> epochRelativeTime(Duration offsetFromEpoch) {
        return engine -> {
            var seq = currentValue(engine.loadedSequence);
            var epoch = seq.epoch().orElseThrow(() -> new IllegalStateException("Sequence does not have an epoch"));
            return absoluteTime(epoch.plus(offsetFromEpoch)).apply(engine);
        };
    }

    static Function<SequenceEngine, Condition> dispatchRelativeTime(Duration offsetFromLastDispatch) {
        return engine -> {
            var lastDispatchTime = currentValue(engine.lastDispatchTime);
            return absoluteTime(lastDispatchTime.plus(offsetFromLastDispatch)).apply(engine);
        };
    }

    static Function<SequenceEngine, Condition> commandComplete() {
        return engine -> when(engine.lastDispatchedCommandComplete);
    }
}
