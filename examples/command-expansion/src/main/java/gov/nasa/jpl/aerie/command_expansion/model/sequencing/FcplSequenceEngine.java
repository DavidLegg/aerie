package gov.nasa.jpl.aerie.command_expansion.model.sequencing;

import gov.nasa.jpl.aerie.command_expansion.command_activities.Command;
import gov.nasa.jpl.aerie.command_expansion.expansion.Sequence;
import gov.nasa.jpl.aerie.command_expansion.expansion.TimedCommand;
import gov.nasa.jpl.aerie.command_expansion.model.Mission;
import gov.nasa.jpl.aerie.command_expansion.util.TimeCondition;
import gov.nasa.jpl.aerie.contrib.streamline.core.MutableResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers.*;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentTime;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging.LOGGER;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteResources.*;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.map;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.*;

// I want to emphasize that this model is mission-specific.
// Missions will likely copy one from a prior similar mission, and/or we can provide "standard" models
// for missions using VML or FCPL, but this is modifiable independent of the rest of "sequencing".
public class FcplSequenceEngine {
    public final String engineId;
    private final Mission mission;

    private final MutableResource<Discrete<Boolean>> active;
    private final MutableResource<Discrete<Optional<Instant>>> epoch;
    private final MutableResource<Discrete<Optional<Sequence>>> loadedSequence;
    private final Resource<Discrete<Boolean>> isLoaded;
    private final Resource<Discrete<String>> loadedSequenceId;
    private final MutableResource<Discrete<Integer>> nextCommandIndex;
    private final Resource<Discrete<Optional<TimedCommand>>> nextCommand;
    private final Resource<Discrete<String>> nextCommandStem;

    private final MutableResource<Discrete<Integer>> lastDispatchedCommandIndex;
    private final Resource<Discrete<Optional<TimedCommand>>> lastDispatchedCommand;
    private final Resource<Discrete<String>> lastDispatchedCommandStem;

    private final MutableResource<Discrete<Integer>> sequenceLoadCounter;
    private final MutableResource<Discrete<Integer>> commandDispatchCounter;
    private final MutableResource<Discrete<Boolean>> currentCommandComplete;
    private final MutableResource<Discrete<Duration>> timeOfLastDispatch;
    private final Condition readyToDispatch;

    public FcplSequenceEngine(String engineId, Registrar registrar, Mission mission) {
        this.engineId = engineId;
        this.mission = mission;

        active = discreteResource(false);
        epoch = discreteResource(Optional.empty());
        loadedSequence = discreteResource(Optional.empty());
        isLoaded = map(loadedSequence, Optional::isPresent);
        loadedSequenceId = map(loadedSequence, seq -> seq.map(Sequence::seqId).orElse(""));
        nextCommandIndex = discreteResource(0);
        nextCommand = map(loadedSequence, nextCommandIndex, (seq, i) ->
                seq.map($ -> 0 <= i && i < $.commands().size() ? $.commands().get(i) : null));
        nextCommandStem = map(nextCommand, $ -> $.map(tc -> tc.command().stem()).orElse(""));

        lastDispatchedCommandIndex = discreteResource(-1);
        lastDispatchedCommand = map(loadedSequence, lastDispatchedCommandIndex, (seq, i) ->
                seq.map($ -> 0 <= i && i < $.commands().size() ? $.commands().get(i) : null));
        lastDispatchedCommandStem = map(lastDispatchedCommand, $ -> $.map(tc -> tc.command().stem()).orElse(""));

        sequenceLoadCounter = discreteResource(0);
        commandDispatchCounter = discreteResource(0);
        currentCommandComplete = discreteResource(false);
        timeOfLastDispatch = discreteResource(Duration.ZERO);

        // TODO - nextCommandIsReady is written kind of messily, from a functional perspective.
        // The "proper" way to write this would use either a map with all the resource arguments,
        // or would bind the necessary resources for each branch...
        // We can get away with this for now because none of the resources involved should ever expire nor error.
        Resource<Discrete<Condition>> nextCommandIsReady = map(nextCommand, $ -> $.map(timedCommand -> switch (timedCommand.timeTag()) {
            case TimedCommand.AbsoluteTimeTag absoluteTimeTag ->
                    TimeCondition.in(Durations.between(currentValue(mission.clock), absoluteTimeTag.time()));
            case TimedCommand.CommandCompleteTimeTag commandCompleteTimeTag ->
                    when(currentCommandComplete);
            case TimedCommand.EpochRelativeTimeTag epochRelativeTimeTag -> {
                var resolvedEpoch = currentValue(epoch).orElseThrow(() ->
                        new IllegalStateException("Epoch relative commanding used without a loaded epoch on engine " + engineId));
                var absoluteTargetTime = Duration.addToInstant(resolvedEpoch, epochRelativeTimeTag.offset());
                yield TimeCondition.in(Durations.between(currentValue(mission.clock), absoluteTargetTime));
            }
            case TimedCommand.RelativeTimeTag relativeTimeTag ->
                    TimeCondition.at(currentValue(timeOfLastDispatch).plus(relativeTimeTag.offset()));
        })
                // When there is no next command, because either the sequence was unloaded or nextCommandIndex is not legal,
                // return TRUE to cycle the engine immediately. This terminates a finished sequence immediately.
                .orElse(Condition.TRUE));

        // Build a condition which reads nextCommandIsReady
        // That way, this condition reacts to any change in state that feeds into this condition.
        readyToDispatch = when(active).and((positive, atEarliest, atLatest) ->
                currentValue(nextCommandIsReady).nextSatisfied(positive, atEarliest, atLatest));

        String prefix = "sequencing.engine_" + engineId + ".";
        registrar.discrete(prefix + "isActive", active, $boolean());
        registrar.discrete(prefix + "isLoaded", isLoaded, $boolean());
        registrar.discrete(prefix + "loadedSequence", loadedSequenceId, string());
        registrar.discrete(prefix + "nextCommandIndex", nextCommandIndex, $int());
        registrar.discrete(prefix + "nextCommand", nextCommandStem, string());
        registrar.discrete(prefix + "lastDispatchedCommandIndex", lastDispatchedCommandIndex, $int());
        registrar.discrete(prefix + "lastDispatchedCommand", lastDispatchedCommandStem, string());
    }

    public void unload() {
        turnOff(active);
        set(epoch, Optional.empty());
        set(loadedSequence, Optional.empty());
        set(nextCommandIndex, 0);

        turnOff(currentCommandComplete);
        increment(sequenceLoadCounter);
        set(lastDispatchedCommandIndex, -1);
    }

    public void load(Sequence sequence) {
        if (currentValue(isLoaded)) {
            LOGGER.error("Cannot load sequence. Engine %d is already loaded.", engineId);
            return;
        }

        unload();
        set(loadedSequence, Optional.of(sequence));
        increment(sequenceLoadCounter);
    }

    /**
     * Synchronously run the currently-loaded sequence.
     * Spawned command activities will appear as children of the current task.
     * <p>
     *     Method returns when sequence finishes executing.
     * </p>
     */
    public void execute() {
        if (!currentValue(isLoaded)) {
            LOGGER.error("Cannot execute sequence. No sequence is loaded in engine %d.", engineId);
            return;
        }

        if (currentValue(active)) {
            LOGGER.warning("Cannot execute sequence. Engine %d is already active.", engineId);
            return;
        }

        turnOn(active);
        // For the purposes of relative-timed dispatch, sequence execution counts as the first time of last dispatch.
        set(timeOfLastDispatch, currentTime());
        int sequenceLoadNumber = currentValue(sequenceLoadCounter);
        // Iteratively dispatch commands until either
        while (currentValue(sequenceLoadCounter) == sequenceLoadNumber) {
            waitUntil(readyToDispatch);
            // Check that the sequence hasn't changed before we dispatch, to avoid possible double-dispatch issues
            // when quickly switching from one sequence to another.
            if (currentValue(sequenceLoadCounter) == sequenceLoadNumber) {
                dispatchNextCommand();
            }
        }
    }

    public void pause() {
        turnOff(active);
    }

    private void dispatchNextCommand() {
        currentValue(nextCommand).ifPresentOrElse(
                // Dispatch is done within a spawn to support overlapping commanding
                timedCommand -> spawn(replaying(() -> {
                    set(lastDispatchedCommandIndex, currentValue(nextCommandIndex));
                    // By default, the next command to dispatch is just the command after this one.
                    increment(nextCommandIndex);
                    // Use the command dispatch counter to detect overlapping command execution,
                    // and avoid earlier-dispatched commands from inadvertently marking later commands as complete.
                    increment(commandDispatchCounter);
                    int commandDispatchNumber = currentValue(commandDispatchCounter);
                    set(timeOfLastDispatch, currentTime());
                    // Run the command itself through a call, not a spawn, so we know when it finishes.
                    timedCommand.command().call(mission);
                    // Check that we haven't dispatched another command before we set the command complete flag.
                    if (currentValue(commandDispatchCounter) == commandDispatchNumber) {
                        turnOn(currentCommandComplete);
                    }
                })),
                // When there is no command left to dispatch, just unload the sequence instead.
                this::unload);
    }

    public Resource<Discrete<Boolean>> isActive() {
        return active;
    }

    public Resource<Discrete<Optional<Sequence>>> loadedSequence() {
        return loadedSequence;
    }

    public Resource<Discrete<Boolean>> isLoaded() {
        return isLoaded;
    }

    public Resource<Discrete<String>> loadedSequenceId() {
        return loadedSequenceId;
    }

    public Resource<Discrete<Integer>> nextCommandIndex() {
        return nextCommandIndex;
    }

    public Resource<Discrete<Optional<TimedCommand>>> nextCommand() {
        return nextCommand;
    }

    public Resource<Discrete<String>> nextCommandStem() {
        return nextCommandStem;
    }

    public Resource<Discrete<Integer>> lastDispatchedCommandIndex() {
        return lastDispatchedCommandIndex;
    }

    public Resource<Discrete<Optional<TimedCommand>>> lastDispatchedCommand() {
        return lastDispatchedCommand;
    }

    public Resource<Discrete<String>> lastDispatchedCommandStem() {
        return lastDispatchedCommandStem;
    }

    // Additional utility functions
    // These could arguably be split into a separate utility class as static methods acting on the public methods above.
    public Resource<Discrete<Boolean>> isLoadedWith(Sequence sequence) {
        return map(loadedSequence(), $ -> $.orElse(null) == sequence);
    }

    public Resource<Discrete<Boolean>> lastDispatched(Command cmd) {
        return map(lastDispatchedCommand(), $ -> $.map(TimedCommand::command).orElse(null) == cmd);
    }

    public Condition nextDispatch() {
        var i = currentValue(lastDispatchedCommandIndex());
        return when(notEquals(lastDispatchedCommandIndex(), constant(i)));
    }
}
