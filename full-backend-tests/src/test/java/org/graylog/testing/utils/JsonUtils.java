package org.graylog.testing.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

public final class JsonUtils {

    private JsonUtils(){}

    public static String toJsonString(Object s) {
        try {
            return new ObjectMapperProvider().get().writeValueAsString(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Search", e);
        }
    }
}
