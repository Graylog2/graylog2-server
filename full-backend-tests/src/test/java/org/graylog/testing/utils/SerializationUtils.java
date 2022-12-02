package org.graylog.testing.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class SerializationUtils {
    public static InputStream serialize(Object request) {
        try {
            final ObjectMapper objectMapper = new ObjectMapperProvider().get();
            return new ByteArrayInputStream(objectMapper.writeValueAsBytes(request));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializing test fixture: ", e);
        }
    }
}
