package gov.nasa.jpl.aerie.command_expansion.expansion;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.*;

// Also a mission-specific type, but this designed to directly mirror the serialized format.
// Since we're using Seq.JSON as our sample format, I'm using Jackson to describe the format as a Java class.
// Other missions might need to do more for serialization

/**
 * Serializable representation of SEQ JSON.
 */
public record SeqJsonSequence(
        String id,
        List<SeqJsonStep> steps
) {
    public record SeqJsonStep(
            String type,
            SeqJsonStepTime time,
            String stem,
            List<SeqJsonCommandArg> args
    ) {
        public static SeqJsonStep command(SeqJsonStepTime time, String stem, List<SeqJsonCommandArg> args) {
            return new SeqJsonStep("command", time, stem, args);
        }
    }

    public record SeqJsonStepTime(
            String type,
            @JsonSerialize(using = SeqJsonStepTimeSerializer.class)
            Object tag
    ) {
        public static SeqJsonStepTime absolute(Instant time) {
            return new SeqJsonStepTime("ABSOLUTE", time);
        }

        public static SeqJsonStepTime relative(Duration offset) {
            return new SeqJsonStepTime("RELATIVE", offset);
        }

        public static SeqJsonStepTime epochRelative(Duration offset) {
            return new SeqJsonStepTime("EPOCH_RELATIVE", offset);
        }

        public static SeqJsonStepTime commandComplete() {
            return new SeqJsonStepTime("COMMAND_COMPLETE", null);
        }
    }

    public record SeqJsonCommandArg(
            String type,
            Object value
    ) {
        public static SeqJsonCommandArg of(Object value) {
            return switch (value) {
                case Number n -> new SeqJsonCommandArg("number", n);
                case String s -> new SeqJsonCommandArg("string", s);
                case Enum<?> e -> new SeqJsonCommandArg("string", e.name());
                default -> throw new RuntimeException("Unsupported arg type: " + value.getClass().getSimpleName());
            };
        }
    }

    public String serialize() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class SeqJsonStepTimeSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            switch (o) {
                case null:
                    jsonGenerator.writeNull();
                    break;
                case Instant instant:
                    jsonGenerator.writeString(
                            DateTimeFormatter.ofPattern("yyyy-DDD'T'HH:mm:ss.SSS")
                                    .withZone(ZoneId.from(ZoneOffset.UTC))
                                    .format(instant));
                    break;
                case Duration duration:
                    jsonGenerator.writeString(hmsFormat(duration));
                    break;
                default:
                    throw new RuntimeException("Unhandled type: " + o.getClass().getSimpleName());
            }
        }

        private String hmsFormat(Duration duration) {
            long h = duration.in(HOURS);
            duration = duration.minus(h, HOURS);
            long m = duration.in(MINUTES);
            duration = duration.minus(m, MINUTES);
            double s = duration.ratioOver(SECONDS);
            return String.format("%02d:%02d:%06.3f", h, m, s);
        }
    }
}
