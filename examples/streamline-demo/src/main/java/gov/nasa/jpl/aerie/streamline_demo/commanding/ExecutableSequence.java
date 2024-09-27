package gov.nasa.jpl.aerie.streamline_demo.commanding;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;
import java.util.Optional;

public record ExecutableSequence(String id, List<ExecutableCommand> commands, Optional<Duration> epoch) {
}
