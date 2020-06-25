
package org.graylog2.utilities;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

// TODO Not sure why this is needed
public class GRNKeyDeserializer extends KeyDeserializer {
    private final GRNRegistry grnRegistry;

    public GRNKeyDeserializer(GRNRegistry grnRegistry) {
        super();
        this.grnRegistry = grnRegistry;
    }

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return grnRegistry.parse(key);
    }

}

