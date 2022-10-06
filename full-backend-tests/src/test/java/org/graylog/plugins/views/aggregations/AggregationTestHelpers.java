package org.graylog.plugins.views.aggregations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

class AggregationTestHelpers {
    static InputStream serialize(Object request) {
        try {
            final ObjectMapper objectMapper = new ObjectMapperProvider().get();
            return new ByteArrayInputStream(objectMapper.writeValueAsBytes(request));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializing test fixture: ", e);
        }
    }
}
