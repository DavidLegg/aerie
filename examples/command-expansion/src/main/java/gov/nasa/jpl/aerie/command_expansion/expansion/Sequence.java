package gov.nasa.jpl.aerie.command_expansion.expansion;

import java.util.List;

import static gov.nasa.jpl.aerie.command_expansion.expansion.SeqJsonSequence.*;

// Mission-specific "logical" sequence. In this case, it's specifically for this mission, and based on Seq.JSON / FCPL
/**
 * Sequence as understood by this mission.
 */
public record Sequence(
        String seqId,
        List<TimedCommand> commands) {

    public SeqJsonSequence toSeqJson() {
        return new SeqJsonSequence(
                seqId,
                commands.stream()
                        .map(cmd -> SeqJsonStep.command(
                                switch (cmd.timeTag()) {
                                    case TimedCommand.AbsoluteTimeTag absolute -> SeqJsonStepTime.absolute(absolute.time());
                                    case TimedCommand.RelativeTimeTag relative -> SeqJsonStepTime.relative(relative.offset());
                                    case TimedCommand.EpochRelativeTimeTag epochRelative -> SeqJsonStepTime.epochRelative(epochRelative.offset());
                                    case TimedCommand.CommandCompleteTimeTag commandComplete -> SeqJsonStepTime.commandComplete();
                                },
                                cmd.command().stem(),
                                cmd.command().args().stream().map(SeqJsonCommandArg::of).toList()
                        ))
                        .toList()
        );
    }
}
