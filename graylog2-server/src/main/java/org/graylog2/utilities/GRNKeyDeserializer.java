
package org.graylog2.utilities;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

// TODO Not sure why this is needed
public class GRNKeyDeserializer extends KeyDeserializer {
    private final GRNRegistry grnRegistry;

    public GRNKeyDeserializer() {
        super();
        // TODO set this up in the ObjectMapperProvider once we move this into server
        grnRegistry = GRNRegistry.createWithBuiltinTypes();
    }

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return grnRegistry.parse(key);
    }

}

