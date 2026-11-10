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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ResponseEntityConverter {
    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};
    private static final TypeReference<List<Object>> LIST_REF = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ResponseEntityConverter(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> convertValue(final Object entity, final Class<?> entityClass) {
        if (entityClass.equals(Void.class) || entityClass.equals(void.class)) {
            return null;
        }
        if (entityClass.equals(String.class)) {
            return Collections.singletonMap("data", objectMapper.convertValue(entity, String.class));
        }
        final JsonNode node = objectMapper.valueToTree(entity);
        if (node.isObject()) {
            return objectMapper.convertValue(node, MAP_REF);
        }
        if (node.isArray()) {
            return Collections.singletonMap("data", objectMapper.convertValue(node, LIST_REF));
        }
        return Collections.singletonMap("data", entity);
    }
}
