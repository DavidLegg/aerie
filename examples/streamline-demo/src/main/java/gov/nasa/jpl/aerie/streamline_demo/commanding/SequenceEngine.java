package gov.nasa.jpl.aerie.streamline_demo.commanding;

import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

public class SequenceEngine {

    // TODO: Protect these states by making them private with read-only Resource, not MutableResource, accessors.
    public final MutableResource<Discrete<ExecutableSequence>> loadedSequence = discreteResource(null);

    public final MutableResource<Discrete<ExecutableCommand>> lastDispatchedCommand = discreteResource(null);
    public final MutableResource<Discrete<Duration>> lastDispatchTime = discreteResource(null);
    public final MutableResource<Discrete<Boolean>> lastDispatchedCommandComplete = discreteResource(false);

    public final MutableResource<Discrete<Integer>> nextCommandIndex = discreteResource(0);
    public final MutableResource<Discrete<Boolean>> active = discreteResource(false);

    public final Resource<Discrete<Boolean>> isLoaded = map(loadedSequence, Objects::nonNull);
    private final Resource<Discrete<ExecutableCommand>> nextCommand =
            map(loadedSequence, nextCommandIndex, (seq, i) -> seq.commands().get(i));
    // Pulling "active" in through a constant resource is a small performance optimization -
    // We generate the condition for active itself only once, then re-use it a lot.
    private final Resource<Discrete<Condition>> nextDispatchCondition =
            map(constant(when(active)), nextCommand, ($active, cmd) -> $active.and(cmd.readyForDispatch(this)));

    public void load(ExecutableSequence sequence) {
        if (currentValue(isLoaded, false)) {
            throw new IllegalStateException("Sequence is already loaded");
        }
        // Force an unload before a load to reset everything.
        unload();
        set(loadedSequence, sequence);
    }

    public void unload() {
        set(loadedSequence, null);
        set(nextCommandIndex, 0);
        set(active, false);
        set(lastDispatchTime, null);
        set(lastDispatchedCommand, null);
        set(lastDispatchedCommandComplete, false);
    }

    public void execute() {
        while (currentValue(isLoaded, false)) {
            waitUntil(readyToDispatch());
            dispatch();
        }
    }

    public void activate(ExecutableSequence sequence) {
        load(sequence);
        execute();
    }

    public void pause() {
        turnOff(active);
    }

    public void resume() {
        if (!currentValue(isLoaded, false)) {
            throw new IllegalStateException("No sequence is loaded");
        }
        turnOn(active);
    }

    private Condition readyToDispatch() {
        // Do the lookup for which command's dispatch condition to reference
        // from within the readyToDispatch() condition itself.
        // That respects control-flow commands which modify nextCommandIndex.
        // Further, use Condition.TRUE when we hit an error or finish this sequence to immediately unload.
        return (positive, atEarliest, atLatest) ->
                currentValue(nextDispatchCondition, Condition.TRUE).nextSatisfied(positive, atEarliest, atLatest);
    }

    private void dispatch() {
        var cmd = currentValue(nextCommand, null);
        if (cmd == null) {
            // Error, or we've finished the sequence (hence "get"ting the nth element of a length-n command list fails.)
            unload();
        } else {
            // Spawn the command asynchronously, because some timing types don't wait for one command before dispatching another.
            spawn(replaying(() -> {
                call(() -> cmd.run(this));
                // Because command dispatches can overlap,
                // only enable the "lastDispatchedCommandComplete" flag if
                // this is still the last dispatched command when we finish.
                if (currentValue(lastDispatchedCommand, null) == cmd) {
                    turnOn(lastDispatchedCommandComplete);
                }
            }));
            // Do some bookkeeping about the command we just dispatched.
            set(lastDispatchedCommand, cmd);
            set(lastDispatchTime, currentTime());
            turnOff(lastDispatchedCommandComplete);
            // By default, advance to the next command
            // Control-flow modifying commands can then modify this state to affect future commands.
            increment(nextCommandIndex);
        }
    }
}
