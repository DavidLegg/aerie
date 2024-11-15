package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.StreamlineSystem;
import gov.nasa.jpl.aerie.contrib.streamline.core.InitialConditionManager.InitialConditions;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import javax.json.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.contrib.streamline.StreamlineSystem.currentInstant;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Logging.LOGGER;
import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.spawn;
import static java.util.stream.Collectors.toMap;

public final class Incons {
    private Incons() {}

    public static InitialConditions readIncons(String fileName) {
        try {
            var reader = Json.createReader(new FileInputStream(fileName));
            var inconJsonObject = reader.readObject();
            var inconMap = jsonToSerializedValue(inconJsonObject).asMap().orElseThrow();
            var result = InitialConditions.of(inconMap);
            // Since this happens during the init phase, we have to defer logging the error to the simulation phase.
            spawn(() -> LOGGER.info("Loaded incons for %s from %s", currentInstant(), fileName));
            return result;
        } catch (FileNotFoundException e) {
            spawn(() -> LOGGER.error("Error loading incons from %s: %s", fileName, e));
            return InitialConditions.of(Map.of());
        }
    }

    public static SerializedValue jsonToSerializedValue(JsonValue jsonValue) {
        return switch (jsonValue.getValueType()) {
            case ARRAY -> SerializedValue.of(jsonValue.asJsonArray().stream()
                    .map(Incons::jsonToSerializedValue).toList());
            case OBJECT -> SerializedValue.of(jsonValue.asJsonObject().entrySet().stream()
                    .collect(toMap(Map.Entry::getKey, entry -> jsonToSerializedValue(entry.getValue()))));
            case STRING -> SerializedValue.of(((JsonString) jsonValue).getString());
            case NUMBER -> {
                var jsonNumber = ((JsonNumber) jsonValue);
                yield jsonNumber.isIntegral() ? SerializedValue.of(jsonNumber.longValueExact()) : SerializedValue.of(jsonNumber.doubleValue());
            }
            case TRUE -> SerializedValue.of(Boolean.TRUE);
            case FALSE -> SerializedValue.of(Boolean.FALSE);
            case NULL -> SerializedValue.NULL;
        };
    }

    public static void writeFincons(String fileName, Map<String, SerializedValue> fincons) {
        try {
            var inconJsonObject = serializedValueToJson(SerializedValue.of(fincons));
            var writer = Json.createWriter(new FileOutputStream(fileName));
            writer.write(inconJsonObject);
            LOGGER.info("Final conditions for %s written to %s", currentInstant(), fileName);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error writing fincons to %s: %s", fileName, e);
        }
    }

    private static JsonValue serializedValueToJson(SerializedValue serializedValue) {
        return serializedValue.match(new SerializedValue.Visitor<>() {
            @Override
            public JsonValue onNull() {
                return JsonValue.NULL;
            }

            @Override
            public JsonValue onNumeric(BigDecimal value) {
                return Json.createValue(value);
            }

            @Override
            public JsonValue onBoolean(boolean value) {
                return value ? JsonValue.TRUE : JsonValue.FALSE;
            }

            @Override
            public JsonValue onString(String value) {
                return Json.createValue(value);
            }

            @Override
            public JsonValue onMap(Map<String, SerializedValue> value) {
                var builder = Json.createObjectBuilder();
                for (var entry : value.entrySet()) {
                    builder.add(entry.getKey(), serializedValueToJson(entry.getValue()));
                }
                return builder.build();
            }

            @Override
            public JsonValue onList(List<SerializedValue> value) {
                var builder = Json.createArrayBuilder();
                for (var entry : value) {
                    builder.add(serializedValueToJson(entry));
                }
                return builder.build();
            }
        });
    }
}
