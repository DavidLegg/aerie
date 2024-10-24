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
                        .map(cmd -> new SeqJsonStep(
                                "command",
                                switch (cmd.timeTag()) {
                                    case TimedCommand.AbsoluteTimeTag absolute ->
                                            new SeqJsonStepTime("ABSOLUTE", absolute.time());
                                    case TimedCommand.RelativeTimeTag relative ->
                                            new SeqJsonStepTime("RELATIVE", relative.offset());
                                    case TimedCommand.EpochRelativeTimeTag epochRelative ->
                                            new SeqJsonStepTime("EPOCH_RELATIVE", epochRelative.offset());
                                    case TimedCommand.CommandCompleteTimeTag commandComplete ->
                                            new SeqJsonStepTime("COMMAND_COMPLETE", null);
                                },
                                cmd.command().stem(),
                                cmd.command().args().stream()
                                        .map(arg -> switch (arg) {
                                            case Number n -> new SeqJsonCommandArg("number", n);
                                            case String s -> new SeqJsonCommandArg("string", s);
                                            default -> throw new RuntimeException("Unsupported arg type: " + arg.getClass().getSimpleName());
                                        })
                                        .toList()
                        ))
                        .toList()
        );
    }
}
