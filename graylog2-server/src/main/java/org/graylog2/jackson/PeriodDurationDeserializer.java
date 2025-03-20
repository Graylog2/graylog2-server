package org.graylog2.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.threeten.extra.PeriodDuration;

import java.io.IOException;

public class PeriodDurationDeserializer extends StdDeserializer<PeriodDuration> {

    public PeriodDurationDeserializer() {
        super(PeriodDuration.class);
    }

    @Override
    public PeriodDuration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentTokenId() == JsonTokenId.ID_STRING) {
            return PeriodDuration.parse(p.getText().trim());
        }
        throw ctxt.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING, "expected String");
    }
}
