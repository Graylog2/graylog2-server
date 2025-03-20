package org.graylog2.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.threeten.extra.PeriodDuration;

import java.io.IOException;

public class PeriodDurationSerializer extends StdSerializer<PeriodDuration> {

    public PeriodDurationSerializer() {
        super(PeriodDuration.class);
    }

    @Override
    public void serialize(PeriodDuration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        final String stringified = value.toString();
        gen.writeString(stringified);
    }
}
