package gov.nasa.jpl.aerie.contrib.streamline.utils;

import gov.nasa.jpl.aerie.merlin.framework.Result;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public final class ValueMapperUtils {
    private ValueMapperUtils() {}

    /**
     * Build a {@link ValueMapper} for U from one for T
     * by specifying an isomorphism between T and U.
     */
    public static <T, U> ValueMapper<U> map(ValueMapper<T> baseMapper, InvertibleFunction<T, U> f) {
        return new ValueMapper<>() {
            @Override
            public ValueSchema getValueSchema() {
                return baseMapper.getValueSchema();
            }

            @Override
            public Result<U, String> deserializeValue(SerializedValue serializedValue) {
                return baseMapper.deserializeValue(serializedValue).mapSuccess(f::apply);
            }

            @Override
            public SerializedValue serializeValue(U value) {
                return baseMapper.serializeValue(f.inverse().apply(value));
            }
        };
    }
}
