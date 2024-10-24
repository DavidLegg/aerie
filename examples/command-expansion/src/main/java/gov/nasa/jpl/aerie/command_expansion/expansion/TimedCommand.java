package gov.nasa.jpl.aerie.command_expansion.expansion;

import gov.nasa.jpl.aerie.command_expansion.command_activities.Command;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;

public record TimedCommand(TimeTag timeTag, Command command) {
    public sealed interface TimeTag { }
    public record AbsoluteTimeTag(Instant time) implements TimeTag { }
    public record RelativeTimeTag(Duration offset) implements TimeTag { }
    public record EpochRelativeTimeTag(Duration offset) implements TimeTag { }
    public record CommandCompleteTimeTag() implements TimeTag { }

    public static TimedCommand absolute(final Instant time, final Command command) {
        return new TimedCommand(new AbsoluteTimeTag(time), command);
    }

    public static TimedCommand relative(final Duration offset, final Command command) {
        return new TimedCommand(new RelativeTimeTag(offset), command);
    }

    public static TimedCommand epochRelative(final Duration offset, final Command command) {
        return new TimedCommand(new EpochRelativeTimeTag(offset), command);
    }

    public static TimedCommand commandComplete(final Command command) {
        return new TimedCommand(new CommandCompleteTimeTag(), command);
    }
}
