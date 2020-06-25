
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

    public GRNDeserializer(GRNRegistry grnRegistry) {
        super(GRN.class);
        this.grnRegistry = grnRegistry;
    }

    @Override
    public GRN deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return grnRegistry.parse(p.getValueAsString());
    }
}

