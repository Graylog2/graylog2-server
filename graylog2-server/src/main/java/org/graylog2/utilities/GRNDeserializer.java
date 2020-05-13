
package org.graylog2.utilities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Converts GRN strings format into a {@link GRN} object.
 */
public class GRNDeserializer extends StdDeserializer<GRN> {
    private final GRNRegistry grnRegistry;

    public GRNDeserializer() {
        super(GRN.class);
        // TODO set this up in the ObjectMapperProvider once we move this into server
        grnRegistry = GRNRegistry.createWithBuiltinTypes();
    }

    @Override
    public GRN deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return grnRegistry.parse(p.getValueAsString());
    }
}

