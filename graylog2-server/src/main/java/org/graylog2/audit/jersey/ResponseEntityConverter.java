/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.audit.jersey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

//TODO: remove this class from enterprise module
public class ResponseEntityConverter {
    private final ObjectMapper objectMapper;

    public ResponseEntityConverter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> convertValue(final Object entity, final Class<?> entityClass) {
        if (entityClass.equals(String.class)) {
            return Collections.singletonMap("data", objectMapper.convertValue(entity, String.class));
        } else if (!entityClass.equals(Void.class) && !entityClass.equals(void.class)) {
            final TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
            };
            try {
                return objectMapper.convertValue(entity, typeRef);
            } catch (IllegalArgumentException e) {
                // Try to convert the response to a list if converting to a map failed.
                final TypeReference<List<Object>> arrayTypeRef = new TypeReference<>() {
                };
                return Collections.singletonMap("data", objectMapper.convertValue(entity, arrayTypeRef));
            }
        }
        return null;
    }


}
